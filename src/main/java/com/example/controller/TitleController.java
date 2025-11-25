package com.example.controller;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.entity.Category;
import com.example.entity.Contents;
import com.example.entity.Review;
import com.example.entity.User;
import com.example.form.OthersReviewForm;
import com.example.repository.CategoryRepository;
import com.example.repository.ContentsRepository;
import com.example.repository.ReviewRepository;
import com.example.service.ContentMetaService;

import jakarta.servlet.http.HttpSession;

@Validated
@Controller
public class TitleController {

	private final ContentsRepository contentsRepository;
	private final ContentMetaService contentMetaService;
	private final ReviewRepository reviewRepository;
	private final CategoryRepository categoryRepository;

	public TitleController(ContentsRepository contentsRepository,
			ContentMetaService contentMetaService,
			ReviewRepository reviewRepository,
			CategoryRepository categoryRepository) {
		this.contentsRepository = contentsRepository;
		this.contentMetaService = contentMetaService;
		this.reviewRepository = reviewRepository;
		this.categoryRepository = categoryRepository;
	}

	@ModelAttribute("categories")
	public List<Category> categories() {
		return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryName"));
	}

	@GetMapping("/review")
	public String getMethodName(@RequestParam String param) {
		return "review_select";
	}

	// GET: URLからメタ取得してプレビュー表示
	@GetMapping("/review/others")
	public String othersGet(
			@RequestParam(value = "url", required = false) String url,
			Model model,
			HttpSession session) { // セッションを受け取る

		// 未ログイン時は /login に遷移
		if (session == null || session.getAttribute("loginUser") == null) {
			return "redirect:/login";
		}

		OthersReviewForm form = new OthersReviewForm();
		if (url != null && !url.isBlank()) {
			try {
				ContentMetaService.Meta m = contentMetaService.fetch(url);
				form.url = m.url;
				form.previewTitle = m.title;
				form.previewImage = m.imageUrl;
			} catch (Exception e) {
				model.addAttribute("error", "メタ情報の取得に失敗したよ… URLを確認してね");
				form.url = url;
			}
		}
		model.addAttribute("form", form);
		return "review_compose_others";
	}

	// POST: 保存（contents再利用 or 新規作成 → review作成）
	@PostMapping("/review/others")
	public String othersPost(@ModelAttribute("form") OthersReviewForm form, Model model, HttpSession session) {
		try {
			// 既存＋確認: 未ログインなら /login へ
			User loginUser = (User) session.getAttribute("loginUser");
			if (loginUser == null) {
				return "redirect:/login"; // 直接リダイレクトに統一
			}

			// URLが変な時に備えて再正規化＆タイトル補完
			ContentMetaService.Meta m = contentMetaService.fetch(form.url);

			// contentsはURL一意で再利用
			Contents contents = contentsRepository.findByContentsUrl(m.url)
					.orElseGet(() -> {
						Contents c = new Contents();
						c.setContentsUrl(m.url);
						c.setContentsTitle(m.title);
						c.setThumbnail(m.imageUrl); // フィールド流用
						c.setBookIsbn(null);
						c.setContentsType(2); // 2=Web記事/動画
						return contentsRepository.save(c);
					});

			// review作成
			Review r = new Review();
			r.setContents(contents);
			r.setRate(form.rate);
			r.setReviewTitle(form.reviewTitle);
			r.setReviewText(form.reviewText);
			r.setUser(loginUser);
			r.setActiveFlag(true); // 公開

			// --- 複数カテゴリ対応 ---
			List<Integer> categoryIds = form.getCategoryIds();
			if (categoryIds == null || categoryIds.isEmpty()) {
				model.addAttribute("error", "カテゴリを1つ以上選択してください。");
				// カテゴリ一覧を再セット（エラー時は必要）
				model.addAttribute("categories",
						categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryName")));
				return "review_compose_others";
			}

			var categories = new java.util.HashSet<Category>(
					categoryRepository.findAllById(categoryIds));
			r.setCategories(categories);
			// --- ここまで ---

			reviewRepository.save(r);

			return "redirect:/mypage";

		} catch (Exception e) {
			model.addAttribute("error", "保存に失敗したよ… 入力内容とURLを確認してね");
		}
		// 再表示用にカテゴリ一覧を毎回入れておく（エラー時のみ到達）
		model.addAttribute("categories",
				categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryName")));
		return "review_compose_others";
	}
}
