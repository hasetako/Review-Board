package com.example.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.entity.Category;
import com.example.entity.Review;
import com.example.repository.CategoryRepository;
import com.example.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;

    // カテゴリ一覧
    @GetMapping("/categories")
    public String categories(Model model) {
        List<Category> categories = categoryRepository.findAll(
                Sort.by(Sort.Direction.ASC, "categoryName"));
        model.addAttribute("categories", categories);
        return "categories_index";
    }

    // 各カテゴリのレビュー一覧（active=1 のみ）
    @GetMapping("/categories/{id}")
    public String categoryShow(@PathVariable("id") Integer id, Model model) {
        Optional<Category> catOpt = categoryRepository.findById(id);
        if (catOpt.isEmpty()) {
            model.addAttribute("error", "カテゴリが見つかりませんでした。");
            return "categories_index";
        }

        Category category = catOpt.get();

        // ★変更ポイント：
        // 以前: 単一カテゴリ外部キー
        //   findByCategory_CategoryIdAndActiveFlagTrueOrderByReviewIdDesc(id);
        // 今回: 多対多（review_category 経由）用メソッドを使用
        List<Review> reviews =
                reviewRepository.findActiveByCategoryOrderByReviewIdDesc(id);

        model.addAttribute("category", category);
        model.addAttribute("reviews", reviews);
        return "category_reviews";
    }
}
