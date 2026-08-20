# Smart Dairy Management System
## Software Requirement Specification (SRS) & System Architecture Document
**Degree Project Specification for B.Tech in Computer Science & Engineering**

---

### Document Overview
- **Project Name**: Smart Dairy Management System
- **Document Version**: 1.2.0 (Phase 2 Completed Baseline)
- **Target Platform**: Desktop (Windows / macOS / Linux)
- **Primary Tech Stack**: Java 17, JavaFX, FXML, MySQL 8.0, HikariCP, Firebase, Google Maps API
- **Architecture**: Model-View-Controller (MVC) with Service & DAO Pattern

---

## MANDATORY DEVELOPMENT RULES FOR ALL FUTURE PHASES

The following development rules are mandatory guidelines for all subsequent development phases (Phase 3 through Phase 7):

1. **Input Validation Integrity**:
   - Never skip input validations.
   - Every Service method MUST validate all input parameters before invoking the DAO layer.
2. **Strict Layer Separation**:
   - Keep ALL business logic inside the Service layer ONLY (`com.agrodairy.management.service.impl`).
   - DAO classes MUST ONLY communicate with the database (`com.agrodairy.management.dao.impl`).
   - Controllers MUST ONLY handle UI events and delegate work to the Service layer (`com.agrodairy.management.controller`).
   - NEVER place business logic inside Controllers.
3. **Database & Query Safety**:
   - NEVER write SQL queries inside Controllers or Service classes. All SQL statements must reside within DAO classes.
   - ALWAYS use `PreparedStatement` with parameterized placeholders (`?`) for database operations to prevent SQL injection.
