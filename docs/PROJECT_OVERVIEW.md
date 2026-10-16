# Project Overview — Smart Dairy Management System

## System Summary
The **Smart Dairy Management System** is a professional desktop application designed for small-to-medium dairy farm owners, doorstep milk distributors, and individual consumers. It automates daily morning/evening milk subscription deliveries, extra product ordering, holiday pause requests, monthly ledger calculation with immutable rate snapshotting, PDF invoice generation, Excel financial exports, and offline route optimization.

## Key Modules & Features

### 1. Authentication & Security Module
- **Role-based Access Control (RBAC)**: Distinguishes between `ADMIN`, `VENDOR`, and `CUSTOMER` roles with strict route and permission separation.
- **Secure Password Hashing**: Utilizes BCrypt hashing algorithm (`jbcrypt`) for password storage and verification.
- **Session Management**: Thread-safe `SessionManager` for maintaining authenticated user state with `isAdmin()`, `isVendor()`, and `isCustomer()` guards.
- **Inactive Account Protection**: Blocks login for accounts marked `is_active = FALSE`.

### 2. Administration & Executive Module
- **Executive Dashboard**: Dynamic metrics for total platform users, active vendors, active customers, active catalog products, and verified non-duplicated total revenue.
- **User Directory & Access Control**: Real-time searchable and filterable user account table with one-click Activate/Deactivate actions, self-deactivation protection, and last-admin preservation.
- **Master Product Catalog**: Read-only centralized catalog of standardized dairy items, units, pricing, and live inventory levels.
- **Live System Health & Diagnostics**: Real-time diagnostics for MySQL connection status, ping latency (ms), HikariCP pool health, JVM heap memory usage, and repository data verification.

### 3. Vendor Operations Module
- **Dashboard & Key Metrics**: High-level statistics on active customer subscriptions, daily milk volume delivered, pending extra product orders, and total revenue collected.
- **Customer Management**: CRUD operations for managing customer profiles, geographic coordinates (latitude & longitude), delivery shifts, and milk rates per liter.
- **Daily Shift Delivery Management**: Record morning and evening shift deliveries turn-by-turn. Preserves immutable price snapshots at the time of delivery.
- **Extra Product Order Management**: Review, approve, or reject customer orders for extra dairy products (Paneer, Curd, Ghee, Butter, Cheese). Approving an order automatically reduces stock inventory.
- **Holiday Pause Management**: Review customer holiday pause applications with automated cut-off validation.
- **Monthly Billing & Payment Collection**: Generate monthly bills, record partial/full payments (UPI, Cash, NetBanking, Credit), export PDF invoices, and export financial ledgers to Excel.

### 3. Customer Portal Module
- **Dashboard Overview**: Access personal delivery schedule, current rate per liter, milk type, and outstanding dues balance.
- **Delivery Log**: View historical delivery records by month and shift with complete transparency.
- **Extra Order Submission**: Place extra product orders with standardized units (`LITER`, `KG`, `GRAM`, `PACKET`).
- **Holiday Pause Application**: Pause daily deliveries before 10 PM cut-off for upcoming vacations.
- **Monthly Bill Statements**: View bill breakdown and download official PDF invoice statements.

### 4. Offline Smart Features
- **Offline Route Planner**: Offline Dijkstra / Nearest-Neighbor shortest path calculation engine using Haversine formula based on customer lat/long coordinates. Starts from Dairy HQ (Dharampeth, Nagpur) and calculates distance and turn-by-turn stop order.
- **Offline Dairy Knowledge Assistant**: Completely offline Q&A assistant powered by TF-IDF keyword matching over `dairy_knowledge_base.json`. Covers milk boiling, storage, shelf life, paneer care, curd storage, dairy hygiene, cow health, and cattle feeding ratios.

### 5. Reporting & Integration
- **PDF Invoice Generation**: Generates readable PDF invoices using OpenPDF library.
- **Excel Financial Export**: Exports monthly billing records to `.xlsx` using Apache POI.
- **Optional Firebase Sync**: Optional cloud backup that gracefully checks configuration without hardcoded credentials, leaving local MySQL as primary source of truth.
