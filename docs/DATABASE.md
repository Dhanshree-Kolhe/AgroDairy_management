# Database Specification & Schema Overview — Smart Dairy Management System

## Engine & Standards
- **Database Engine**: MySQL 8.0+
- **Storage Engine**: InnoDB (ACID compliant)
- **Normal Form**: 3rd Normal Form (3NF) compliant
- **Character Set**: `utf8mb4_unicode_ci`

---

## Entity Relationship & Schema Design

### 1. `users` (Core Authentication & Role Management)
- `user_id` (BIGINT, PK, Auto Increment)
- `username` (VARCHAR(50), Unique)
- `password_hash` (VARCHAR(255))
- `full_name` (VARCHAR(100))
- `email` (VARCHAR(100), Unique)
- `phone` (VARCHAR(20))
- `role` (ENUM: `'ADMIN'`, `'VENDOR'`, `'CUSTOMER'`)
- `is_active` (BOOLEAN)
- `created_at`, `updated_at` (TIMESTAMP)

### 2. `vendors` (Vendor Master Profile)
- `vendor_id` (BIGINT, PK, Auto Increment)
- `user_id` (BIGINT, FK -> `users.user_id`)
- `vendor_code` (VARCHAR(30), Unique)
- `business_name` (VARCHAR(100))
- `owner_name` (VARCHAR(100))
- `phone`, `email`, `address`, `gst_number`

### 3. `customers` (Customer Master & Subscription Details)
- `customer_id` (BIGINT, PK, Auto Increment)
- `user_id` (BIGINT, FK -> `users.user_id`)
- `vendor_id` (BIGINT, FK -> `vendors.vendor_id`)
- `customer_code` (VARCHAR(30), Unique)
- `full_name`, `phone`, `house_number`, `street`, `area`, `city`, `pincode`, `address`
- `latitude` (DECIMAL(10,8)), `longitude` (DECIMAL(11,8)) — Used by Dijkstra Route Planner
- `milk_type` (ENUM: `'COW'`, `'BUFFALO'`, `'MIXED'`)
- `morning_quantity` (DECIMAL(8,2)), `evening_quantity` (DECIMAL(8,2))
- `delivery_shift` (ENUM: `'MORNING'`, `'EVENING'`, `'BOTH'`)
- `current_price_per_liter` (DECIMAL(8,2))
- `status` (ENUM: `'ACTIVE'`, `'INACTIVE'`, `'PAUSED'`)

### 4. `products` (Dairy Products Master Catalog)
- `product_id` (BIGINT, PK, Auto Increment)
- `product_code` (VARCHAR(30), Unique)
- `product_name` (VARCHAR(100))
- `category` (ENUM: `'MILK'`, `'DAIRY_PRODUCT'`, `'BY_PRODUCT'`)
- `unit` (ENUM: `'LITER'`, `'KG'`, `'GRAM'`, `'PACKET'`) — ProductUnit Standardisation
- `price` (DECIMAL(8,2))
- `status` (ENUM: `'ACTIVE'`, `'INACTIVE'`)

### 5. `inventory` (Stock Management)
- `inventory_id` (BIGINT, PK, Auto Increment)
- `product_id` (BIGINT, FK -> `products.product_id`, Unique)
- `opening_stock`, `current_stock`, `minimum_stock`, `reorder_level` (DECIMAL(10,2))
- `last_updated` (TIMESTAMP)

### 6. `delivery_records` (Shift Delivery Log with Rate Snapshotting)
- `delivery_id` (BIGINT, PK, Auto Increment)
- `customer_id` (BIGINT, FK -> `customers.customer_id`)
- `delivery_date` (DATE), `delivery_shift` (ENUM: `'MORNING'`, `'EVENING'`)
- `delivered_quantity` (DECIMAL(8,2))
- `rate_per_liter` (DECIMAL(8,2)) — **Immutable Price Snapshot**
- `total_amount` (DECIMAL(10,2))
- `status` (ENUM: `'DELIVERED'`, `'NOT_DELIVERED'`, `'PAUSED'`)
- `remarks` (VARCHAR(255))
- Unique Key: (`customer_id`, `delivery_date`, `delivery_shift`)

### 7. `monthly_bills` (Automated Billing Ledger)
- `bill_id` (BIGINT, PK, Auto Increment)
- `bill_number` (VARCHAR(30), Unique)
- `customer_id` (BIGINT, FK -> `customers.customer_id`)
- `billing_month` (INT), `billing_year` (INT)
- `total_milk_quantity`, `milk_charges`, `extra_orders_charges`, `holiday_deductions`, `gross_amount`, `previous_balance`, `net_payable`, `paid_amount`, `balance_due` (DECIMAL(10,2))
- `status` (ENUM: `'UNPAID'`, `'PARTIAL'`, `'PAID'`)
- `bill_date`, `due_date` (DATE)

### 8. `payments` (Customer Payments)
- `payment_id` (BIGINT, PK, Auto Increment)
- `bill_id` (BIGINT, FK -> `monthly_bills.bill_id`)
- `customer_id` (BIGINT, FK -> `customers.customer_id`)
- `amount_paid` (DECIMAL(10,2))
- `payment_date` (DATE)
- `payment_mode` (ENUM: `'CASH'`, `'UPI'`, `'NET_BANKING'`, `'CREDIT'`)
- `transaction_reference` (VARCHAR(100))
- `status` (ENUM: `'SUCCESS'`, `'PENDING'`, `'FAILED'`)

### 9. `extra_orders` (Extra Product Orders)
- `order_id` (BIGINT, PK, Auto Increment)
- `customer_id` (BIGINT, FK -> `customers.customer_id`)
- `product_id` (BIGINT, FK -> `products.product_id`)
- `quantity` (DECIMAL(8,2))
- `price_snapshot` (DECIMAL(8,2)) — **Price Snapshot at Order Creation**
- `total_price` (DECIMAL(10,2))
- `order_date`, `delivery_date` (DATE)
- `status` (ENUM: `'PENDING'`, `'APPROVED'`, `'DELIVERED'`, `'REJECTED'`)

### 10. `holiday_requests` (Delivery Pause Applications)
- `request_id` (BIGINT, PK, Auto Increment)
- `customer_id` (BIGINT, FK -> `customers.customer_id`)
- `start_date`, `end_date` (DATE), `reason` (VARCHAR(255))
- `approval_status` (ENUM: `'PENDING'`, `'APPROVED'`, `'REJECTED'`)
- `vendor_remarks` (VARCHAR(255))

### 11. `notifications` & 12. `faq_knowledge_base`
- Notifications repository and offline FAQ knowledge base.
