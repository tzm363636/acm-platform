-- Non-destructive additions. Existing identities, articles and JSON bodies remain unchanged.
ALTER TABLE users
 ADD COLUMN credential_version BIGINT NOT NULL DEFAULT 0,
 ADD CONSTRAINT chk_credential_version CHECK(credential_version>=0);

ALTER TABLE articles
 ADD COLUMN draft_key VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
 ADD CONSTRAINT uq_article_draft_identity UNIQUE(author_id,draft_key);
