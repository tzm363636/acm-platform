-- Incremental only; existing author rows remain non-login identities (username/password NULL).
UPDATE users SET role='USER' WHERE role='AUTHOR';
ALTER TABLE users DROP CHECK users_chk_1,
 ADD COLUMN username VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NULL,
 ADD CONSTRAINT uq_users_username UNIQUE(username),
 ADD CONSTRAINT chk_users_role CHECK(role IN ('USER','ADMIN')),
 ADD CONSTRAINT chk_login_identity CHECK(username IS NULL OR (password_hash IS NOT NULL AND is_demo=FALSE));

ALTER TABLE articles DROP CHECK articles_chk_1,
 ADD CONSTRAINT chk_article_status CHECK(status IN ('DRAFT','PENDING','PUBLISHED','REJECTED','ARCHIVED')),
 ADD COLUMN submitted_at DATETIME(6) NULL,
 ADD COLUMN review_round INT NOT NULL DEFAULT 0,
 ADD COLUMN revision INT NOT NULL DEFAULT 0,
 ADD INDEX idx_article_author_status(author_id,status,updated_at,id),
 ADD INDEX idx_article_review(status,submitted_at,id);

CREATE TABLE article_reviews (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 article_id BIGINT NOT NULL,
 review_round INT NOT NULL,
 reviewer_id BIGINT NOT NULL,
 decision VARCHAR(16) NOT NULL,
 reason TEXT NOT NULL,
 reviewed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE(article_id,review_round),
 FOREIGN KEY(article_id) REFERENCES articles(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 FOREIGN KEY(reviewer_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CHECK(decision IN ('APPROVED','REJECTED','ARCHIVED','PUBLISHED')),
 CHECK(review_round>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
