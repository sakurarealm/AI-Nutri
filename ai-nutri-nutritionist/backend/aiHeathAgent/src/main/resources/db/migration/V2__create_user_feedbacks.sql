CREATE TABLE IF NOT EXISTS user_feedbacks (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  subject VARCHAR(64) NOT NULL,
  rating INT NOT NULL,
  content VARCHAR(2000) NOT NULL,
  order_no VARCHAR(64) NULL,
  status INT NOT NULL DEFAULT 0,
  admin_remark VARCHAR(2000) NULL,
  handled_by_admin_id BIGINT NULL,
  handled_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_uf_user (user_id),
  KEY idx_uf_status_created (status, created_at),
  KEY idx_uf_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

