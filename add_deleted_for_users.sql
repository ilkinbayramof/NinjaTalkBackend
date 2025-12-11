-- Migration: Add deleted_for_users column to messages table
-- Purpose: Enable per-user message deletion (WhatsApp-style "delete for me")

ALTER TABLE messages 
ADD COLUMN IF NOT EXISTS deleted_for_users TEXT DEFAULT '';

-- Verify
SELECT column_name, data_type, column_default 
FROM information_schema.columns 
WHERE table_name = 'messages';
