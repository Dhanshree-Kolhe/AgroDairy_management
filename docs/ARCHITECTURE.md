# System Architecture & Layered Design — Smart Dairy Management System

## Architectural Pattern
The application follows a clean 4-Layer Architecture adhering to Separation of Concerns (SoC) and Dependency Inversion principles:

```
+-------------------------------------------------------------+
|                      USER INTERFACE                         |
|      JavaFX Views (.fxml) + Custom CSS Theme Styling        |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                     CONTROLLER LAYER                        |
|   Handles UI events, user input validation, dialogs,        |
|   and TableView model binding (e.g. BillingController)      |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                      SERVICE LAYER                          |
|   Business Logic, Ledger Calculation, Price Snapshotting,   |
|   Dijkstra Route Planning, Knowledge Assistant TF-IDF,      |
|   PDF/Excel Exporters, Cloud Backup (e.g. BillingService)   |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                      DATA ACCESS LAYER                      |
|   DAO Interfaces & JDBC Implementations using HikariCP      |
|   (e.g. MonthlyBillDAO, DeliveryRecordDAO, CustomerDAO)     |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                       DATABASE LAYER                        |
|            MySQL Database (agro_dairy Schema)               |
+-------------------------------------------------------------+
```

---

## Package Responsibilities

- **`com.agrodairy.management.config`**: Core constants (`AppConstants`) and HikariCP database initialization (`DatabaseConfig`).
- **`com.agrodairy.management.controller`**: JavaFX Controller classes handling UI interaction and layout switching.
- **`com.agrodairy.management.dao` & `dao.impl`**: Data Access Objects managing SQL queries, `PreparedStatement` parameters, and `ResultSet` mapping.
- **`com.agrodairy.management.model` & `model.enums`**: Pure POJO domain entities (`Customer`, `Product`, `MonthlyBill`, `DeliveryRecord`, etc.) and enums (`ProductUnit`, `DeliveryShift`, `PaymentMode`, `PaymentStatus`).
- **`com.agrodairy.management.service` & `service.impl`**: Core business services implementing ledger formulas, stock reduction on approval, offline Dijkstra route ordering, and knowledge base search.
- **`com.agrodairy.management.util`**: Helper utilities (`PdfInvoiceGenerator`, `ExcelReportExporter`, `NavigationManager`, `SessionManager`, `AlertUtils`, `PasswordUtil`).
- **`com.agrodairy.management.exception`**: Custom exception hierarchy (`ValidationException`, `ResourceNotFoundException`, `DatabaseException`, `DuplicateDeliveryException`).
