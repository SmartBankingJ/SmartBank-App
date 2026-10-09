USE bankdirect;

-- Client de test (Karim Alami)
INSERT INTO customers (id, first_name, last_name, cin, email, phone, address) 
VALUES (1, 'Karim', 'Alami', 'BE123456', 'karim.alami@email.ma', '0661001122', 'Casablanca, Bd d''Anfa')
ON DUPLICATE KEY UPDATE id=id;

-- User login: user "karim" / password "123456" (en clair ou hash simulé pour l'instant)
INSERT INTO users (id, username, password_hash, role, status, customer_id)
VALUES (1, 'karim', '123456', 'CUSTOMER', 'ACTIVE', 1)
ON DUPLICATE KEY UPDATE id=id;

-- 2 Comptes pour Karim (Compte Chèque + Épargne)
INSERT INTO accounts (id, customer_id, rib, type, currency, balance, status, opened_at)
VALUES 
(1, 1, '011780000012345678901234', 'CURRENT', 'MAD', 25450.00, 'ACTIVE', NOW()),
(2, 1, '011780000098765432109876', 'SAVINGS', 'MAD', 120000.00, 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE id=id;

-- Bénéficiaire enregistré
INSERT INTO beneficiaries (id, customer_id, full_name, rib, bank_name, created_at)
VALUES (1, 1, 'Fatima Zahra Benali', '225780000055443322110099', 'Attijariwafa Bank', NOW())
ON DUPLICATE KEY UPDATE id=id;
