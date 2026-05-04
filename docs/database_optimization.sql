-- Database Performance Optimization
-- Add indexes for frequently queried columns

-- IP Blacklist table indexes
CREATE INDEX IF NOT EXISTS idx_ip_blacklist_ip ON ip_blacklist(ip_address);
CREATE INDEX IF NOT EXISTS idx_ip_blacklist_create_time ON ip_blacklist(create_time);

-- Login Attempts table indexes
CREATE INDEX IF NOT EXISTS idx_login_attempts_ip ON login_attempts(ip_address);
CREATE INDEX IF NOT EXISTS idx_login_attempts_time ON login_attempts(attempt_time);

-- OCI KV table index (for defense_mode queries)
CREATE INDEX IF NOT EXISTS idx_oci_kv_code ON oci_kv(code);

-- Optimize existing queries
ANALYZE ip_blacklist;
ANALYZE login_attempts;
ANALYZE oci_kv;

-- Cleanup old login attempts (>30 days)
DELETE FROM login_attempts WHERE attempt_time < datetime('now', '-30 days');

-- Vacuum to reclaim space
VACUUM;
