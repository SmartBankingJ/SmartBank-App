CREATE DATABASE IF NOT EXISTS bankdirect CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bankdirect;

CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    cin VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(120),
    phone VARCHAR(30),
    address VARCHAR(255),
    advisor_id BIGINT NULL
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(60) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failed_attempts INT NOT NULL DEFAULT 0,
    customer_id BIGINT NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    rib VARCHAR(30) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'MAD',
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    opened_at DATETIME NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    amount DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    balance_after DECIMAL(15,2) NOT NULL,
    label VARCHAR(255),
    reference VARCHAR(40) NOT NULL,
    counterpart_rib VARCHAR(30),
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS beneficiaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    rib VARCHAR(30) NOT NULL,
    bank_name VARCHAR(80),
    created_at DATETIME NOT NULL,
    UNIQUE (customer_id, rib),
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);
