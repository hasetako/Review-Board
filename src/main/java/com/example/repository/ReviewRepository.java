package com.example.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    // ★既存: キーワード全文検索（タイトル/本文）＋ active=1
    @Query(value = """
        SELECT *
          FROM review r
         WHERE r.active_flag = 1
           AND (LOWER(r.review_title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR  LOWER(r.review_text)  LIKE LOWER(CONCAT('%', :keyword, '%')))
         ORDER BY r.review_id DESC
        """,
        nativeQuery = true)
    List<Review> searchByKeywordActive(@Param("keyword") String keyword);

    // ★既存: トップ一覧用（active=1 のみ）
    List<Review> findByActiveFlagTrueOrderByReviewIdDesc();

    // ==================================================
    // ★修正ポイント：カテゴリ別のレビュー取得（多対多対応版）
    //   - review_category 経由で Category を JOIN
    //   - r.activeFlag = true のみ
    //   - reviewId 降順
    //   - CategoryController から呼ばれる
    // ==================================================
    @Query("""
        SELECT r
          FROM Review r
          JOIN r.categories c
         WHERE c.categoryId = :categoryId
           AND r.activeFlag = true
         ORDER BY r.reviewId DESC
    """)
    List<Review> findActiveByCategoryOrderByReviewIdDesc(
            @Param("categoryId") Integer categoryId);

    // ==========================================
    // 同じコンテンツの他ユーザーのアクティブレビュー取得
    // - contents_id 一致
    // - active_flag = 1
    // - 自分の review_id を除外
    // - 自分の user_id を除外
    // ==========================================
    @Query("""
        SELECT r
          FROM Review r
         WHERE r.contents.contentsId = :contentsId
           AND r.activeFlag = true
           AND r.reviewId <> :excludeReviewId
           AND r.user.id <> :excludeUserId
         ORDER BY r.reviewId DESC
    """)
    List<Review> findActiveBySameContentsOtherUsers(
            @Param("contentsId") Integer contentsId,
            @Param("excludeReviewId") Integer excludeReviewId,
            @Param("excludeUserId") Integer excludeUserId);
}
