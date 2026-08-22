# Setup & Execution Guide — Smart Dairy Management System

## System Requirements
- **JDK**: Java Development Kit 17 or higher (JDK 17/21/25 tested).
- **Database Engine**: MySQL Server 8.0+ running locally on port 3306.
- **Build Tool**: Apache Maven 3.9+ (or IntelliJ IDEA bundled Maven).

---

## 1. Database Initialization
1. Start your local MySQL service.
2. Log in via MySQL CLI or MySQL Workbench:
   ```bash
   mysql -u root -p
   ```
3. Execute the initial SQL schema script provided in the project:
   ```sql
   SOURCE docs/database_schema.sql;
   ```
4. Verify database creation:
   ```sql
   USE agro_dairy;
   SHOW TABLES;
   ```

---

## 2. Environment Configuration
Verify configuration settings in `src/main/resources/config/database.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/agro_dairy?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=root
```
*(You can override these properties using system environment variables `DB_URL`, `DB_USER`, and `DB_PASSWORD`)*.

---

## 3. Building & Running the Application

### Maven Commands (System Maven in PATH)
- **Compile Project**:
  ```bash
  mvn compile
  ```
- **Execute Unit & Integration Tests**:
  ```bash
  mvn test
  ```
- **Launch JavaFX Desktop Application**:
  ```bash
  mvn javafx:run
  ```

### Maven Commands (IntelliJ Bundled Maven Environment)
If Maven is not directly added to system PATH:
- **Execute Unit & Integration Tests**:
  ```powershell
  & "C:\Users\Dhanshree\IntelliJ IDEA 2025.3.2\plugins\maven\lib\maven3\bin\mvn.cmd" test
  ```
- **Launch JavaFX Desktop Application**:
  ```powershell
  & "C:\Users\Dhanshree\IntelliJ IDEA 2025.3.2\plugins\maven\lib\maven3\bin\mvn.cmd" javafx:run
  ```

---

## 4. Demo Login Credentials

| User Type | Username | Password | Purpose |
| :--- | :--- | :--- | :--- |
| **Vendor** | `vendor_ramesh` | `Password123` | Access Vendor Dashboard, Customer Management, Billing, Route Planner, Stock |
| **Customer** | `cust_rahul` | `Password123` | Access Customer Dashboard, Order Extra Products, Request Holiday Pause, PDF Bills |
| **Customer** | `cust_sunita` | `Password123` | Access Customer Dashboard |

---

## 5. Native Packaging Instructions (jpackage)

To package the JavaFX application into a standalone native Windows executable (`.exe` or `.msi`) using JDK `jpackage`:

1. Build project JAR dependencies:
   ```bash
   mvn clean package
   ```
2. Run `jpackage` command:
   ```powershell
   jpackage --name "SmartDairyManager" `
            --input target/ `
            --main-jar management-1.0.0.jar `
            --main-class com.agrodairy.management.App `
            --type msi `
            --win-shortcut `
            --win-menu
   ```
*(Requires WiX Toolset for `.msi` generation on Windows)*
