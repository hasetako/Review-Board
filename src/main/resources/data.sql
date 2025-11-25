-- === data.sql ===

-- カテゴリの初期データ
INSERT INTO category (category_name) VALUES
  ('HTML'),
  ('CSS'),
  ('Javascript'),
  ('PHP'),
  ('Java'),
  ('SpringBoot'),
  ('Eclipse'),
  ('SQL');

-- ============================
-- ユーザーのシード
-- ============================
INSERT INTO users (id, username, password) VALUES
  (1, 'user1', '$2a$10$kZhfPmTYNjniJXgkJzr/NuSKt2/IFa9VC9tmI8X1RsG/z5/NyWY7S'),
  (2, 'user2', '$2a$10$kZhfPmTYNjniJXgkJzr/NuSKt2/IFa9VC9tmI8X1RsG/z5/NyWY7S'),
  (3, 'user3', '$2a$10$kZhfPmTYNjniJXgkJzr/NuSKt2/IFa9VC9tmI8X1RsG/z5/NyWY7S'),
  (4, 'user4', '$2a$10$kZhfPmTYNjniJXgkJzr/NuSKt2/IFa9VC9tmI8X1RsG/z5/NyWY7S'),
  (5, 'user5', '$2a$10$kZhfPmTYNjniJXgkJzr/NuSKt2/IFa9VC9tmI8X1RsG/z5/NyWY7S');

-- ============================
-- コンテンツのシード
-- ============================
INSERT INTO contents (contents_id, contents_url, contents_title, thumbnail, book_isbn, contents_type) VALUES
  (1, 'https://example.com/articles/jsoup-intro',      'Jsoup入門：HTMLをパースしてみよう',          'https://picsum.photos/seed/jsoup/200/200',      NULL, 2),
  (2, 'https://example.com/blog/spring-boot-review',   'Spring Bootで作るレビューボード',            'https://picsum.photos/seed/spring/200/200',     NULL, 2),
  (3, 'https://example.com/video/web-scraping-tips',   '動画解説：Webスクレイピングの基本と注意点',   'https://picsum.photos/seed/scraping/200/200',   NULL, 2);

-- ============================
-- レビュー（20件）
--   ※ category_id はここでは持たせない
-- ============================

-- 1) user1
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (1, 1, 1, 5, '実務でも使える！', 'Jsoupの基本がよくまとまっていて、最短で雰囲気を掴めました。コード例も分かりやすい。');

-- 2) user1
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (2, 2, 1, 4, 'Spring Boot入門に最適', 'REST + Thymeleafの流れが理解しやすく、最初の一歩にちょうど良い内容です。');

-- 3) user1
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (3, 3, 1, 3, 'スクレイピングの注意点が参考に', '法的・技術的な注意点がまとまっていて、基礎固めに使えます。');

-- 4) user2
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (4, 1, 2, 4, 'HTMLの実例が多く理解しやすい', 'DOMの見方や基本タグの使い方が丁寧。初心者にも安心。');

-- 5) user2
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (5, 2, 2, 5, '小さく作って育てる指南として◎', '設定から起動まで一気通貫で載っており、再現性が高かったです。');

-- 6) user2
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (6, 3, 2, 2, 'IDE前提の操作が多め', 'Eclipseユーザー向けの補足があるとさらに良かった。');

-- 7) user3
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (7, 1, 3, 5, '実案件にも適用できた', 'クローリングの前処理に役立つTIPSが助かった。');

-- 8) user3
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (8, 2, 3, 3, '設定は簡単、設計は難しい', 'Controller/Service/Repositoryの責務分担の話がもう少し欲しい。');

-- 9) user3
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (9, 3, 3, 4, 'CSSとの連携例もあると嬉しい', 'UI周りの最小構成も触れられているとさらに理解が深まりそう。');

-- 10) user4
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (10, 1, 4, 3, 'PHPとの比較視点が有用', 'テンプレートエンジンの思想を知るのに良い対比材料になった。');

-- 11) user4
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (11, 2, 4, 5, 'テスト観点も触れていて好印象', '最小のユニットテスト例があるので真似しやすい。');

-- 12) user4
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (12, 3, 4, 4, 'イベント駆動の流れが掴めた', '非同期処理の地雷が簡潔にまとまっている。');

-- 13) user5
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (13, 1, 5, 2, '基礎寄りで中級者には物足りない', '初学者向けには十分。発展編が欲しい。');

-- 14) user5
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (14, 2, 5, 4, '設定のハマりどころが回避できた', 'プロパティ周りの補足が地味に効く。');

-- 15) user5
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (15, 3, 5, 5, '網羅的で保存版', '法律面の注意喚起が強調されていて安心してチーム共有できる。');

-- 16) user2
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (16, 1, 2, 5, 'チートシート的に使える', 'セレクタの例が豊富で素早く書けるようになった。');

-- 17) user3
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (17, 2, 3, 4, 'DI/IoCの説明がわかりやすい', '図解が多く、クラス設計の理解が進む。');

-- 18) user4
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (18, 3, 4, 3, 'プラグイン依存の前提が合わなかった', 'Eclipse以外のIDE補足があるとより汎用的。');

-- 19) user1
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (19, 1, 1, 4, 'HTML構造の把握に最適', '要素の関係性を意識した解説が良い。');

-- 20) user5
INSERT INTO review (review_id, contents_id, user_id, rate, review_title, review_text) VALUES
  (20, 2, 5, 3, '運用を見据えた章が欲しい', '本番運用・監視の話が少しでもあると嬉しい。');

-- ============================
-- review_category マッピング
-- 元々の category_id をそのまま 1対1 で移行
-- ============================
INSERT INTO review_category (review_id, category_id) VALUES
  (1, 8),
  (2, 6),
  (3, 3),
  (4, 1),
  (5, 5),
  (6, 7),
  (7, 8),
  (8, 6),
  (9, 2),
  (10, 4),
  (11, 5),
  (12, 3),
  (13, 1),
  (14, 6),
  (15, 2),
  (16, 8),
  (17, 5),
  (18, 7),
  (19, 1),
  (20, 6);
