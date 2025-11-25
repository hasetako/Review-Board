package com.example.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.entity.Contents;

@Repository
public interface ContentsRepository extends JpaRepository<Contents, Integer> {

    Optional<Contents> findByContentsUrl(String contentsUrl);

    //Optional<Contents> findByContentsTitle(String contentsTitle);		//呼び出し０なので削除

    // 楽天APIから取得したISBNで既存レコードを特定するためのクエリ
    // Contents エンティティのフィールド名が「bookIsbn」であることが前提です。
    // （DB列名はマッピングにより異なっていてもOK）
    Contents findByBookIsbn(String bookIsbn);

    // （任意利用）: 存在チェック用。重複登録の抑止や分岐に使えます。
    boolean existsByBookIsbn(String bookIsbn);
}
