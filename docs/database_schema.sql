-- =================================================================
-- Smart Dairy Management System - Database Schema Script
-- Database Engine: MySQL 8.0+
-- Standard: 3rd Normal Form (3NF) compliant with ACID Guarantees
-- =================================================================

CREATE DATABASE IF NOT EXISTS agro_dairy_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE agro_dairy_db;

-- Enable Foreign Key Checks
SET FOREIGN_KEY_CHECKS = 0;

-- Drop Existing Tables in Reverse Dependency Order for Clean Initialization
DROP TABLE IF EXISTS faq_knowledge_base;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS holiday_requests;
DROP TABLE IF EXISTS extra_orders;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS monthly_bills;
DROP TABLE IF EXISTS delivery_records;
DROP TABLE IF EXISTS inventory;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS vendors;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------
-- 1. Users Table (Core Authentication & Role Management)
-- -----------------------------------------------------------------
CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20) NOT NULL,
    role ENUM('VENDOR', 'CUSTOMER') NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_username (username),
    INDEX idx_user_role (role)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------
-- 2. Vendors Table (Vendor Profile & Business Details)
-- -----------------------------------------------------------------
CREATE TABLE vendors (
    vendor_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    vendor_code VARCHAR(30) NOT NULL UNIQUE,
    business_name VARCHAR(100) NOT NULL,
    owner_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    address TEXT,
    gst_number VARCHAR(30),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    INDEX idx_vendor_code (vendor_code)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------
-- 3. Customers Table (Customer Master Record & Subscriptions)
-- -----------------------------------------------------------------
CREATE TABLE customers (
    customer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE,
    vendor_id BIGINT NOT NULL,
    customer_code VARCHAR(30) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    house_number VARCHAR(50),
    street VARCHAR(100),
    area VARCHAR(100) NOT NULL,
    city VARCHAR(50) NOT NULL DEFAULT 'Nagpur',
    pincode VARCHAR(10) NOT NULL,
    address TEXT,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    milk_type ENUM('COW', 'BUFFALO', 'MIXED') NOT NULL DEFAULT 'COW',
    morning_quantity DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    evening_quantity DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    delivery_shift ENUM('MORNING', 'EVENING', 'BOTH') NOT NULL DEFAULT 'MORNING',
    current_price_per_liter DECIMAL(8,2) NOT NULL DEFAULT 60.00,
    status ENUM('ACTIVE', 'INACTIVE', 'PAUSED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (vendor_id) REFERENCES vendors(vendor_id) ON DELETE RESTRICT,
    INDEX idx_customer_code (customer_code),
    INDEX idx_customer_phone (phone),
    INDEX idx_customer_area (area),
    INDEX idx_customer_status (status)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------
-- 4. Products Table (Master Dairy Products Catalog with ProductUnit)
-- -----------------------------------------------------------------
CREATE TABLE products (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(30) NOT NULL UNIQUE,
    product_name VARCHAR(100) NOT NULL,
    category ENUM('MILK', 'DAIRY_PRODUCT', 'BY_PRODUCT') NOT NULL,
    unit ENUM('LITER', 'KG', 'GRAM', 'PACKET') NOT NULL, -- ProductUnit Standardization
    price DECIMAL(8,2) NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_product_code (product_code),
    INDEX idx_product_category (category)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------
-- 5. Inventory Table (Stock Tracking & Reorder Alerts)
-- -----------------------------------------------------------------
CREATE TABLE inventory (
    inventory_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT UNIQUE NOT NULL,
    opening_stock DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    current_stock DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    minimum_stock DECIMAL(10,2) NOT NULL DEFAULT 10.00,
    reorder_level DECIMAL(10,2) NOT NULL DEFAULT 15.00,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE RESTRICT,
    INDEX idx_inventory_reorder (current_stock, reorder_level)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------
-- 6. Delivery Records Table (Daily Shift Delivery & Rate Snapshot)
-- -----------------------------------------------------------------
CREATE TABLE delivery_records (
    delivery_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    delivery_date DATE NOT NULL,
    delivery_shift ENUM('MORNING', 'EVENING') NOT NULL,
    delivered_quantity DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    rate_per_liter DECIMAL(8,2) NOT NULL, -- CRITICAL: PRICE SNAPSHOT AT DELIVERY TIME
    total_amount DECIMAL(10,2) NOT NULL,  -- delivered_quantity * rate_per_liter
    status ENUM('DELIVERED', 'NOT_DELIVERED', 'PAUSED') NOT NULL DEFAULT 'DELIVERED',
    remarks VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT,
    UNIQUE KEY uk_customer_shift_date (customer_id, delivery_date, delivery_shift),
    INDEX idx_delivery_date_shift (delivery_date, delivery_shift),
    INDEX idx_delivery_customer (customer_id, delivery_date)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------
-- 7. Monthly Bills Table (Automated Monthly Ledger Summaries)
-- -----------------------------------------------------------------
CREATE TABLE monthly_bills (
    bill_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_number VARCHAR(30) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    billing_month INT NOT NULL, -- 1 to 12
    billing_year INT NOT NULL,  -- e.g. 2026
    total_milk_quantity DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    milk_charges DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    extra_orders_charges DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    holiday_deductions DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    gross_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    previous_balance DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    net_payable DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    balance_due DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status ENUM('UNPAID', 'PARTIAL', 'PAID') NOT NULL DEFAULT 'UNPAID',
    bill_date DATE NOT NULL,
    due_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT,
    UNIQUE KEY uk_customer_billing_period (customer_id, billing_month, billing_year),
    INDEX idx_bill_status (status),
    INDEX idx_bill_number (bill_number)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------
-- 8. Payments Table (Customer Payments & Ledger Credits)
-- -----------------------------------------------------------------
CREATE TABLE payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    amount_paid DECIMAL(10,2) NOT NULL,
    payment_date DATE NOT NULL,
    payment_mode ENUM('CASH', 'UPI', 'NET_BANKING', 'CREDIT') NOT NULL DEFAULT 'CASH',
    transaction_reference VARCHAR(100),
    status ENUM('SUCCESS', 'PENDING', 'FAILED') NOT NULL DEFAULT 'SUCCESS',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bill_id) REFERENCES monthly_bills(bill_id) ON DELETE RESTRICT,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT,
    INDEX idx_payment_bill (bill_id),
    INDEX idx_payment_customer (customer_id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------
-- 9. Extra Orders Table (Customer Extra Product & Milk Orders)
-- -----------------------------------------------------------------
CREATE TABLE extra_orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity DECIMAL(8,2) NOT NULL,
    price_snapshot DECIMAL(8,2) NOT NULL, -- PRICE SNAPSHOT AT ORDER TIME
    total_price DECIMAL(10,2) NOT NULL,   -- quantity * price_snapshot
    order_date DATE NOT NULL,
    delivery_date DATE NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'DELIVERED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE RESTRICT,
    INDEX idx_order_customer (customer_id),
    INDEX idx_order_delivery_date (delivery_date)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------
-- 10. Holiday Requests Table (Delivery Pause Applications)
-- -----------------------------------------------------------------
CREATE TABLE holiday_requests (
    request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(255),
    approval_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    vendor_remarks VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT,
    INDEX idx_holiday_customer (customer_id),
    INDEX idx_holiday_dates (start_date, end_date)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------
-- 11. Notifications Table (System Notifications & Reminders)
-- -----------------------------------------------------------------
CREATE TABLE notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    category ENUM('CUSTOMER', 'VENDOR', 'DELIVERY', 'PAYMENTS', 'REQUESTS') NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_notification_user (user_id, is_read)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------
-- 12. FAQ Knowledge Base Table (Offline Dairy Q&A Repository)
-- -----------------------------------------------------------------
CREATE TABLE faq_knowledge_base (
    faq_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    keywords VARCHAR(255) NOT NULL,
    tags VARCHAR(255),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FULLTEXT idx_faq_search (question, keywords, tags)
) ENGINE=InnoDB;

-- =================================================================
-- SAMPLE SEED DATA INSERTIONS (REALISTIC NAGPUR LOCATION DATA)
-- =================================================================

-- 1. Users (1 Vendor User + 10 Customer Users)
INSERT INTO users (username, password_hash, full_name, email, phone, role) VALUES 
('vendor_ramesh', '$2a$12$K8C.k2B.P8p5O6hJ3iN1e.1P4X.sKzR6D2Y0wR8S.7e7y0Y1u5W4O', 'Ramesh Dairy Farm Owner', 'ramesh@agrodairy.com', '9822011223', 'VENDOR'),
('cust_rahul',    '$2a$12$K8C.k2B.P8p5O6hJ3iN1e.1P4X.sKzR6D2Y0wR8S.7e7y0Y1u5W4O', 'Rahul Verma', 'rahul@gmail.com', '9876500001', 'CUSTOMER'),
('cust_sunita',   '$2a$12$K8C.k2B.P8p5O6hJ3iN1e.1P4X.sKzR6D2Y0wR8S.7e7y0Y1u5W4O', 'Sunita Deshmukh', 'sunita@gmail.com', '9876500002', 'CUSTOMER'),
('cust_vikram',   '$2a$12$K8C.k2B.P8p5O6hJ3iN1e.1P4X.sKzR6D2Y0wR8S.7e7y0Y1u5W4O', 'Vikram Joshi', 'vikram@gmail.com', '9876500003', 'CUSTOMER'),
('cust_mahesh',   '$2a$12$K8C.k2B.P8p5O6hJ3iN1e.1P4X.sKzR6D2Y0wR8S.7e7y0Y1u5W4O', 'Mahesh Patil', 'mahesh@gmail.com', '9876500004', 'CUSTOMER'),
('cust_anita',    '$2a$12$K8C.k2B.P8p5O6hJ3iN1e.1P4X.sKzR6D2Y0wR8S.7e7y0Y1u5W4O', 'Anita Shinde', 'anita@gmail.com', '9876500005', 'CUSTOMER'),
('cust_sanjay',   '$2a$12$K8C.k2B.P8p5O6hJ3iN1e.1P4X.sKzR6D2Y0wR8S.7e7y0Y1u5W4O', 'Sanjay Kulkarni', 'sanjay@gmail.com', '9876500006', 'CUSTOMER'),
('cust_priya',    '$2a$12$K8C.k2B.P8p5O6hJ3iN1e.1P4X.sKzR6D2Y0wR8S.7e7y0Y1u5W4O', 'Priya Nair', 'priya@gmail.com', '9876500007', 'CUSTOMER'),
('cust_amit',     '$2a$12$K8C.k2B.P8p5O6hJ3iN1e.1P4X.sKzR6D2Y0wR8S.7e7y0Y1u5W4O', 'Amit Shah', 'amit@gmail.com', '9876500008', 'CUSTOMER'),
('cust_pooja',    '$2a$12$K8C.k2B.P8p5O6hJ3iN1e.1P4X.sKzR6D2Y0wR8S.7e7y0Y1u5W4O', 'Pooja Mehta', 'pooja@gmail.com', '9876500009', 'CUSTOMER'),
('cust_rajesh',   '$2a$12$K8C.k2B.P8p5O6hJ3iN1e.1P4X.sKzR6D2Y0wR8S.7e7y0Y1u5W4O', 'Rajesh Pawar', 'rajesh@gmail.com', '9876500010', 'CUSTOMER');

-- 2. Vendor Record (Nagpur Headquarters)
INSERT INTO vendors (user_id, vendor_code, business_name, owner_name, phone, email, address, gst_number) VALUES
(1, 'VEND-101', 'Ramesh Agro Dairy Services', 'Ramesh Dairy Farm Owner', '9822011223', 'ramesh@agrodairy.com', 'Plot 12, WHC Road, Dharampeth, Nagpur, Maharashtra - 440010', '27ABCDE1234F1ZH');

-- 3. 10 Customers (Nagpur Areas with Exact Coordinates)
INSERT INTO customers (user_id, vendor_id, customer_code, full_name, phone, house_number, street, area, city, pincode, address, latitude, longitude, milk_type, morning_quantity, evening_quantity, delivery_shift, current_price_per_liter, status) VALUES
(2,  1, 'CUST-1001', 'Rahul Verma',    '9876500001', 'Flat 402', 'Sunshine Apts',    'Dharampeth',     'Nagpur', '440010', 'Flat 402, Sunshine Apts, Dharampeth, Nagpur',     21.1415, 79.0634, 'COW',     1.50, 1.00, 'BOTH',    60.00, 'ACTIVE'),
(3,  1, 'CUST-1002', 'Sunita Deshmukh','9876500002', 'Bungalow 12','Ring Road',      'Pratap Nagar',   'Nagpur', '440022', 'Bungalow 12, Ring Road, Pratap Nagar, Nagpur',   21.1195, 79.0558, 'BUFFALO', 2.00, 2.00, 'BOTH',    75.00, 'ACTIVE'),
(4,  1, 'CUST-1003', 'Vikram Joshi',   '9876500003', 'Flat 101', 'Green Acres',     'Manish Nagar',   'Nagpur', '440015', 'Flat 101, Green Acres, Manish Nagar, Nagpur',   21.0875, 79.0765, 'MIXED',   1.00, 0.00, 'MORNING', 55.00, 'PAUSED'),
(5,  1, 'CUST-1004', 'Mahesh Patil',   '9876500004', 'Shop No 5', 'Residency Road',  'Sadar',          'Nagpur', '440001', 'Shop No 5, Residency Road, Sadar, Nagpur',        21.1610, 79.0805, 'BUFFALO', 5.00, 5.00, 'BOTH',    75.00, 'ACTIVE'),
(6,  1, 'CUST-1005', 'Anita Shinde',   '9876500005', 'Plot 88',  'NIT Layout',      'Trimurti Nagar', 'Nagpur', '440022', 'Plot 88, NIT Layout, Trimurti Nagar, Nagpur',     21.1167, 79.0435, 'COW',     1.00, 1.00, 'BOTH',    60.00, 'ACTIVE'),
(7,  1, 'CUST-1006', 'Sanjay Kulkarni','9876500006', 'Flat 304', 'Hasanbagh Rd',    'Nandanvan',      'Nagpur', '440009', 'Flat 304, Hasanbagh Rd, Nandanvan, Nagpur',       21.1350, 79.1215, 'COW',     2.00, 0.00, 'MORNING', 60.00, 'ACTIVE'),
(8,  1, 'CUST-1007', 'Priya Nair',     '9876500007', 'Flat 501', 'Somalwada',       'Wardha Road',    'Nagpur', '440015', 'Flat 501, Somalwada, Wardha Road, Nagpur',        21.0965, 79.0712, 'BUFFALO', 1.50, 1.50, 'BOTH',    75.00, 'ACTIVE'),
(9,  1, 'CUST-1008', 'Amit Shah',      '9876500008', 'Row House 4','MIDC Area',      'Hingna Road',    'Nagpur', '440016', 'Row House 4, MIDC Area, Hingna Road, Nagpur',     21.1120, 79.0185, 'COW',     3.00, 2.00, 'BOTH',    60.00, 'ACTIVE'),
(10, 1, 'CUST-1009', 'Pooja Mehta',    '9876500009', 'Flat 202', 'Abhyankar Nagar','Bajaj Nagar',    'Nagpur', '440010', 'Flat 202, Abhyankar Nagar, Bajaj Nagar, Nagpur',  21.1275, 79.0650, 'MIXED',   1.00, 1.00, 'BOTH',    55.00, 'ACTIVE'),
(11, 1, 'CUST-1010', 'Rajesh Pawar',   '9876500010', 'Bungalow 7', 'Central Bazaar','Ramdaspeth',     'Nagpur', '440010', 'Bungalow 7, Central Bazaar, Ramdaspeth, Nagpur',  21.1355, 79.0740, 'BUFFALO', 2.50, 0.00, 'MORNING', 75.00, 'ACTIVE');

-- 4. Standardized Products & Inventory (ProductUnit Enum Integration)
INSERT INTO products (product_code, product_name, category, unit, price, status) VALUES
('PRD-101', 'Fresh Cow Milk',      'MILK',          'LITER',   60.00, 'ACTIVE'),
('PRD-102', 'Fresh Buffalo Milk',  'MILK',          'LITER',   75.00, 'ACTIVE'),
('PRD-103', 'Fresh Malai Paneer',  'DAIRY_PRODUCT', 'KG',     360.00, 'ACTIVE'),
('PRD-104', 'Set Thick Curd',      'DAIRY_PRODUCT', 'KG',      90.00, 'ACTIVE'),
('PRD-105', 'Pure Cow Ghee',       'DAIRY_PRODUCT', 'KG',     650.00, 'ACTIVE'),
('PRD-106', 'Fresh Salted Butter', 'DAIRY_PRODUCT', 'GRAM',   280.00, 'ACTIVE'),
('PRD-107', 'Processed Cheese Block','DAIRY_PRODUCT','PACKET', 140.00, 'ACTIVE');

INSERT INTO inventory (product_id, opening_stock, current_stock, minimum_stock, reorder_level) VALUES
(1, 500.00, 320.00, 50.00, 100.00),
(2, 400.00, 210.00, 40.00, 80.00),
(3,  50.00,   8.00,  5.00, 10.00), -- Low Stock Alert (< 10)
(4, 100.00,  45.00, 10.00, 20.00),
(5,  80.00,  35.00, 10.00, 15.00),
(6,  60.00,  25.00,  5.00, 10.00),
(7,  40.00,  18.00,  5.00, 10.00);

-- 5. Sample Delivery Records (With Rate Snapshotting)
INSERT INTO delivery_records (customer_id, delivery_date, delivery_shift, delivered_quantity, rate_per_liter, total_amount, status, remarks) VALUES
(1, '2026-08-01', 'MORNING', 1.50, 60.00,  90.00, 'DELIVERED', 'Delivered at doorstep'),
(1, '2026-08-01', 'EVENING', 1.00, 60.00,  60.00, 'DELIVERED', 'Delivered on time'),
(2, '2026-08-01', 'MORNING', 2.00, 75.00, 150.00, 'DELIVERED', 'Fresh buffalo milk'),
(2, '2026-08-01', 'EVENING', 2.00, 75.00, 150.00, 'DELIVERED', 'Delivered'),
(3, '2026-08-01', 'MORNING', 0.00, 55.00,   0.00, 'PAUSED',    'Customer on vacation'),
(4, '2026-08-01', 'MORNING', 5.00, 75.00, 375.00, 'DELIVERED', 'Bulk morning order');

-- 6. Sample Monthly Bills & Payments
INSERT INTO monthly_bills (bill_number, customer_id, billing_month, billing_year, total_milk_quantity, milk_charges, extra_orders_charges, holiday_deductions, gross_amount, previous_balance, net_payable, paid_amount, balance_due, status, bill_date, due_date) VALUES
('BILL-202607-1001', 1, 7, 2026, 75.00, 4500.00, 360.00, 0.00, 4860.00, 0.00, 4860.00, 4860.00, 0.00, 'PAID',   '2026-08-01', '2026-08-10'),
('BILL-202607-1002', 2, 7, 2026, 120.00, 9000.00,  0.00, 0.00, 9000.00, 0.00, 9000.00, 5000.00, 4000.00, 'PARTIAL', '2026-08-01', '2026-08-10');

INSERT INTO payments (bill_id, customer_id, amount_paid, payment_date, payment_mode, transaction_reference, status) VALUES
(1, 1, 4860.00, '2026-08-02', 'UPI', 'UPI/20260802/12998391', 'SUCCESS'),
(2, 2, 5000.00, '2026-08-02', 'CASH', 'CASH-REC-1092', 'SUCCESS');

-- 7. Extra Orders & Holiday Requests
INSERT INTO extra_orders (customer_id, product_id, quantity, price_snapshot, total_price, order_date, delivery_date, status) VALUES
(1, 3, 1.00, 360.00, 360.00, '2026-08-01', '2026-08-02', 'DELIVERED'),
(2, 5, 0.50, 650.00, 325.00, '2026-08-01', '2026-08-03', 'PENDING');

INSERT INTO holiday_requests (customer_id, start_date, end_date, reason, approval_status, vendor_remarks) VALUES
(3, '2026-08-01', '2026-08-07', 'Out of station for family wedding', 'APPROVED', 'Approved by Ramesh Vendor');

-- 8. Notifications
INSERT INTO notifications (user_id, title, message, category, is_read) VALUES
(1, 'Low Stock Warning', 'Paneer stock (8 Kg) is below reorder level (10 Kg).', 'VENDOR', FALSE),
(2, 'Bill Generated', 'Your July 2026 bill BILL-202607-1001 of Rs. 4,860 is paid.', 'CUSTOMER', TRUE);

-- 9. FAQ Knowledge Base (Offline Content)
INSERT INTO faq_knowledge_base (question, answer, category, keywords, tags) VALUES
('How should I boil and store fresh cow milk to prevent spoiling?', 'Always boil fresh raw milk within 2 hours of delivery on medium heat. Allow it to cool down naturally to room temperature before covering and storing in the refrigerator at 4 deg C or lower.', 'Milk Storage', 'boil, store, raw milk, refrigeration, spoil, temperature', 'Customer, Preservation'),
('What is the typical shelf life of fresh malai paneer?', 'Unopened fresh paneer lasts up to 3 to 5 days in a refrigerator. Once opened, submerge unused paneer in a bowl of fresh clean water inside the fridge and change the water daily to keep it soft and fresh for up to 7 days.', 'Paneer Care', 'paneer, shelf life, fridge, store, soft, preservation', 'Customer, Dairy Care'),
('What are the key benefits of drinking pure A2 Cow Milk?', 'A2 Cow milk contains A2 beta-casein protein which is easier to digest, reduces gut inflammation, and contains rich essential minerals like Calcium, Vitamin D, and Vitamin B12 suitable for young children and elderly adults.', 'Nutritional Info', 'cow milk, A2 milk, benefits, digestion, calcium, vitamins', 'Customer, Health'),
('What is the ideal feeding ratio for dairy cattle to increase fat and SNF percentage?', 'A balanced cattle feed ratio consists of 60% Green Fodder (Legumes/Maize), 25% Dry Fodder (Wheat Straw), and 15% Concentrated Feed mix with mineral mixture and salt. Maintaining high fiber intake enhances milk Fat percentage.', 'Cattle Feeding', 'cattle feed, fat, SNF, green fodder, dry fodder, mineral mix', 'Vendor, Farm Care');
