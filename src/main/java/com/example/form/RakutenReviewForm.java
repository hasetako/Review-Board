package com.example.form;

import java.util.List;

// バリデーション（必要なら）
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 書籍レビュー（楽天API）の1ページ完結フォーム
 * 既存コメントは残しつつ、プレビュー用フィールドを追加
 */
@Getter
@Setter
public class RakutenReviewForm {

    // 検索用（タイトル／ISBN）
    private String title; // 既存想定：検索ワード（タイトル） // 既存コメントは削除しないこと
    private String isbn; // ISBNでの検索/選択用

    // プレビュー表示用（検索結果から1件選択した内容を保持）
    private String previewTitle; // プレビュー表示用タイトル
    private String previewImage; // プレビュー表示用サムネイル
    private String url; // 楽天のitemUrl（レビュー保存時にcontents_urlへ）

    // 投稿用
    @NotNull(message = "評価を選択してください")
    private Integer rate;

    // 必須化
    @NotNull(message = "カテゴリを選択してください")
    private List<Integer> categoryIds;

    @NotBlank(message = "レビュータイトルを入力してください")
    private String reviewTitle;

    @NotBlank(message = "レビュー本文を入力してください")
    private String reviewText;
}
