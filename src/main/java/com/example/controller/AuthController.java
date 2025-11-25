package com.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.entity.User;
import com.example.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthController {

	private final UserService userService;

	// ===== 会員登録 =====
	@GetMapping("/register")
	public String registerForm(HttpSession session) { // セッション受け取り
		// ログイン済みは /mypage
		if (session != null && session.getAttribute("loginUser") != null) {
			return "redirect:/mypage";
		}
		return "register";
	}

	@PostMapping("/register")
	public String register(@RequestParam String username,
			@RequestParam String password,
			HttpSession session, //
			Model model) {

		// ログイン済みは /mypage
		if (session != null && session.getAttribute("loginUser") != null) {
			return "redirect:/mypage";
		}

		try {
			userService.register(username, password);
			return "redirect:/login";
		} catch (UserService.UsernameDuplicatedException e) { // 一意制約の事前検出をキャッチ
			model.addAttribute("error", "そのユーザー名は既に使用されています。別の名前をお試しください。");
			model.addAttribute("username", username); // 入力値の保持
			return "register"; // 500に落とさず登録画面に戻す
		} catch (IllegalArgumentException e) { // 空値など
			model.addAttribute("error", "ユーザー名とパスワードを入力してください。"); 
			model.addAttribute("username", username); 
			return "register"; 
		}
	}

	// ===== ログイン =====
	@GetMapping("/login")
	public String loginForm(HttpSession session) {
		// ログイン済みは /mypage
		if (session != null && session.getAttribute("loginUser") != null) {
			return "redirect:/mypage";
		}
		return "login";
	}

	@PostMapping("/login")
	public String login(@RequestParam String username,
			@RequestParam String password,
			HttpSession session) {

		// ログイン済みは /mypage
		if (session != null && session.getAttribute("loginUser") != null) {
			return "redirect:/mypage";
		}

		User user = userService.login(username, password);
		if (user != null) {
			session.setAttribute("loginUser", user); // セッション属性名を loginUser に統一
			return "redirect:/mypage";
		} else {
			return "login"; // 失敗時はログイン画面へ
		}
	}

	// ===== ログアウト =====
	@GetMapping("/logout")
	public String logout(HttpSession session) {
		// 未ログイン（またはセッションにユーザーなし）は /login に遷移
		if (session == null || session.getAttribute("loginUser") == null) {
			return "redirect:/login";
		}
		session.invalidate();
		return "redirect:/login";
	}
}
