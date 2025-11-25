package com.example.controller;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.example.entity.Category;
import com.example.entity.Contents;
import com.example.entity.Review;
import com.example.entity.User;
import com.example.form.RakutenReviewForm;
import com.example.repository.CategoryRepository;
import com.example.repository.ContentsRepository;
import com.example.repository.ReviewRepository;
import com.example.repository.UserRepository;
import com.example.service.RakutenBooksService;
import com.example.service.RakutenBooksService.BookItem;
import com.example.service.RakutenBooksService.ExternalApiUnavailableException;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 楽天API検索/選択/保存フロー専用のController
 * - 既存 ReviewController から「/review/book」系のハンドラを★移動
 * - 既存コメントは削除せず保持
 * - ルーティングは据え置き（Thymeleaf側のaction変更不要）
 */
@Controller
@RequiredArgsConstructor
public class RakutenController {

	private final CategoryRepository categoryRepository;
	private final RakutenBooksService rakutenBooksService;
	private final ContentsRepository contentsRepository;
	private final ReviewRepository reviewRepository;
	private final UserRepository userRepository;

	// ==============================
	// 本レビュー（楽天API）画面
	// ==============================

	// カテゴリは常に供給
	@ModelAttribute("categories")
	public List<Category> categories() {
		return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryName"));
	}

	@GetMapping("/review/book")
	public String reviewBookGet(
			@ModelAttribute("form") RakutenReviewForm form,
			@RequestParam(name = "title", required = false) String titleParam,
			Model model,
			HttpSession session) {

		//
		// List<Category> categories =
		// categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryName"));
		// model.addAttribute("categories", categories);

		String title = (titleParam != null && !titleParam.isBlank())
				? titleParam
				: (form.getTitle() != null ? form.getTitle() : null);

		if (title != null && !title.isBlank()) {
			try {
				List<BookItem> results = rakutenBooksService.searchByTitle(title, 10);
				if (!results.isEmpty()) {
					model.addAttribute("results", results);
					model.addAttribute("message", "検索結果を表示しました。対象の本を1件選択してください。");
				} else {
					model.addAttribute("error", "該当する書籍が見つかりませんでした。");
				}
			} catch (ExternalApiUnavailableException e) { // 503等をユーザ向けメッセージに
				model.addAttribute("error", "現在、外部検索サービスが不安定です。時間をおいて再実行してください。");
			}
		}

		return "review_compose_book";
	}

	@PostMapping("/review/book/pick")
	public String pickBook(@ModelAttribute("form") RakutenReviewForm form, Model model) {

		if (form.getTitle() != null && !form.getTitle().isBlank()) {
			try {
				List<BookItem> results = rakutenBooksService.searchByTitle(form.getTitle(), 10);
				model.addAttribute("results", results);
			} catch (ExternalApiUnavailableException e) {
				model.addAttribute("error", "現在、外部検索サービスが不安定です。時間をおいて再実行してください。");
			}
		}

		model.addAttribute("message", "選択した本をプレビューに反映しました。");
		return "review_compose_book";
	}

