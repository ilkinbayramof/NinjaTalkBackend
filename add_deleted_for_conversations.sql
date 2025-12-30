-- Migration: Add deleted_for column to conversations table
-- Purpose: Enable per-user conversation deletion (soft delete)

ALTER TABLE conversations 
ADD COLUMN IF NOT EXISTS deleted_for TEXT DEFAULT '';

-- Verify
SELECT column_name, data_type, column_default 
FROM information_schema.columns 
WHERE table_name = 'conversations';
