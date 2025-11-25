package com.example.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // 名前からカテゴリを一意取得（あれば）
    Optional<Category> findByCategoryName(String categoryName);

    // すでに使っているなら残してOK：
    // List<Category> findAllByOrderByCategoryNameAsc();
}
