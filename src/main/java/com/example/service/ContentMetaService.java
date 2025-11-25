package com.example.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Optional;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

@Service
public class ContentMetaService {

    public static class Meta {
        public final String url;       // 最終到達URL（リダイレクト後）
        public final String title;     // OGP or <title>（512文字に安全トリム）
        public final String imageUrl;  // OGP画像 or 空（4096 文字以内にサニタイズ済）

        public Meta(String url, String title, String imageUrl) {
            this.url = url;
            this.title = title;
            this.imageUrl = imageUrl;
        }
    }

    public Meta fetch(String rawUrl) throws IOException, URISyntaxException {
        String normalizedInput = normalizeUrl(rawUrl); // 入力の最低限正規化（スキーム補完など）

        Connection conn = Jsoup.connect(normalizedInput)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                         + "AppleWebKit/537.36 (KHTML, like Gecko) "
                         + "Chrome/120.0.0.0 Safari/537.36 JsoupReviewBoard/1.0")
                .timeout(15_000)               // 既存: 15秒
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .ignoreContentType(true);

        try {
            Document doc = conn.get();

            // 最終到達URL
            URL finalUrl = conn.response() != null ? conn.response().url() : new URL(normalizedInput);
            String finalUrlStr = finalUrl.toString();

            String rawTitle = firstOg(doc, "og:title")
                    .orElseGet(() -> Optional.ofNullable(doc.title()).orElse("（無題）"));

            String safeTitle = sanitizeTitle(rawTitle, 512);

            // 変更: サムネイルURLは 4096 文字以内にサニタイズ
            String rawImage = firstOg(doc, "og:image").orElse("");
            String safeImage = sanitizeThumbnailUrl(rawImage, 4096); // 4096 に拡張

            return new Meta(finalUrlStr, safeTitle, safeImage);
        } catch (IOException e) {
            throw new IOException("Meta fetch failed for URL: " + normalizedInput + " (" + e.getClass().getSimpleName() + ")", e);
        }
    }

    private Optional<String> firstOg(Document doc, String property) {
        Element el = doc.selectFirst("meta[property=" + property + "]");
        if (el != null) {
            String c = el.attr("content");
            if (c != null && !c.isBlank()) return Optional.of(c);
        }
        return Optional.empty();
    }

    private String normalizeUrl(String raw) throws URISyntaxException {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) return trimmed;

        URI u = new URI(trimmed);
        // スキーム省略時は https を補う
        if (u.getScheme() == null) {
            u = new URI("https://" + trimmed);
        }
        // 余分なフラグメント等はここで落とすなど最小正規化
        return u.toString();
    }

    // タイトルの安全化
    private String sanitizeTitle(String input, int limitChars) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFC);
        StringBuilder sb = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (Character.isISOControl(ch) && ch != '\n' && ch != '\r' && ch != '\t') continue;
            sb.append(ch);
        }
        String cleaned = sb.toString().trim();
        if (cleaned.codePointCount(0, cleaned.length()) > limitChars) {
            cleaned = new String(cleaned.codePoints().limit(limitChars).toArray(), 0, limitChars) + "…";
        }
        new String(cleaned.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        return cleaned;
    }

    // サムネイルURLの安全化（BeanValidation/@Column と合わせる）
    private String sanitizeThumbnailUrl(String input, int maxChars) {
        if (input == null) return "";
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return "";
        if (trimmed.codePointCount(0, trimmed.length()) <= maxChars) {
            return trimmed; // 4096 以下はそのまま採用
        }
        // それ以上は署名付きURLの破損を避けるため空に落とす
        return "";
    }
}