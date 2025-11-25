package com.example.controller;

import java.util.List;
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
import com.example.repository.ReviewRepository;
import com.example.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserController {

	private final ReviewRepository reviewRepository;
	private final UserService userService; // プロフィール更新などで使用

	@GetMapping("/mypage")
	public String mypage(HttpSession session, Model model) {
		User user = (User) session.getAttribute("loginUser");
		if (user == null) {
			return "redirect:/login"; // 未ログインならログイン画面へ
		}

		// 自分のレビューかつ active=1 のみ表示
		List<Review> all = reviewRepository.findAll(Sort.by(Sort.Direction.DESC, "reviewId"));
		List<Review> mine = all.stream()
				.filter(r -> r.getUser() != null && r.getUser().getId().equals(user.getId()))
				.filter(r -> Boolean.TRUE.equals(r.getActiveFlag()))
				.toList();

		model.addAttribute("user", user);
		model.addAttribute("reviews", mine);
		return "mypage";
	}

	// ============================
	// ユーザー詳細（公開ページ）
	// ============================
	@GetMapping("/user/{id}")
	public String userShow(@PathVariable("id") Integer id, HttpSession session, Model model) {
		// 本人ならマイページへリダイレクト
		User loginUser = (User) session.getAttribute("loginUser");
		if (loginUser != null && loginUser.getId().equals(id)) {
			return "redirect:/mypage";
		}

		Optional<User> opt = userService.findById(id);
		if (opt.isEmpty()) {
			model.addAttribute("error", "ユーザーが見つかりませんでした。");
			return "index";
		}
		User target = opt.get();

		// 該当ユーザーの active=1 レビュー一覧
		List<Review> all = reviewRepository.findAll(Sort.by(Sort.Direction.DESC, "reviewId"));
		List<Review> theirs = all.stream()
				.filter(r -> r.getUser() != null && r.getUser().getId().equals(target.getId()))
				.filter(r -> Boolean.TRUE.equals(r.getActiveFlag()))
				.toList();

		model.addAttribute("target", target);
		model.addAttribute("reviews", theirs);
		return "user_show";
	}

	// ==========================================
	// プロフィール編集（GET）
	// /user/edit/{id}  自分以外は /mypage に戻す
	// ==========================================
	@GetMapping("/user/edit/{id}")
	public String editForm(@PathVariable("id") Integer id, HttpSession session, Model model) {
		User loginUser = (User) session.getAttribute("loginUser");
		if (loginUser == null) {
			return "redirect:/login"; // 未ログインガード
		}
		if (!loginUser.getId().equals(id)) {
			// 自分以外の編集画面は拒否 → /mypage へ
			return "redirect:/mypage";
		}

		// 最新の情報を取得して表示（DBから再取得）
		Optional<User> opt = userService.findById(id);
		if (opt.isEmpty()) {
			return "redirect:/mypage"; // 想定外: 自分が消えている等
		}
		model.addAttribute("user", opt.get());
		return "user_edit";
	}

	// ==========================================
	// プロフィール更新（POST）
	// ==========================================
	@PostMapping("/user/edit/{id}")
	public String editUpdate(@PathVariable("id") Integer id,
			@RequestParam("username") String username,
			@RequestParam(name = "newPassword", required = false) String newPassword,
			@RequestParam(name = "confirmPassword", required = false) String confirmPassword,
			HttpSession session,
			Model model) {

		User loginUser = (User) session.getAttribute("loginUser");
		if (loginUser == null) {
			return "redirect:/login";
		}
		if (!loginUser.getId().equals(id)) {
			return "redirect:/mypage"; // 自分以外の更新は拒否
		}

		// 簡易バリデーション
		if (username == null || username.isBlank()) {
			model.addAttribute("error", "ユーザー名を入力してください。");
			model.addAttribute("user", loginUser);
			return "user_edit";
		}
		if (newPassword != null && !newPassword.isBlank()) {
			if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
				model.addAttribute("error", "新しいパスワードと確認用パスワードが一致しません。");
				model.addAttribute("user", loginUser);
				return "user_edit";
			}
		}

		try {
			User updated = userService.updateProfile(id, username, newPassword); // newPasswordは空なら変更なし
			session.setAttribute("loginUser", updated);
			model.addAttribute("message", "プロフィールを更新しました。");
			model.addAttribute("user", updated);
			return "user_edit";
		} catch (UserService.UsernameDuplicatedException e) {
			model.addAttribute("error", "そのユーザー名は既に使用されています。別の名前をお試しください。");
			model.addAttribute("user", loginUser);
			return "user_edit";
		} catch (IllegalArgumentException e) {
			model.addAttribute("error", "不正なリクエストです。");
			model.addAttribute("user", loginUser);
			return "user_edit";
		}
	}
}
