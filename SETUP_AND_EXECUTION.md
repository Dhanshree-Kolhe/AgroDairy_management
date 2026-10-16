# Setup & Execution Guide — Smart Dairy Management System

## System Requirements
- **JDK**: Java Development Kit 17 or higher (JDK 17, 21, and 25 compatible).
- **Database Engine**: MySQL Server 8.0+ running locally on port 3306.
- **Build Tool**: Apache Maven 3.9+ (or IntelliJ IDEA bundled Maven).

---

## 1. Database Initialization
1. Ensure your local MySQL service is running.
2. Log in via MySQL CLI or MySQL Workbench:
   ```bash
   mysql -u root -p
   ```
3. Execute the database schema script provided in the repository:
   ```sql
   SOURCE docs/database_schema.sql;
   ```
4. Verify database creation and table setup:
   ```sql
   USE agro_dairy;
   SHOW TABLES;
   ```
   *Expected: 12 tables (`customers`, `delivery_records`, `extra_orders`, `faq_knowledge_base`, `holiday_requests`, `inventory`, `monthly_bills`, `notifications`, `payments`, `products`, `users`, `vendors`).*

---

## 2. Environment Configuration
The application connects to MySQL via `DatabaseConfig` with HikariCP connection pooling.

Configuration can be specified through system environment variables:
| Environment Variable | Description | Default Fallback |
| :--- | :--- | :--- |
| `AGRO_DAIRY_DB_URL` | JDBC Connection URL | `jdbc:mysql://localhost:3306/agro_dairy?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `AGRO_DAIRY_DB_USER` | MySQL Username | `root` |
| `AGRO_DAIRY_DB_PASS` | MySQL Password | Default configured in `AppConstants.DEFAULT_DB_PASS` |
| `RAZORPAY_KEY_ID` | Optional live gateway API key | Unset (defaults safely to Simulated Demo flow) |
| `RAZORPAY_KEY_SECRET` | Optional live gateway API secret | Unset |

---

## 3. Building & Running the Application

### Maven Commands (System Maven)
- **Compile Project**:
  ```bash
  mvn compile
  ```
- **Execute Automated Test Suite (41 tests)**:
  ```bash
  mvn test
  ```
- **Launch JavaFX Desktop Application**:
  ```bash
  mvn javafx:run
  ```

### Maven Commands (IntelliJ Bundled Maven Environment)
If Maven is not directly added to system PATH:
- **Execute Tests**:
  ```powershell
  & "C:\Users\Dhanshree\IntelliJ IDEA 2025.3.2\plugins\maven\lib\maven3\bin\mvn.cmd" test
  ```
- **Launch JavaFX Desktop Application**:
  ```powershell
  & "C:\Users\Dhanshree\IntelliJ IDEA 2025.3.2\plugins\maven\lib\maven3\bin\mvn.cmd" javafx:run
  ```

---

## 4. Demo Login Accounts

The system provides 1-click demo autofill buttons directly on the Login screen (`LoginView.fxml`) for instant access:

| Role | Username | Purpose & Access Scope |
| :--- | :--- | :--- |
| **Administrator** | `admin_super` | System Executive Overview, User Management, Access Control, Diagnostics |
| **Vendor** | `vendor_ramesh` | Vendor Dashboard, Customer Management, Daily Shift Deliveries, Billing, Inventory |
| **Customer** | `cust_rahul` | Customer Dashboard, Delivery History, Extra Orders, Holiday Pause, Invoices, Payment |
| **Customer** | `cust_sunita` | Customer Portal (Pratap Nagar Zone - has partially paid bill for testing) |
| **Customer** | `cust_mahesh` | Customer Portal (Sadar Zone) |

---

## 5. Customer Payment Selection & Settlement Flow

When the customer clicks **Pay Now** from either their Dashboard or the **My Bills** table, the payment selection modal opens:

### 1. Choose Payment Type
- **Online Payment**: Instant digital checkout channel.
- **Offline Payment**: In-person cash or direct bank transfer channel.

### 2. Online Payment Options (Simulated Demo Flow)
- **UPI**: Google Pay (GPay), PhonePe, Paytm
- **Debit / Credit Card**: Visa, MasterCard, RuPay
- **Net Banking**: All major Indian commercial banks
- **Safety Safeguard**: Because no external payment gateway SDK (Razorpay/Stripe) or live API credentials are configured, the online payment flow is **explicitly labeled as Demo Mode**. Confirming an online payment displays an informative demo alert and **does NOT record a successful payment or debit customer bank accounts**, preserving database and accounting integrity.

### 3. Offline Payment Options
- **Cash**: Direct doorstep handover to the delivery vendor. Immediately records payment with status `SUCCESS`, decrements the bill balance due, and marks the bill status as `PARTIAL` or `PAID`.
- **Bank Transfer (NEFT / IMPS / RTGS)**: Customer enters their 12-digit UTR or bank reference number. Records the payment in `payments` with status `PENDING`. Leaves the bill balance uncredited until confirmed by the vendor or admin.

### 4. Financial Integrity Controls
- **Flexible Amounts**: Supports full payment, 50% partial payment, or any custom numeric figure.
- **Bounds Checking**: Rejects negative figures, zero amounts, non-numeric strings, and amounts exceeding the outstanding balance due.
- **Duplicate Prevention**: Rejects payment submissions on already fully settled bills, detects duplicate bank UTR references, and prevents multiple pending transfer submissions for the same statement.

---

## 6. Production Payment Gateway Integration Guidelines

To activate live online payments (e.g. Razorpay):
1. **Add Gateway SDK to `pom.xml`**:
   ```xml
   <dependency>
       <groupId>com.razorpay</groupId>
       <artifactId>razorpay-java</artifactId>
       <version>1.4.6</version>
   </dependency>
   ```
2. **Configure API Credentials**:
   Add `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` to environment variables or backend secrets vault.
3. **Order Creation & Dynamic Checkout**:
   - Call `RazorpayClient.orders.create()` before presenting payment controls to generate an official order ID.
   - Embed a JavaFX `WebView` loading Razorpay Standard Checkout or display dynamic UPI QR code.
4. **Signature Verification & Webhook**:
   - Verify `razorpay_signature` using HMAC-SHA256 with `RAZORPAY_KEY_SECRET`.
   - Call `PaymentService.recordPayment(...)` with status `SUCCESS` only upon validated cryptographic confirmation.

---

## 6. AI Dairy Knowledge Assistant & Vision Image Analysis Setup

The Dairy Knowledge Assistant (`/fxml/common/KnowledgeAssistantView.fxml`) has been upgraded with:
- **Interactive Chat Interface**: Ask questions on cattle management, milk handling, boiling, and storage.
- **Vision Image Analysis**: Upload photos (JPG, JPEG, PNG up to 10 MB) of dairy cattle or milk samples for automated visual inspection.
- **Category Selection**:
  1. *Cow/buffalo health concerns*: Evaluates posture, coat, lesions, udder signs, and recommends veterinary follow-up steps.
  2. *Milk-related problems*: Evaluates physical curdling, color, consistency, notes limitations of visual inspection, and provides safe testing instructions.
- **Medical & Food Safety Safeguards**: Prominent disclaimers warning that animal analysis is non-diagnostic and milk purity requires laboratory verification.
- **Offline Knowledge Base Fallback**: When AI is unconfigured or offline, inquiries automatically fall back to the offline JSON/MySQL knowledge base.

### Configuring Live AI Vision API Keys

The application supports **Google Gemini Vision** (recommended, `gemini-1.5-flash`) and **OpenAI Vision** (`gpt-4o-mini`). It automatically reads the key from environment variables or JVM system properties without hardcoding credentials:

#### Option A: Google Gemini Vision (Recommended)
1. Get a free API key from [Google AI Studio](https://aistudio.google.com).
2. Set the environment variable before launching the application:
   - **PowerShell**:
     ```powershell
     $env:GEMINI_API_KEY="your_actual_gemini_api_key"
     mvn javafx:run
     ```
   - **Command Prompt (CMD)**:
     ```cmd
     set GEMINI_API_KEY=your_actual_gemini_api_key
     mvn javafx:run
     ```
   - **Maven JVM System Property**:
     ```powershell
     mvn javafx:run -DGEMINI_API_KEY="your_actual_gemini_api_key"
     ```

#### Option B: OpenAI Vision
1. Get an API key from [OpenAI Platform](https://platform.openai.com).
2. Set the environment variable:
   - **PowerShell**:
     ```powershell
     $env:OPENAI_API_KEY="your_actual_openai_api_key"
     mvn javafx:run
     ```

### Testing the Upgraded Screen in the Running Application
1. Launch the app: `mvn javafx:run`.
2. Log in as Customer (`cust_rahul` / `secret123`) or Vendor (`vendor_ramesh` / `secret123`).
3. Click **Dairy Knowledge Assistant** on the sidebar.
4. Verify the top status badge:
   - Shows `🟢 Google Gemini Vision (gemini-1.5-flash)` when `GEMINI_API_KEY` is set.
   - Shows `⚠️ Unconfigured (Offline Fallback Mode)` when no key is set.
5. In the chat input, type a question (e.g., *"How to set thick curd in summer?"*) and press **Send**.
6. On the right panel, select **Cow/buffalo health concerns** or **Milk-related problems**.
7. Click **Choose Image...**, pick a JPG/PNG file, preview the thumbnail, optionally type notes, and click **Analyze Image with AI**.
8. View the structured AI observations and safety disclaimers directly inside the chat conversation!
9. Switch to the **Offline FAQ Directory** tab to browse or search local offline FAQs.


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
