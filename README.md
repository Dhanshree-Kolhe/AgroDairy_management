# Smart Dairy Management System 🥛

A professional, offline-first Desktop Application built using **JavaFX**, **Java 17**, and **MySQL** for modern dairy business management, doorstep milk delivery tracking, automated monthly ledger calculation, PDF invoice generation, Excel reporting, and intelligent route optimization.

---

## 📌 Problem Statement & Objectives

Traditional dairy vendors and doorstep milk distributors face huge operational overheads in tracking daily shift milk deliveries, maintaining customer subscriptions, managing holiday pause requests, snapshotting historical milk prices, and compiling monthly bills manually.

### Core Objectives:
1. **Automate Daily Operations**: Manage vendor & customer profiles, subscriptions, and morning/evening shift delivery records.
2. **Accurate Price Snapshotting**: Ensure historical deliveries and extra product orders maintain immutable price snapshots at the time of delivery/order.
3. **100% Offline Capability**: Provide an offline Dijkstra-based delivery route planner and a keyword-matching Dairy Knowledge Assistant that work completely without internet access.
4. **Automated Ledger & Reporting**: Instantly calculate monthly bills, deductions, balances, and generate professional PDF invoices (via OpenPDF) and Excel spreadsheets (via Apache POI).
5. **Optional Cloud Synchronization**: Keep local MySQL as the primary source of truth while supporting optional Firebase cloud backup when configured.

---

## ✨ Key Features

### 🛡️ Admin Portal
- **Executive Dashboard & KPIs**: Real-time dynamically calculated statistics on total platform users, active vendors, active customers, active products, and non-duplicated total revenue.
- **User Access Control**: Comprehensive user directory with real-time text search and role/status filtering (`ALL`, `ADMIN`, `VENDOR`, `CUSTOMER`, `ACTIVE`, `INACTIVE`).
- **Account Activation & Security**: One-click Activate / Deactivate account actions with safety confirmations, immediate database updates, self-deactivation protection, and last-admin preservation.
- **Master Product Catalog**: Read-only directory of standardized dairy products, pricing, units (`LITER`, `KG`, `GRAM`, `PACKET`), and current inventory stock levels.
- **Live System Health & Diagnostics**: Real-time ping diagnostics for MySQL database connectivity, JDBC query latency (ms), HikariCP pool status, JVM memory usage heap, and repository data verification.

### 🏢 Vendor Portal
- **Dashboard & Analytics**: Real-time metrics on total active customers, daily milk volume delivered, pending orders, and total revenue.
- **Customer Management**: Add, view, update, and manage customer subscriptions and delivery preferences.
- **Daily Shift Deliveries**: Mark morning and evening deliveries turn-by-turn with rate snapshotting and holiday auto-pauses.
- **Monthly Billing & Ledger**: Automated bill generation, payment recording (UPI/Cash/NetBanking), PDF invoice export, and Excel financial report export.
- **Stock & Inventory**: Track product stock levels, set reorder alerts, and restock dairy inventory.
- **Extra Product Orders**: Review and approve/reject extra milk and dairy product orders submitted by customers.
- **Holiday Request Approval**: Review and approve/reject customer delivery pause requests with cut-off validation.
- **Nagpur Route Planner**: Offline Dijkstra/Nearest-Neighbor route optimizer for shortest delivery paths between customer stops using Haversine formulas.

### 👤 Customer Portal
- **Dashboard Overview**: Modern KPI cards for daily subscription quota, current monthly bill, balance due with quick pay trigger, delivery status, and recent tables.
- **Monthly Bills & Instant Payment**: View full invoice breakdowns (net payable, paid amount, remaining balance, status badges), filter by status, and settle dues via an intuitive Pay Now interface.
- **Simulated Demo Payment Gateway**: Safe sandbox flow supporting UPI, Net Banking, Cash, and Store Credit with partial and full payment options and atomic ledger synchronization.
- **Delivery History & PDF Invoices**: Track complete shift-wise delivery records and download official PDF monthly invoices directly.
- **Order Extra Products**: Place orders for extra milk or dairy products (Paneer, Curd, Ghee, Butter, Cheese) with unit support.
- **Holiday Pause Application**: Request delivery pauses before the 10 PM cut-off time for upcoming vacations.

### 🤖 Offline Dairy Knowledge Assistant
- Completely offline Q&A search engine using TF-IDF token scoring and keyword matching.
- Pre-loaded with comprehensive FAQs from `dairy_knowledge_base.json` covering:
  - Milk storage & boiling techniques
  - Milk and Paneer shelf life guidelines
  - Curd setting and storage instructions
  - Dairy hygiene & milking equipment sanitation
  - Cow health monitoring & mastitis prevention
  - Cattle feeding ratios (green fodder, dry fodder, mineral mix)
  - General dairy nutrition & app usage FAQs

---

## 🛠️ Technology Stack

