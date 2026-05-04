-- 全量 v4.0 数据库迁移脚本
-- 涵盖所有新增表和字段

-- 1. 补全 oci_user 表字段 (deleted)
-- SQLite 不支持 IF NOT EXISTS 的 ADD COLUMN，所以需要忽略错误或通过外部逻辑控制
-- 这里我们假设 Runner 会捕获 duplicate column 异常并忽略
ALTER TABLE oci_user ADD COLUMN deleted INTEGER DEFAULT 0;

-- 2. 创建 IP 黑名单表
CREATE TABLE IF NOT EXISTS ip_blacklist (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ip_address TEXT,
    reason TEXT,
    banned_by TEXT,
    create_time DATETIME
);

-- 3. 创建 登录尝试记录表
CREATE TABLE IF NOT EXISTS login_attempts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ip_address TEXT,
    attempt_count INTEGER,
    last_attempt DATETIME
);

-- 4. 创建 审计日志表
CREATE TABLE IF NOT EXISTS audit_log (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    username TEXT,
    operation TEXT NOT NULL,
    target TEXT,
    details TEXT,
    success INTEGER DEFAULT 1,
    error_message TEXT,
    ip_address TEXT,
    user_agent TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 5. 创建索引
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_operation ON audit_log(operation);
CREATE INDEX IF NOT EXISTS idx_audit_log_create_time ON audit_log(create_time);
CREATE INDEX IF NOT EXISTS idx_audit_log_success ON audit_log(success);

CREATE INDEX IF NOT EXISTS idx_oci_user_deleted ON oci_user(deleted);
CREATE INDEX IF NOT EXISTS idx_oci_user_create_time ON oci_user(create_time);
CREATE INDEX IF NOT EXISTS idx_oci_kv_type ON oci_kv(type);
CREATE INDEX IF NOT EXISTS idx_oci_create_task_status ON oci_create_task(status);
CREATE INDEX IF NOT EXISTS idx_oci_create_task_create_time ON oci_create_task(create_time);
