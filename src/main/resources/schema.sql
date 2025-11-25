-- === schema.sql ===

-- 依存関係のある順に DROP
DROP TABLE IF EXISTS review_category;
DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS contents;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS users;

-- =======================
-- users
-- =======================
CREATE TABLE users
(
   id INT AUTO_INCREMENT PRIMARY KEY,
   username VARCHAR(100) NOT NULL UNIQUE,
   password VARCHAR(255) NOT NULL
)
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =======================
-- category
-- =======================
CREATE TABLE category
(
   category_id   INT AUTO_INCREMENT PRIMARY KEY,
   category_name VARCHAR(255) NOT NULL UNIQUE
)
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =======================
-- contents
-- =======================
CREATE TABLE contents
(
   contents_id    INT AUTO_INCREMENT PRIMARY KEY,
   contents_url   VARCHAR(2048) NOT NULL,
   contents_title VARCHAR(512)  NOT NULL,
   thumbnail      VARCHAR(4096),
   book_isbn      VARCHAR(128),
   contents_type  INT,
   CONSTRAINT uk_contents_url UNIQUE (contents_url)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =======================
-- review
--   ※ category_id は削除（多対多に変更）
-- =======================
CREATE TABLE review
(
   review_id    INT AUTO_INCREMENT PRIMARY KEY,
   contents_id  INT NOT NULL,
   user_id      INT NOT NULL,
   rate         INT NOT NULL,
   review_title VARCHAR(255) NOT NULL,
   review_text  LONGTEXT NOT NULL,
   active_flag  TINYINT(1) NOT NULL DEFAULT 1,

   CONSTRAINT fk_review_contents FOREIGN KEY (contents_id)
       REFERENCES contents (contents_id)
       ON UPDATE CASCADE ON DELETE RESTRICT,

   CONSTRAINT fk_review_user FOREIGN KEY (user_id)
       REFERENCES users (id)
       ON UPDATE CASCADE ON DELETE RESTRICT,

   CONSTRAINT ck_review_rate CHECK (rate BETWEEN 1 AND 5)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_review_contents ON review (contents_id);
CREATE INDEX idx_review_user     ON review (user_id);
CREATE INDEX idx_review_active   ON review (active_flag);

-- =======================
-- review_category（多対多の中間テーブル）
-- =======================
CREATE TABLE review_category
(
    review_id   INT NOT NULL,
    category_id INT NOT NULL,
    PRIMARY KEY (review_id, category_id),

    CONSTRAINT fk_review_category_review FOREIGN KEY (review_id)
        REFERENCES review (review_id)
        ON UPDATE CASCADE ON DELETE CASCADE,

    CONSTRAINT fk_review_category_category FOREIGN KEY (category_id)
        REFERENCES category (category_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
)
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- カテゴリ側からレビューを引くとき用
CREATE INDEX idx_review_category_category ON review_category (category_id);
