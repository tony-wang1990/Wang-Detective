--用户表
create table if not exists `oci_user`
(
    id                 varchar(64)                                     not null,
    username           varchar(64)                                     null,
    tenant_name        varchar(64)                                     null,
    tenant_create_time datetime                                        null,
    oci_tenant_id      varchar(64)                                     null,
    oci_user_id        varchar(64)                                     null,
    oci_fingerprint    varchar(64)                                     not null,
    oci_region         varchar(32)                                     not null,
    oci_key_path       varchar(256)                                    not null,
    deleted            INTEGER     DEFAULT 0,
    create_time        datetime default (datetime('now', 'localtime')) not null,
    primary key ("id")
);
CREATE INDEX if not exists oci_user_create_time ON oci_user (create_time DESC);

--开机任务表
create table if not exists `oci_create_task`
(
    id               varchar(64)                                        not null,
    user_id          varchar(64)                                        null,
    oci_region       varchar(64)                                        null,
    ocpus            REAL        DEFAULT 1.0,
    memory           REAL        DEFAULT 6.0,
    disk             INTEGER     DEFAULT 50,
    architecture     varchar(64) DEFAULT 'ARM',
    interval         INTEGER     DEFAULT 60,
    create_numbers   INTEGER     DEFAULT 1,
    root_password    varchar(64),
    operation_system varchar(64) DEFAULT 'Ubuntu',
    create_time      datetime    default (datetime('now', 'localtime')) not null,
    primary key ("id")
);
CREATE INDEX if not exists oci_create_task_create_time ON oci_create_task (create_time DESC);

--键值表
create table if not exists `oci_kv`
(
    id          varchar(64)                                     not null,
    code        varchar(64)                                     not null,
    value       text                                            null,
    type        varchar(64)                                     not null,
    create_time datetime default (datetime('now', 'localtime')) not null,
    primary key ("id")
);
CREATE INDEX if not exists oci_kv_code ON oci_kv (code DESC);
CREATE INDEX if not exists oci_kv_type ON oci_kv (type DESC);
CREATE INDEX if not exists oci_kv_create_time ON oci_kv (create_time DESC);

--CF配置表
create table if not exists `cf_cfg`
(
    id          varchar(64)                                     not null,
    domain      varchar(64)                                     not null,
    zone_id     varchar(255)                                    not null,
    api_token   varchar(255)                                    not null,
    create_time datetime default (datetime('now', 'localtime')) not null,
    primary key ("id")
);

-- IP数据表
create table if not exists `ip_data`
(
    id          varchar(64)                                     not null,
    ip          varchar(255)                                    not null,
    country     varchar(255)                                    null,
    area        varchar(120)                                    null,
    city        varchar(120)                                    null,
    org         varchar(120)                                    null,
    asn         varchar(64)                                     null,
    type        varchar(64)                                     null,
    lat         REAL,
    lng         REAL,
    create_time datetime default (datetime('now', 'localtime')) not null,
    primary key ("id")
);

-- 登录尝试表 (防爆破)
create table if not exists `login_attempts`
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    ip_address   varchar(255)                                    null,
    attempt_count INTEGER                                        DEFAULT 0,
    last_attempt datetime                                        null
);

-- IP黑名单表
create table if not exists `ip_blacklist`
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    ip_address  varchar(255)                                    null,
    reason      varchar(255)                                    null,
    banned_by   varchar(255)                                    null,
    create_time datetime default (datetime('now', 'localtime')) not null
);

-- 审计日志表
CREATE TABLE IF NOT EXISTS `audit_log` (
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

-- 创建索引
CREATE INDEX if not exists idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX if not exists idx_audit_log_operation ON audit_log(operation);
CREATE INDEX if not exists idx_audit_log_create_time ON audit_log(create_time);
CREATE INDEX if not exists idx_audit_log_success ON audit_log(success);
