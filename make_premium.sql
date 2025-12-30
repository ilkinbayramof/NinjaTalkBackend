-- Update test4@gmail.com user to premium
UPDATE users SET is_premium = true WHERE email = 'test4@gmail.com';

-- Verify the update
SELECT id, email, is_premium FROM users WHERE email = 'test4@gmail.com';