	@PostMapping("/review/book")
	public String reviewBookPost(
			@Valid @ModelAttribute("form") RakutenReviewForm form,
			BindingResult binding,
			@SessionAttribute(name = "userId", required = false) Integer userId,
			HttpSession session,
			Model model) {

		boolean selected = (form.getUrl() != null && !form.getUrl().isBlank())
				|| (form.getIsbn() != null && !form.getIsbn().isBlank());
		if (!selected) {
			model.addAttribute("error", "本が未選択です。検索結果から対象の本を1件選択してください。");
			if (form.getTitle() != null && !form.getTitle().isBlank()) {
				try {
					List<BookItem> results = rakutenBooksService.searchByTitle(form.getTitle(), 10);
					model.addAttribute("results", results);
				} catch (ExternalApiUnavailableException e) {
					model.addAttribute("error", "現在、外部検索サービスが不安定です。時間をおいて再実行してください。");
				}
			}
			return "review_compose_book";
		}

		if (binding.hasErrors()) {
			model.addAttribute("error", "未入力の項目があります。");
			if (form.getTitle() != null && !form.getTitle().isBlank()) {
				try {
					List<BookItem> results = rakutenBooksService.searchByTitle(form.getTitle(), 10);
					model.addAttribute("results", results);
				} catch (ExternalApiUnavailableException e) {
					model.addAttribute("error", "現在、外部検索サービスが不安定です。時間をおいて再実行してください。");
				}
			}
			return "review_compose_book";
		}
		if (form.getRate() == null || form.getRate() < 1 || form.getRate() > 5) {
			model.addAttribute("error", "評価は1〜5で入力してください。");
			if (form.getTitle() != null && !form.getTitle().isBlank()) {
				try {
					List<BookItem> results = rakutenBooksService.searchByTitle(form.getTitle(), 10);
					model.addAttribute("results", results);
				} catch (ExternalApiUnavailableException e) {
					model.addAttribute("error", "現在、外部検索サービスが不安定です。時間をおいて再実行してください。");
				}
			}
			return "review_compose_book";
		}
		if (form.getReviewTitle() == null || form.getReviewTitle().isBlank()) {
			model.addAttribute("error", "レビュータイトルを入力してください。");
			if (form.getTitle() != null && !form.getTitle().isBlank()) {
				try {
					List<BookItem> results = rakutenBooksService.searchByTitle(form.getTitle(), 10);
					model.addAttribute("results", results);
				} catch (ExternalApiUnavailableException e) {
					model.addAttribute("error", "現在、外部検索サービスが不安定です。時間をおいて再実行してください。");
				}
			}
			return "review_compose_book";
		}
		if (form.getReviewText() == null || form.getReviewText().isBlank()) {
			model.addAttribute("error", "レビュー本文を入力してください。");
			if (form.getTitle() != null && !form.getTitle().isBlank()) {
				try {
					List<BookItem> results = rakutenBooksService.searchByTitle(form.getTitle(), 10);
					model.addAttribute("results", results);
				} catch (ExternalApiUnavailableException e) {
					model.addAttribute("error", "現在、外部検索サービスが不安定です。時間をおいて再実行してください。");
				}
			}
			return "review_compose_book";
		}
		if (form.getCategoryIds() == null || form.getCategoryIds().isEmpty()) {
			model.addAttribute("error", "カテゴリを1つ以上選択してください。");
			// タイトルで再検索して結果を再表示
			if (form.getTitle() != null && !form.getTitle().isBlank()) {
				try {
					List<BookItem> results = rakutenBooksService.searchByTitle(form.getTitle(), 10);
					model.addAttribute("results", results);
				} catch (ExternalApiUnavailableException e) {
					model.addAttribute("error", "現在、外部検索サービスが不安定です。時間をおいて再実行してください。");
				}
			}
			return "review_compose_book";
		}

		// ユーザー解決
		User user = null;
		if (userId != null) {
			user = userRepository.findById(userId).orElse(null);
		}
		if (user == null) {
			user = (User) session.getAttribute("loginUser");
		}
		if (user == null) {
			model.addAttribute("error", "ログインが必要です。");
			if (form.getTitle() != null && !form.getTitle().isBlank()) {
				try {
					List<BookItem> results = rakutenBooksService.searchByTitle(form.getTitle(), 10);
					model.addAttribute("results", results);
				} catch (ExternalApiUnavailableException e) {
					model.addAttribute("error", "現在、外部検索サービスが不安定です。時間をおいて再実行してください。");
				}
			}
			return "review_compose_book";
		}

		// contents upsert
		Contents contents = null;
		if (form.getIsbn() != null && !form.getIsbn().isBlank()) {
			contents = contentsRepository.findByBookIsbn(form.getIsbn());
		}
		if (contents == null && form.getUrl() != null && !form.getUrl().isBlank()) {
			contents = contentsRepository.findByContentsUrl(form.getUrl()).orElse(null);
		}
		if (contents == null) {
			contents = new Contents();
			String titleToSave = (form.getPreviewTitle() != null && !form.getPreviewTitle().isBlank())
					? form.getPreviewTitle()
					: form.getReviewTitle();
			contents.setContentsTitle(titleToSave);
			contents.setThumbnail(form.getPreviewImage());
			contents.setBookIsbn(form.getIsbn());
			contents.setContentsUrl(form.getUrl());
			contents.setContentsType(1); // 1=書籍
			contents = contentsRepository.save(contents);
		}

		// Review保存（複数カテゴリ対応）
		Review r = new Review();
		r.setContents(contents);
		r.setUser(user);
		r.setRate(form.getRate());
		r.setReviewTitle(form.getReviewTitle());
		r.setReviewText(form.getReviewText());

		// --- ここが重要 ---
		List<Integer> categoryIds = form.getCategoryIds();
		if (categoryIds == null || categoryIds.isEmpty()) {
			model.addAttribute("error", "カテゴリを1つ以上選択してください。");
			// 検索結果の再表示（今のコードでやっている処理）もここで呼び直す
			if (form.getTitle() != null && !form.getTitle().isBlank()) {
				try {
					List<BookItem> results = rakutenBooksService.searchByTitle(form.getTitle(), 10);
					model.addAttribute("results", results);
				} catch (ExternalApiUnavailableException e) {
					model.addAttribute("error", "現在、外部検索サービスが不安定です。時間をおいて再実行してください。");
				}
			}
			return "review_compose_book";
		}

		// ID → Categoryエンティティのセットに変換
		var categories = new java.util.HashSet<Category>(
				categoryRepository.findAllById(categoryIds));
		r.setCategories(categories);
		// --- ここまで ---

		r.setActiveFlag(true);
		reviewRepository.save(r);

		return "redirect:/mypage";

	}
}
