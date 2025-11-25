package com.example.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.entity.Review;
import com.example.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final ReviewRepository reviewRepository;

    // 呼び出し先を searchByKeywordActive(...) に変更（active=1 限定）
    @GetMapping("/search")
    public String search(@RequestParam(name = "keyword", required = false) String keyword, Model model) {
        if (keyword == null || keyword.isBlank()) {
            // 空検索時もテンプレートが期待する属性を必ず供給
            model.addAttribute("keyword", ""); //
            model.addAttribute("reviews", Collections.emptyList()); // null回避
            model.addAttribute("totalCount", 0); // 集計行で使用
            model.addAttribute("error", "キーワードを入力してください。");
            return "search_results";
        }
        // active=1 のみ対象
        List<Review> reviews = reviewRepository.searchByKeywordActive(keyword);
        model.addAttribute("keyword", keyword);
        model.addAttribute("reviews", reviews);
        model.addAttribute("totalCount", reviews.size()); // テンプレの集計表示向け
        return "search_results";
    }
}
