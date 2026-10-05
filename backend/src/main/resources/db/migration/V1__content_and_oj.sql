-- Authoritative schema. MySQL >= 8.0.16 (enforced CHECK constraints).
-- UTC DATETIME(6); milliseconds and binary megabytes (1 MB = 1048576 bytes).
-- No database creation, destructive reset, seed data or hidden test answers.
CREATE TABLE users (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 public_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
 display_name VARCHAR(100) NOT NULL,
 password_hash VARCHAR(255) NULL,
 role VARCHAR(16) NOT NULL DEFAULT 'USER',
 is_demo BOOLEAN NOT NULL DEFAULT FALSE,
 disabled_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CHECK (role IN ('USER','AUTHOR','ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE categories (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(64) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE tags (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(64) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE articles (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 public_id BIGINT NOT NULL UNIQUE,
 author_id BIGINT NOT NULL,
 category_id BIGINT NOT NULL,
 title VARCHAR(255) NOT NULL,
 summary TEXT NOT NULL,
 body JSON NOT NULL COMMENT 'Existing ArticleSection[]; code and paragraphs remain unchanged',
 preview JSON NOT NULL,
 featured BOOLEAN NOT NULL DEFAULT FALSE,
 wide BOOLEAN NOT NULL DEFAULT FALSE,
 status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
 data_kind VARCHAR(8) NOT NULL DEFAULT 'REAL',
 published_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
 CHECK (data_kind IN ('REAL','DEMO')),
 INDEX idx_article_public (status,published_at,id),
 INDEX idx_article_category (category_id,status,published_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE article_tags (
 article_id BIGINT NOT NULL,
 tag_id BIGINT NOT NULL,
 position INT NOT NULL,
 PRIMARY KEY(article_id,tag_id),
 UNIQUE(article_id,position),
 FOREIGN KEY(article_id) REFERENCES articles(id) ON DELETE CASCADE ON UPDATE RESTRICT,
 FOREIGN KEY(tag_id) REFERENCES tags(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 INDEX idx_article_tag (tag_id,article_id),
 CHECK (position>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE problems (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 public_id VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
 title VARCHAR(255) NOT NULL,
 difficulty VARCHAR(8) NOT NULL,
 description MEDIUMTEXT NOT NULL,
 input_format TEXT NOT NULL,
 output_format TEXT NOT NULL,
 constraints_json JSON NOT NULL,
 template_cpp17 MEDIUMTEXT NOT NULL,
 time_limit_ms INT NOT NULL,
 memory_limit_mb INT NOT NULL,
 source VARCHAR(255) NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
 data_kind VARCHAR(8) NOT NULL DEFAULT 'REAL',
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 CHECK (difficulty IN ('简单','中等','困难')),
 CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
 CHECK (data_kind IN ('REAL','DEMO')),
 CHECK(time_limit_ms>0 AND memory_limit_mb>0),
 INDEX idx_problem_list (status,difficulty,public_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE problem_tags (
 problem_id BIGINT NOT NULL,
 tag_id BIGINT NOT NULL,
 position INT NOT NULL,
 PRIMARY KEY(problem_id,tag_id),
 UNIQUE(problem_id,position),
 FOREIGN KEY(problem_id) REFERENCES problems(id) ON DELETE CASCADE ON UPDATE RESTRICT,
 FOREIGN KEY(tag_id) REFERENCES tags(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 INDEX idx_problem_tag (tag_id,problem_id),
 CHECK(position>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE problem_samples (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 problem_id BIGINT NOT NULL,
 position INT NOT NULL,
 input_text MEDIUMTEXT NOT NULL,
 output_text MEDIUMTEXT NOT NULL,
 explanation TEXT NOT NULL,
 UNIQUE(problem_id,position),
 FOREIGN KEY(problem_id) REFERENCES problems(id) ON DELETE CASCADE ON UPDATE RESTRICT,
 CHECK(position>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE problem_articles (
 problem_id BIGINT NOT NULL,
 article_id BIGINT NOT NULL,
 position INT NOT NULL,
 PRIMARY KEY(problem_id,article_id),
 UNIQUE(problem_id,position),
 FOREIGN KEY(problem_id) REFERENCES problems(id) ON DELETE CASCADE ON UPDATE RESTRICT,
 FOREIGN KEY(article_id) REFERENCES articles(id) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE submissions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 public_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
 user_id BIGINT NOT NULL,
 problem_id BIGINT NOT NULL,
 language VARCHAR(16) NOT NULL,
 code MEDIUMTEXT NULL COMMENT 'Immutable snapshot; NULL only for source-less demo fixtures',
 verdict VARCHAR(16) NOT NULL DEFAULT 'Pending',
 phase VARCHAR(16) NOT NULL DEFAULT 'waiting',
 data_kind VARCHAR(8) NOT NULL,
 origin VARCHAR(16) NOT NULL,
 demo_session_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
 demo_scenario VARCHAR(16) NULL,
 submitted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 finished_at DATETIME(6) NULL,
 time_ms INT NULL,
 memory_mb DECIMAL(12,3) NULL,
 FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 FOREIGN KEY(problem_id) REFERENCES problems(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CHECK(language='cpp17'),
 CHECK(verdict IN ('Pending','Judging','AC','WA','TLE','MLE','RE','CE','SystemError')),
 CHECK(phase IN ('waiting','compiling','judging','finished')),
 CHECK(data_kind IN ('REAL','DEMO')),
 CHECK(origin IN ('fixture','local','real')),
 CHECK((data_kind='REAL' AND origin='real' AND code IS NOT NULL AND demo_scenario IS NULL AND demo_session_hash IS NULL) OR
       (data_kind='DEMO' AND origin IN ('fixture','local') AND demo_scenario IN ('AC','WA','TLE','MLE','RE','CE','SystemError'))),
 CHECK(origin!='local' OR (code IS NOT NULL AND demo_session_hash IS NOT NULL)),
 CHECK((verdict IN ('Pending','Judging') AND finished_at IS NULL AND time_ms IS NULL AND memory_mb IS NULL AND phase!='finished') OR
       (verdict NOT IN ('Pending','Judging') AND finished_at IS NOT NULL AND phase='finished')),
 CHECK(time_ms IS NULL OR time_ms>=0),
 CHECK(memory_mb IS NULL OR memory_mb>=0),
 INDEX idx_submission_problem (problem_id,data_kind,submitted_at,id),
 INDEX idx_submission_user (user_id,data_kind,submitted_at,id),
 INDEX idx_submission_time (data_kind,submitted_at,id),
 INDEX idx_submission_verdict (data_kind,verdict,submitted_at,id),
 INDEX idx_submission_personal (demo_session_hash,problem_id,verdict),
 INDEX idx_submission_user_verdict (user_id,data_kind,verdict,problem_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE submission_details (
 submission_id BIGINT NOT NULL PRIMARY KEY,
 compiler_version VARCHAR(100) NULL,
 information MEDIUMTEXT NOT NULL,
 FOREIGN KEY(submission_id) REFERENCES submissions(id) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE submission_cases (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 submission_id BIGINT NOT NULL,
 position INT NOT NULL,
 public_name VARCHAR(100) NOT NULL,
 verdict VARCHAR(16) NOT NULL,
 time_ms INT NULL,
 memory_mb DECIMAL(12,3) NULL,
 UNIQUE(submission_id,position),
 FOREIGN KEY(submission_id) REFERENCES submissions(id) ON DELETE CASCADE ON UPDATE RESTRICT,
 CHECK(verdict IN ('Pending','Judging','AC','WA','TLE','MLE','RE','CE','SystemError')),
 CHECK(time_ms IS NULL OR time_ms>=0),
 CHECK(memory_mb IS NULL OR memory_mb>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
