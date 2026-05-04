-- 审计日志表
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

-- 创建索引以提升查询性能
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_operation ON audit_log(operation);
CREATE INDEX IF NOT EXISTS idx_audit_log_create_time ON audit_log(create_time);
CREATE INDEX IF NOT EXISTS idx_audit_log_success ON audit_log(success);

-- 为现有表添加索引优化（如果不存在）
CREATE INDEX IF NOT EXISTS idx_oci_user_deleted ON oci_user(deleted);
CREATE INDEX IF NOT EXISTS idx_oci_user_create_time ON oci_user(create_time);
CREATE INDEX IF NOT EXISTS idx_oci_kv_type ON oci_kv(type);
CREATE INDEX IF NOT EXISTS idx_oci_create_task_status ON oci_create_task(status);
CREATE INDEX IF NOT EXISTS idx_oci_create_task_create_time ON oci_create_task(create_time);
