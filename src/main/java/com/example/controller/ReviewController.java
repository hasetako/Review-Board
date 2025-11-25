package com.example.controller;

import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.entity.Review;
import com.example.entity.User;
import com.example.repository.CategoryRepository;
import com.example.repository.ReviewRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ReviewController {

	private final ReviewRepository reviewRepository;
	// ★追加: 編集画面でカテゴリのプルダウンを出すために注入
	private final CategoryRepository categoryRepository;

	// ★追加: 新規レビューの作成方式を選ぶ画面（本 or 本以外）
	// 画面: review_new.html（本 or 本以外の2ボタンで /review/book と /review/others へ遷移）
	@GetMapping("/review/new")
	public String newReviewSelector() {
		return "review_new";
	}

	// レビュー詳細（/reviews/{id}）
	// 非アクティブ（論理削除）レビューは「本人以外」閲覧不可にする
	@GetMapping("/reviews/{id}")
	public String show(@PathVariable("id") Integer id, HttpSession session, Model model) { // HttpSession 受け取り
		Optional<Review> opt = reviewRepository.findById(id);
		if (opt.isEmpty()) {
			// 簡易エラーハンドリング（存在しないID）
			model.addAttribute("error", "レビューが見つかりませんでした。");
			return "index"; // トップへ戻す（必要に応じて404ページへ）
		}
		Review review = opt.get();

		// 非アクティブなら、本人のみ閲覧可
		User loginUser = (User) session.getAttribute("loginUser");
		boolean owner = (loginUser != null && review.getUser() != null
				&& review.getUser().getId().equals(loginUser.getId()));
		if (Boolean.FALSE.equals(review.getActiveFlag()) && !owner) {
			model.addAttribute("error", "このレビューは閲覧できません。");
			return "index";
		}

		model.addAttribute("review", review);

		// ===============================================
		// 同じコンテンツの「他ユーザー」のレビュー一覧を取得して渡す
		// ・active=1 のみ
		// ・自身のレビューIDと自身のユーザーIDは除外
		// ===============================================
		var otherReviews = reviewRepository.findActiveBySameContentsOtherUsers(
				review.getContents().getContentsId(),
				review.getReviewId(),
				review.getUser() != null ? review.getUser().getId() : -1 // null安全
		);
		model.addAttribute("otherReviews", otherReviews);

		return "review_show";
	}

	// ==============================
	// レビュー編集（GET）
	// ==============================
	@GetMapping("/reviews/{id}/edit")
	public String editForm(@PathVariable("id") Integer id, HttpSession session, Model model) {
		User loginUser = (User) session.getAttribute("loginUser");
		if (loginUser == null) {
			// 未ログインガード
			return "redirect:/login";
		}

		Optional<Review> opt = reviewRepository.findById(id);
		if (opt.isEmpty()) {
			model.addAttribute("error", "レビューが見つかりませんでした。");
			return "index";
		}

		Review review = opt.get();
		// オーナーシップチェック（自分の投稿のみ編集可）
		if (review.getUser() == null || !review.getUser().getId().equals(loginUser.getId())) {
			model.addAttribute("error", "このレビューを編集する権限がありません。");
			return "index";
		}

		// 編集フォーム用にそのままエンティティを渡す（簡易）
		model.addAttribute("review", review);

		// カテゴリ一覧
		model.addAttribute("categories",
				categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryName")));

		return "review_edit";
	}

	// ==============================
	// レビュー編集（POST）
	// ==============================
	@PostMapping("/reviews/{id}/edit")
	public String editSubmit(@PathVariable("id") Integer id,
			@RequestParam("rate") Integer rate,
			@RequestParam("reviewTitle") String reviewTitle,
			@RequestParam("reviewText") String reviewText,
			// ★ここを単体 category → 複数 categoryIds に変更
			@RequestParam(name = "categoryIds", required = false) java.util.List<Integer> categoryIds,
			HttpSession session,
			Model model) {

		User loginUser = (User) session.getAttribute("loginUser");
		if (loginUser == null) {
			return "redirect:/login";
		}

		var opt = reviewRepository.findById(id);
		if (opt.isEmpty()) {
			model.addAttribute("error", "レビューが見つかりませんでした。");
			return "index";
		}

		Review review = opt.get();
		// オーナーシップチェック
		if (review.getUser() == null || !review.getUser().getId().equals(loginUser.getId())) {
			model.addAttribute("error", "このレビューを編集する権限がありません。");
			return "index";
		}

		// ===== 入力バリデーション =====
		if (rate == null || rate < 1 || rate > 5) {
			model.addAttribute("error", "評価は1〜5で入力してください。");
			model.addAttribute("review", review);
			model.addAttribute("categories",
					categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryName")));
			return "review_edit";
		}

		if (reviewTitle == null || reviewTitle.isBlank()) {
			model.addAttribute("error", "レビュータイトルを入力してください。");
			model.addAttribute("review", review);
			model.addAttribute("categories",
					categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryName")));
			return "review_edit";
		}

		if (reviewText == null || reviewText.isBlank()) {
			model.addAttribute("error", "レビュー本文を入力してください。");
			model.addAttribute("review", review);
			model.addAttribute("categories",
					categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryName")));
			return "review_edit";
		}

		// ★カテゴリ：複数必須にするならここでチェック
		if (categoryIds == null || categoryIds.isEmpty()) {
			model.addAttribute("error", "カテゴリを1つ以上選択してください。");
			model.addAttribute("review", review);
			model.addAttribute("categories",
					categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryName")));
			return "review_edit";
		}

		// ===== 更新処理 =====
		review.setRate(rate);
		review.setReviewTitle(reviewTitle);
		review.setReviewText(reviewText);

		// いったん全部クリアしてから付け直す
		review.getCategories().clear();
		if (categoryIds != null && !categoryIds.isEmpty()) {
			var selectedCategories = categoryRepository.findAllById(categoryIds);
			review.getCategories().addAll(selectedCategories);
		}

		reviewRepository.save(review);

		return "redirect:/reviews/" + id;
	}

	// ==============================
	// レビュー論理削除（POST）
	// 本人のみ可。active_flag = 0 に更新。
	// ==============================
	@PostMapping("/reviews/{id}/delete")
	public String deleteLogical(@PathVariable("id") Integer id, HttpSession session, Model model) {
		User loginUser = (User) session.getAttribute("loginUser");
		if (loginUser == null) {
			return "redirect:/login";
		}

		Optional<Review> opt = reviewRepository.findById(id);
		if (opt.isEmpty()) {
			return "redirect:/mypage";
		}

		Review review = opt.get();
		if (review.getUser() == null || !review.getUser().getId().equals(loginUser.getId())) {
			return "redirect:/mypage"; // 自分以外は拒否
		}

		// 論理削除
		review.setActiveFlag(false);
		reviewRepository.save(review);

		return "redirect:/mypage";
	}
}