4. **Code Quality & Architecture Enforcement**:
   - Follow clean code principles, DRY (Don't Repeat Yourself), and robust exception handling using custom runtime exceptions.
   - Reuse existing classes, DTOs, and utility helpers whenever possible.
   - NEVER redesign the package structure, database schema, or core MVC architecture in future phases.

---

## 1. Complete Software Requirement Specification (SRS)

### 1.1 Document Purpose
This Software Requirement Specification (SRS) establishes the complete functional, technical, structural, and behavioral baseline for the **Smart Dairy Management System**.

### 1.2 Scope of the System
The system is designed as a standalone, enterprise-grade desktop management application targeting local milk vendors ("Doodhvalas") and small-to-medium dairy cooperatives in India. The application digitizes daily milk delivery, customer subscription tracking, extra dairy product ordering, automated monthly billing, inventory tracking, delivery route planning, offline cloud backup, and an offline dairy knowledge assistant.

### 1.3 System Constraints
- **Primary Execution Environment**: Desktop JVM (Java 17 LTS or higher).
- **Database Requirements**: Local/Networked MySQL 8.0+ Instance with InnoDB storage engine.
- **Network Requirements**: Functional offline capabilities; internet connection is only utilized for optional Firebase Cloud Backup and Google Maps Route Fetching.
- **UI Framework**: JavaFX 21+ using FXML for declarative views and CSS for custom styling.

---

## 2. Problem Statement

In the traditional Indian doorstep milk distribution ecosystem, independent milk vendors handle delivery to dozens or hundreds of residential households twice daily (Morning and Evening shifts). Currently, over 85% of these vendors rely on manual notebooks ("Khatta-bahi"), paper slips, or mental memory to record daily deliveries, milk quantities, customer pauses, and advance payments.

This manual operational model suffers from critical friction points:
1. **Manual Billing & Calculation Errors**: Monthly bill generation requires manually counting tally marks for 30 days per customer. This leads to frequent calculation mistakes, lost revenue, and strained vendor-customer trust.
2. **Delivery & Pause Disputes**: Customers frequently dispute whether milk was delivered on specific dates or whether pause/holiday requests were communicated correctly via phone calls or SMS.
3. **Product & Inventory Leakage**: Vendors lack real-time visibility into daily stock levels for auxiliary dairy products (Paneer, Curd, Ghee, Butter, Cheese), resulting in stockouts or product spoilage.
4. **Inefficient Delivery Routes**: Vendors navigate delivery areas based on intuition rather than optimized geographic routing, consuming excess fuel and time.
5. **Lack of Data Safety**: Notebooks are vulnerable to physical loss, water damage, or tear, wiping out entire financial ledgers without any backup.

---

## 3. Existing System Analysis

### 3.1 Legacy Workflow
1. Vendor visits houses sequentially based on memory.
2. Vendor physically marks a paper notebook at the end of the shift.
3. At the end of the month, vendor spends 2-3 full days manually calculating bills using paper calculators.
4. Bills are distributed via paper slips; payment collections are tracked manually with cash registers.

### 3.2 Key Drawbacks of Existing Systems
| Metric | Legacy Manual System | Impact on Vendor |
| :--- | :--- | :--- |
| **Billing Accuracy** | ~85-90% (High Human Error) | Revenue loss of 5-15% monthly |
| **Time Spent on Billing** | 16-24 Hours / month | Massive operational overhead |
| **Data Redundancy & Safety** | Zero Backup (Paper-based) | High risk of total ledger loss |
| **Route Planning** | Intuitive / Unoptimized | 20-30% wasted fuel & delivery time |
| **Customer Transparency** | Oral / Paper Slips | Constant disputes & missed deliveries |

---

## 4. Proposed System

The **Smart Dairy Management System** provides a centralized, dual-portal desktop application that bridges vendors and customers through an automated, software-driven workflow.

### Key Innovations of the Proposed System:
- **Dual-Role Isolated Workspaces**: Dedicated Vendor (Admin) and Customer interfaces with role-based access control.
- **Automated Billing Engine with Rate Snapshot**: Every marked delivery snapshotting the per-liter price at delivery time, instantly updating the customer's monthly balance ledger without any manual intervention.
- **Practical Route Planner**: Primary route generation using Google Maps Directions API, with Dijkstra's shortest path algorithm as a reliable offline fallback.
- **Hybrid Data Persistence**: Operates primarily on high-performance local MySQL with asynchronous background backup to Firebase Cloud.
- **Offline Dairy Knowledge Assistant**: Lightweight, local JSON-based knowledge assistant operating via simple keyword matching—no machine learning, no LLMs, no internet required.

---

## 5. Research Gap

Existing commercial applications (e.g., enterprise SaaS milk delivery apps like Country Delight or MilkBasket) are designed for large corporate aggregators. They present several gaps for individual local vendors:

1. **High Infrastructure & Subscription Costs**: Existing solutions require expensive monthly per-user SaaS licenses and constant cloud server hosting.
2. **Cloud Dependency**: SaaS apps fail completely when delivery boys operate in low-connectivity or offline rural/suburban areas.
3. **Over-complexity**: Enterprise tools include complex supply chain features irrelevant to local vendors, making user adoption difficult.
4. **Lack of Integrated Local Knowledge**: No existing desktop application includes localized offline guidance for both vendor herd management/hygiene and customer storage/usage tips.

**Our Project's Research Contribution**: A lightweight, offline-first hybrid architecture combining desktop SQL reliability with cloud synchronization, practical Dijkstra/Google Maps route calculation, and an offline JSON keyword knowledge assistant tailored specifically for independent urban and semi-urban Indian vendors.

---

## 6. Project Objectives

1. **Automate Daily Delivery & Attendance**: Provide a single-click interface to mark morning/evening deliveries, immediately reflecting status in customer accounts.
2. **Zero-Error Automated Billing with Rate Locking**: Eliminate manual billing by dynamically compiling milk deliveries (using historical price snapshots), rate adjustments, extra product orders, and holiday deductions into monthly PDF/Excel invoices.
3. **Streamline Customer Holiday & Extra Requests**: Enable customers to request delivery pauses or extra milk/products with vendor approval workflows.
4. **Optimize Delivery Logistics**: Reduce delivery travel distance and time using Google Maps Directions API integration with Dijkstra algorithm fallback.
5. **Ensure Data Resilience**: Guarantee zero data loss by combining local MySQL ACID compliance with asynchronous Firebase cloud backup.
6. **Provide Offline Guidance**: Empower users with a lightweight Offline Dairy Knowledge Assistant operating via simple local keyword search.

---

## 7. Functional Requirements

### 7.1 Authentication & Security Module
- **FR-1.1**: System shall support distinct login credentials for Vendor (Admin) and Customer roles.
- **FR-1.2**: Passwords must be encrypted using BCrypt salted hashing (`jBCrypt`) prior to storage.
- **FR-1.3**: System shall maintain session context and enforce Role-Based Access Control (RBAC) across all FXML controllers.

### 7.2 Customer Management Module (Vendor)
- **FR-2.1**: Vendor shall be able to Create, Read, Update, and Soft-Delete customer profiles.
- **FR-2.2**: Customer profiles must store: Name, Mobile Number, Address, Area/Zone (Nagpur delivery areas), Latitude/Longitude coordinates, Daily Morning Quantity (Liters), Daily Evening Quantity (Liters), Preferred Milk Type (Cow, Buffalo, Mixed), Default Per-Liter Rate, Subscription Type, and Account Status (Active, Paused, Inactive).
- **FR-2.3**: Vendor shall be able to filter and search customers by Name, Mobile, Area, or Status.

### 7.3 Daily Delivery Tracking & Rate Snapshot Module
- **FR-3.1**: Vendor shall view a real-time list of expected deliveries for the selected date and shift (Morning / Evening).
- **FR-3.2**: Vendor can mark individual or bulk status (`Delivered`, `Not Delivered`, `Paused`).
- **FR-3.3 Rate Snapshot Guarantee**: When a delivery is marked `Delivered`, the system **MUST explicitly store the current effective price per liter (`rate_per_liter`) directly inside the delivery record**. Future price updates to customer rates or base product rates shall **NEVER** retroactively alter previously saved delivery billing records.

### 7.4 Vendor & Customer Dashboard Specifications
- **FR-4.1 Vendor Dashboard Metrics**: Today's Customers, Delivered, Pending Deliveries, Today's Revenue, Pending Payments, Extra Orders, Low Stock Alerts.
- **FR-4.2 Customer Dashboard Metrics**: Today's Delivery Status, Monthly Bill, Payment Status, Extra Orders, Notifications.

---

## 8. Package Structure

```
com.agrodairy.management
├── App.java                                // Application Entry Point (JavaFX Launch)
├── config                                  // System Configurations
│   ├── DatabaseConfig.java                 // HikariCP Connection Pool Config
│   ├── FirebaseConfig.java                 // Firebase SDK Setup
│   └── AppConstants.java                   // Global Constants & System Paths
├── model                                   // Data Models & Domain Entities
│   ├── UserLogin.java                      // User Credentials Entity
│   ├── User.java                           // User Entity
│   ├── Vendor.java                         // Vendor Entity
│   ├── Customer.java                       // Customer Entity (Nagpur Locations)
│   ├── DeliveryRecord.java                 // Daily Delivery Record (with rate_per_liter snapshot)
│   ├── MonthlyBill.java                    // Monthly Bill Summary
│   ├── Product.java                        // Product Entity (ProductUnit Enum)
│   ├── Inventory.java                      // Product Inventory Entity
│   ├── Payment.java                        // Payment Transaction Entity
│   ├── ExtraOrder.java                     // Extra Order Entity
│   ├── HolidayRequest.java                 // Holiday Request Entity
│   ├── Notification.java                   // System Notification Entity
│   ├── FAQKnowledgeBase.java               // FAQ Entity
│   └── enums                               // System Enumerations
│       ├── Role.java                       // VENDOR, CUSTOMER
│       ├── MilkType.java                   // COW, BUFFALO, MIXED
│       ├── DeliveryShift.java              // MORNING, EVENING, BOTH
│       ├── DeliveryStatus.java             // DELIVERED, NOT_DELIVERED, PAUSED
│       ├── SubscriptionStatus.java         // ACTIVE, INACTIVE, PAUSED
│       ├── RequestStatus.java              // PENDING, APPROVED, DELIVERED, REJECTED
│       ├── PaymentStatus.java              // UNPAID, PARTIAL, PAID
│       ├── PaymentMode.java                // CASH, UPI, NET_BANKING, CREDIT
│       ├── ProductCategory.java            // MILK, DAIRY_PRODUCT, BY_PRODUCT
│       ├── ProductUnit.java               // LITER, KG, GRAM, PACKET
│       └── NotificationCategory.java       // CUSTOMER, VENDOR, DELIVERY, PAYMENTS, REQUESTS
├── dao                                     // Data Access Layer (Interfaces)
│   ├── UserLoginDAO.java
│   ├── UserDAO.java
│   ├── VendorDAO.java
│   ├── CustomerDAO.java
│   ├── ProductDAO.java
│   ├── InventoryDAO.java
│   ├── DeliveryRecordDAO.java & DeliveryDAO.java
│   ├── MonthlyBillDAO.java & BillingDAO.java
│   ├── PaymentDAO.java
│   ├── ExtraOrderDAO.java
│   ├── HolidayRequestDAO.java & HolidayDAO.java
│   ├── NotificationDAO.java
│   └── FAQKnowledgeBaseDAO.java & FAQDAO.java
├── service                                 // Business Logic Layer (Interfaces)
└── controller                              // JavaFX FXML Controllers
```

---
**End of Software Requirement Specification Document**
