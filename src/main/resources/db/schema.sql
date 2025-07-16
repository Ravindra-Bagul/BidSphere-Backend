-- Drop database if exists and create new
DROP DATABASE IF EXISTS bidsphere;
CREATE DATABASE bidsphere;
USE bidsphere;

-- Create users table first (no dependencies)
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    user_type ENUM('SELLER', 'BIDDER') NOT NULL
);

-- Create auctions table (depends on users)
CREATE TABLE auctions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    item_condition VARCHAR(50) NOT NULL,
    location VARCHAR(100),
    starting_price DECIMAL(10, 2) NOT NULL,
    current_price DECIMAL(10, 2) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    image_url VARCHAR(500),
    status ENUM('ACTIVE', 'ENDED', 'CANCELLED') DEFAULT 'ACTIVE',
    bid_count INT DEFAULT 0,
    seller_id BIGINT NOT NULL,
    FOREIGN KEY (seller_id) REFERENCES users(id),
    INDEX idx_seller_id (seller_id),
    INDEX idx_status (status),
    INDEX idx_end_time (end_time)
);

-- Create bids table (depends on users and auctions)
CREATE TABLE bids (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    auction_id BIGINT NOT NULL,
    bidder_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    bid_time DATETIME NOT NULL,
    FOREIGN KEY (auction_id) REFERENCES auctions(id),
    FOREIGN KEY (bidder_id) REFERENCES users(id),
    INDEX idx_auction_id (auction_id),
    INDEX idx_bidder_id (bidder_id)
);

-- Add winning_bid reference to auctions
ALTER TABLE auctions 
ADD COLUMN winning_bid_id BIGINT,
ADD CONSTRAINT fk_winning_bid 
FOREIGN KEY (winning_bid_id) REFERENCES bids(id);

-- Create payments table (depends on all other tables)
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bid_id BIGINT NOT NULL,
    bidder_id BIGINT NOT NULL,
    auction_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status ENUM('PENDING', 'PAID', 'FAILED') DEFAULT 'PENDING',
    payment_time DATETIME NOT NULL,
    payment_method VARCHAR(50),
    transaction_id VARCHAR(100),
    FOREIGN KEY (bid_id) REFERENCES bids(id),
    FOREIGN KEY (bidder_id) REFERENCES users(id),
    FOREIGN KEY (auction_id) REFERENCES auctions(id),
    INDEX idx_status (status),
    INDEX idx_transaction_id (transaction_id)
);
