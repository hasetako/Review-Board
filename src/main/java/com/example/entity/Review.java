package com.example.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "review")
@Getter
@Setter
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Integer reviewId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contents_id", nullable = false)
    private Contents contents;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "rate", nullable = false)
    private Integer rate;

    @Column(name = "review_title", nullable = false, length = 255)
    private String reviewTitle;

    @Lob
    @Column(name = "review_text", nullable = false)
    private String reviewText;

    @Column(name = "active_flag", nullable = false)
    private Boolean activeFlag = Boolean.TRUE;

    /**
     * カテゴリとの多対多関係
     * review_category テーブルでマッピング
     */
    @ManyToMany
    @JoinTable(
        name = "review_category",
        joinColumns = @JoinColumn(name = "review_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    // ===============================
    // 互換用の「単一カテゴリ」アクセサ
    // 既存コードの getCategory()/setCategory(...) を殺さないため
    // ===============================

    /**
     * 便宜上、先頭のカテゴリを「代表カテゴリ」として返す。
     * 何も付いていなければ null。
     */
    @Transient
    public Category getCategory() {
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        // Set なので iterator() から1件目を返す
        return categories.iterator().next();
    }

    /**
     * 便宜上、「代表カテゴリ」を 1件だけセットする。
     * 既存の setCategory(...) 呼び出しは
     * 「カテゴリを 1件だけ持つレビュー」として扱われる。
     */
    @Transient
    public void setCategory(Category category) {
        this.categories.clear();
        if (category != null) {
            this.categories.add(category);
        }
    }
}
