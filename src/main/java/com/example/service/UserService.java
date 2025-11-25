package com.example.service;

import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.entity.User;
import com.example.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	
	// BCrypt を利用するためのエンコーダ
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	// 会員登録
	public User register(String username, String password) {
		// 簡易バリデーション（null/空文字を防止）
		if (username == null || username.isBlank() || password == null || password.isBlank()) {
			throw new IllegalArgumentException("username/password required");
		}
		// 一意チェック（DB制約の前に弾く）
		if (userRepository.existsByUsername(username)) {
			throw new UsernameDuplicatedException();
		}

		User user = new User();
		user.setUsername(username);
		// パスワードをハッシュ化してから保存
        user.setPassword(passwordEncoder.encode(password));
		return userRepository.save(user);
	}

	// ログインチェック
	public User login(String username, String password) {
		User user = userRepository.findByUsername(username);
		if (user != null && passwordEncoder.matches(password, user.getPassword())) {	//DB上に保存されているハッシュ値と比較
            return user; // 認証成功
        }
		return null;
	}

	public User findByUsername(String username) {
		return userRepository.findByUsername(username);
	}

	// IDでの検索
	public Optional<User> findById(Integer id) {
		return userRepository.findById(id);
	}

	// 例外（ユーザー名重複）
	public static class UsernameDuplicatedException extends RuntimeException {
	}

	// プロフィール更新（username必須、newPasswordは任意）
	public User updateProfile(Integer id, String newUsername, String newPassword) {
		User u = userRepository.findById(id).orElseThrow(IllegalArgumentException::new);

		// ユーザー名の重複チェック（自分以外）
		if (!u.getUsername().equals(newUsername) && userRepository.existsByUsername(newUsername)) {
			throw new UsernameDuplicatedException(); // 呼び出し元でメッセージ表示
		}

		u.setUsername(newUsername);
		if (newPassword != null && !newPassword.isBlank()) {
			// パスワード更新時は必ずBCryptでハッシュ化して保存する
			u.setPassword(passwordEncoder.encode(newPassword));
		}
		return userRepository.save(u);
	}
}