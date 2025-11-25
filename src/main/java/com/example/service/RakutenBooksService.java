package com.example.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value; // application.properties から取得
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

// ★追加: 警告ログ用
import jakarta.annotation.PostConstruct;

/**
 * 楽天BooksBook APIの最小クライアントサービス
 * 既存構成を崩さないため、外部ライブラリはSpring標準のRestClientのみを使用
 * 画面は1ページ完結で「検索→プレビュー→レビュー投稿」を行う
 */
@Service
public class RakutenBooksService {

	private static final Logger log = LoggerFactory.getLogger(RakutenBooksService.class); 

	// プロパティ未設定でもプレースホルダ解決エラーで落ちないようデフォルトを与える
	// endpoint はデフォルトで BooksBook/Search のURLを使用
	@Value("${rakuten.api.endpoint:https://app.rakuten.co.jp/services/api/BooksBook/Search/20170404}")
	private String endpoint; // 例: https://app.rakuten.co.jp/services/api/BooksBook/Search/20170404

	// 未設定時は空文字（起動時に落とさず、呼び出し時にチェック）
	@Value("${rakuten.api.applicationId:}")
	private String applicationId;

	// 軽いリトライ回数（503や接続エラー時）
	@Value("${rakuten.api.retries:2}")
	private int retries; // デフォルト2回（合計3試行: 初回+2リトライ）

	private RestClient client() {
		// 毎回生成しても軽い/最小構成重視（必要なら@Bean化）
		return RestClient.builder()
				.baseUrl(endpoint)
				.build();
	}

	@PostConstruct
	void warnIfMissingConfig() {
		if (isBlank(applicationId)) {
			log.warn(
					"[RakutenBooksService] 'rakuten.api.applicationId' が未設定です。検索時にエラーになります。application.properties に設定してください。");
		}
		if (isBlank(endpoint)) {
			log.warn("[RakutenBooksService] 'rakuten.api.endpoint' が未設定です。デフォルトURLを使用します。");
		}
	}

	/** タイトルで検索（最小フィールドのみ使用） */
	public List<BookItem> searchByTitle(String title, int hits) {
		ensureConfigured();
		MultiValueMap<String, String> q = baseParams(hits);
		q.add("title", title);

		// 503/接続エラー時の軽量リトライ + やさしい例外へ変換
		RakutenBooksResponse resp = executeWithRetry(() -> client().get()
				.uri(b -> b.queryParams(q).build())
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.body(RakutenBooksResponse.class));

		return toItems(resp);
	}

//	/** ISBNで検索（1件想定） ISBN検索は今回利用しない方針*/
//	public List<BookItem> searchByIsbn(String isbn) {
//		ensureConfigured(); // ★既存
//		MultiValueMap<String, String> q = baseParams(1);
//		q.add("isbn", isbn);
//
//		// ★変更: 503/接続エラー時の軽量リトライ + やさしい例外へ変換
//		RakutenBooksResponse resp = executeWithRetry(() -> client().get()
//				.uri(b -> b.queryParams(q).build())
//				.accept(MediaType.APPLICATION_JSON)
//				.retrieve()
//				.body(RakutenBooksResponse.class));
//
//		return toItems(resp);
//	}

	// 実行時に設定が無ければ明確に伝える
	private void ensureConfigured() {
		if (isBlank(applicationId)) {
			throw new IllegalStateException(
					"Rakuten applicationId が未設定です。'rakuten.api.applicationId' を application.properties に設定してください。");
		}
	}

	private MultiValueMap<String, String> baseParams(int hits) {
		MultiValueMap<String, String> q = new LinkedMultiValueMap<>();
		q.add("applicationId", applicationId);
		q.add("format", "json");
		q.add("hits", String.valueOf(hits));
		return q;
	}

	private List<BookItem> toItems(RakutenBooksResponse resp) {
		List<BookItem> out = new ArrayList<>();
		if (resp == null || resp.items == null)
			return out;
		for (var w : resp.items) {
			var it = w.item;
			if (it == null)
				continue;
			var dto = new BookItem();
			dto.title = it.title;
			dto.isbn = (!isBlank(it.isbn)) ? it.isbn : it.isbnJan;
			dto.itemUrl = it.itemUrl;
			dto.thumbnail = firstPresent(it.mediumImageUrl, it.smallImageUrl, it.largeImageUrl);
			out.add(dto);
		}
		return out;
	}

	// ===== ここから 503/接続エラー対策（軽量リトライ＋例外変換） =====

	@FunctionalInterface
	private interface SupplierX<T> {
		T get() throws RestClientException;
	}

	/** 503 や一時的な接続エラーをリトライし、最終的にドメイン例外へ変換 */
	private <T> T executeWithRetry(SupplierX<T> call) {
		int attempt = 0;
		RestClientException last = null;

		while (attempt <= retries) {
			try {
				return call.get();
			} catch (RestClientResponseException e) {
				// ステータス別に判定（5xxは再試行対象）
				if (e.getStatusCode().is5xxServerError()) {
					last = e;
					backoff(attempt++);
					continue;
				}
				// 4xx 等はリトライせずそのまま上位へ（要件に応じて扱いを変える）
				throw e;
			} catch (RestClientException e) {
				// 低レベルな接続エラー（タイムアウト等）もリトライ対象
				last = e;
				backoff(attempt++);
			}
		}
		// すべて失敗した場合は、画面で扱いやすい独自例外に変換
		throw new ExternalApiUnavailableException("楽天Books API が一時的に利用できません。", last);
	}

	private void backoff(int attempt) {
		// 指数バックオフ (200ms, 400ms, 800ms...)
		long ms = (long) (200L * Math.pow(2, attempt));
		try {
			Thread.sleep(ms);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}

	// JDK8互換のblank判定ユーティリティ
	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	// 既存
	private static String firstPresent(String... s) {
		for (String v : s)
			if (!isBlank(v))
				return v;
		return null;
	}

	/** 画面表示用の最小DTO（内部クラス） */
	public static class BookItem {
		public String title;
		public String isbn;
		public String itemUrl;
		public String thumbnail;
	}

	/** 楽天レスポンスDTO（必要最小） */
	static class RakutenBooksResponse {
		@com.fasterxml.jackson.annotation.JsonProperty("Items")
		public java.util.List<RakutenItemWrapper> items;
	}

	static class RakutenItemWrapper {
		@com.fasterxml.jackson.annotation.JsonProperty("Item")
		public RakutenItem item;
	}

	static class RakutenItem {
		public String title;
		public String isbn;
		@com.fasterxml.jackson.annotation.JsonProperty("isbnjan")
		public String isbnJan;
		public String itemUrl;
		public String smallImageUrl;
		public String mediumImageUrl;
		public String largeImageUrl;
	}

	// 外部API一時障害をコントローラで握りやすい例外
	public static class ExternalApiUnavailableException extends RuntimeException {
		public ExternalApiUnavailableException(String msg, Throwable cause) {
			super(msg, cause);
		}
	}
}
