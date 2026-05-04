-- Update schema for King-Detective v3.0.0

-- Add deleted column to oci_user table (default 0 = active)
ALTER TABLE oci_user ADD COLUMN deleted INTEGER DEFAULT 0;

-- Ensure indexes exist
CREATE INDEX IF NOT EXISTS idx_oci_user_username ON oci_user(username);
CREATE INDEX IF NOT EXISTS idx_oci_user_deleted ON oci_user(deleted);

-- Verify update
PRAGMA table_info(oci_user);