- **User Interface**: JavaFX 21.0.2 with FXML and custom CSS styling
- **Core Language**: Java 17 / 25
- **Database**: MySQL 8.0+ (3NF Schema with ACID transactions)
- **Connection Pool**: HikariCP 5.1.0
- **Security & Hashing**: BCrypt (`jbcrypt:0.4`)
- **PDF Generation**: OpenPDF (`openpdf:1.3.39`)
- **Excel Export**: Apache POI (`poi-ooxml:5.2.5`)
- **Logging**: SLF4J 2.0.12 with SimpleLogger
- **Testing**: JUnit 5 (JUnit Jupiter 5.10.2)
- **Build Tool**: Apache Maven 3.9+

---

## 🏗️ Architecture & Database Design

The system follows a strict layered Architecture:

```
[ JavaFX View (.fxml) ] 
       │
[ Controller Layer ] (Event Handling & Input Validation)
       │
[ Service Layer ] (Business Rules, Billing Logic, Route Planning)
       │
[ DAO Layer ] (PreparedStatements & Transaction Management)
       │
[ Database Layer ] (MySQL / HikariCP Pool)
```

### Database Schema Highlights:
- **`users`**: Authentication credentials & role assignment (`VENDOR`, `CUSTOMER`).
- **`vendors` & `customers`**: Master profile data including geographic coordinates (`latitude`, `longitude`) for route planning.
- **`products` & `inventory`**: Standardized products with `ProductUnit` enum (`LITER`, `KG`, `GRAM`, `PACKET`) and stock tracking.
- **`delivery_records`**: Daily shift deliveries with immutable `rate_per_liter` snapshots.
- **`monthly_bills` & `payments`**: Ledger summaries, partial/full payment tracking, and due balances.
- **`extra_orders` & `holiday_requests`**: Order requests and holiday pauses.
- **`faq_knowledge_base`**: Repository for offline assistant queries.

---

## ⚙️ Setup & Installation

### Prerequisites
1. **Java Development Kit (JDK 17+)** installed.
2. **MySQL Server 8.0+** running locally.
3. **Apache Maven 3.9+** (or bundled IDE Maven).

### 1. Database Setup
1. Open MySQL Command Line or Workbench.
2. Execute the provided database schema script located at:
   `docs/database_schema.sql`
   ```sql
   SOURCE docs/database_schema.sql;
   ```
3. Update database credentials in `src/main/resources/config/database.properties` or system environment variables if different from default:
   - URL: `jdbc:mysql://localhost:3306/agro_dairy`
   - User: `root`
   - Password: `your_mysql_password`

---

## 🚀 How to Build & Run

### Running Maven Tests
Execute full integration test suite (41 automated unit & integration tests across 11 test suites):
```powershell
& "C:\Users\Dhanshree\IntelliJ IDEA 2025.3.2\plugins\maven\lib\maven3\bin\mvn.cmd" test
```
*(Or `mvn test` if `mvn` is added to system PATH)*

### Launching the JavaFX Application
Launch application using JavaFX Maven plugin:
```powershell
& "C:\Users\Dhanshree\IntelliJ IDEA 2025.3.2\plugins\maven\lib\maven3\bin\mvn.cmd" javafx:run
```
*(Or `mvn javafx:run` if `mvn` is added to system PATH)*

---

## 🔑 Demo Login Credentials

The seed data provides pre-configured demo users for testing:

| Role | Username | Default Password | Details |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin_super` | `Password123` | System Super Administrator |
| **Vendor** | `vendor_ramesh` | `Password123` | Ramesh Dairy Owner (Nagpur HQ) |
| **Customer** | `cust_rahul` | `Password123` | Rahul Verma (Dharampeth, Nagpur) |
| **Customer** | `cust_sunita` | `Password123` | Sunita Deshmukh (Pratap Nagar, Nagpur) |
| **Customer** | `cust_mahesh` | `Password123` | Mahesh Patil (Sadar, Nagpur) |

> **Note**: Demo passwords can also be entered as `secret123`. Quick 1-click autofill demo buttons are also provided on the modern login screen.

---

## ☁️ Optional Firebase Cloud Configuration

The application operates **100% offline** with local MySQL as the primary source of truth. Firebase cloud sync is optional.

To enable optional Firebase synchronization:
1. Set system environment variable `FIREBASE_DATABASE_URL` (e.g. `https://your-app.firebaseio.com`).
2. Alternatively set `FIREBASE_CONFIG_PATH` pointing to your service account JSON file.
3. If unconfigured or offline, the application logs status gracefully without disrupting execution.

---

## 🔭 Future Scope

- **Mobile App Companion**: Flutter or Android companion app for delivery boys.
- **SMS/WhatsApp Notifications**: Integration with Twilio/WhatsApp Business API for delivery alerts.
- **GPS Live Tracking**: Real-time vehicle location tracking during route delivery legs.
