package com.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
// タイトルのユニーク制約は撤廃済み。URLのみユニークを維持。
@Table(name = "contents", uniqueConstraints = {
        @UniqueConstraint(name = "uk_contents_url", columnNames = "contents_url")
})
@Getter
@Setter
public class Contents {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contents_id")
    private Integer contentsId;

    // MariaDBでユニーク制約を張るためVARCHARに変更（2048）
    // @Lob
    @Size(min = 1, max = 2048) // 妥当性チェック
    @Column(name = "contents_url", nullable = false, length = 2048)
    private String contentsUrl;

    @NotBlank
    @Size(min = 1, max = 512)
    // @Lob
    // タイトルはユニークではない（重複許容）
    @Column(name = "contents_title", nullable = false, length = 512)
    private String contentsTitle;

    // サムネイルURLは可変が大きい可能性 → 4096 文字まで許容
    // @Lob
    @Size(max = 4096) // 長めのURLに対応（2048→4096）
    @Column(name = "thumbnail", length = 4096)
    private String thumbnail;

    @Column(name = "book_isbn", length = 128)
    private String bookIsbn;

    @Column(name = "contents_type") //1=book,2=それ以外
    private Integer contentsType;
}