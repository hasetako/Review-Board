package com.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ReviewRepository reviewRepository;

    // トップページ = レビュー一覧（active=1 のみ表示）
    @GetMapping("/")
    public String top(Model model) {
        model.addAttribute("reviews",
                reviewRepository.findByActiveFlagTrueOrderByReviewIdDesc());
        return "index";
    }
}
