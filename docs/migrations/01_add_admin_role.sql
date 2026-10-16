-- =================================================================
-- Migration 01: Add ADMIN Role Support & Seed Initial Admin Account
-- Database: agro_dairy
-- Idempotent: Can be run multiple times safely
-- =================================================================

USE agro_dairy;

-- 1. Modify users role ENUM to include 'ADMIN' alongside 'VENDOR' and 'CUSTOMER'
ALTER TABLE users MODIFY COLUMN role ENUM('VENDOR', 'CUSTOMER', 'ADMIN') NOT NULL;

-- 2. Insert initial Administrator user if not already present
-- Password default: 'secret123' (BCrypt hashed)
INSERT INTO users (username, password_hash, full_name, email, phone, role, is_active)
SELECT 'admin_super', '$2a$12$n/0MUkh8n/CpbmVoJJ7q4.ayNgIuj1z0fOoCSGenICVxvMvnLZqm.', 'System Super Administrator', 'admin@agrodairy.com', '9800000000', 'ADMIN', 1
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin_super');

-- Ensure admin_super has the exact valid BCrypt hash for secret123
UPDATE users 
SET password_hash = '$2a$12$n/0MUkh8n/CpbmVoJJ7q4.ayNgIuj1z0fOoCSGenICVxvMvnLZqm.' 
WHERE username = 'admin_super';
