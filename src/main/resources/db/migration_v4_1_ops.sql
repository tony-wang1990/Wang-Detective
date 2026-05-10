CREATE TABLE IF NOT EXISTS ops_ssh_host (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    host TEXT NOT NULL,
    port INTEGER DEFAULT 22,
    username TEXT NOT NULL,
    auth_type TEXT NOT NULL DEFAULT 'password',
    password_cipher TEXT,
    private_key_cipher TEXT,
    passphrase_cipher TEXT,
    tags TEXT,
    description TEXT,
    last_used_at DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ops_ssh_host_name ON ops_ssh_host(name);
CREATE INDEX IF NOT EXISTS idx_ops_ssh_host_host ON ops_ssh_host(host);
CREATE INDEX IF NOT EXISTS idx_ops_ssh_host_update_time ON ops_ssh_host(update_time);
