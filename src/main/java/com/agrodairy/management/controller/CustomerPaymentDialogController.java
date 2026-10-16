package com.agrodairy.management.controller;

import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.MonthlyBill;
import com.agrodairy.management.model.Payment;
import com.agrodairy.management.model.enums.PaymentMode;
import com.agrodairy.management.model.enums.PaymentStatus;
import com.agrodairy.management.service.PaymentService;
import com.agrodairy.management.service.impl.PaymentServiceImpl;
import com.agrodairy.management.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for Customer Payment Selection, Confirmation Review, and Settlement.
 * Supports:
 * A. Online Payment (Demo Sandbox / Unconfigured Gateway):
 *    - UPI (Google Pay, PhonePe, Paytm, BHIM)
 *    - Debit / Credit Card
 *    - Net Banking
 * B. Pay on Delivery / Offline Payment:
 *    - Cash on Delivery (COD) / Direct Vendor Payment (Pending Doorstep Collection)
 *    - Bank Transfer with UTR Reference (Pending Verification)
 */
public class CustomerPaymentDialogController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerPaymentDialogController.class);

    // Bill Overview Labels
    @FXML private Label gatewayStatusBadge;
    @FXML private Label customerNameLabel;
    @FXML private Label billNumberLabel;
    @FXML private Label periodLabel;
    @FXML private Label totalAmountLabel;
    @FXML private Label paidAmountLabel;
    @FXML private Label balanceDueLabel;
    @FXML private Label errorLabel;

    // STEP 1: Form Container & Amount
    @FXML private VBox formContainer;
    @FXML private TextField amountField;

    // 5 Selectable Method Cards & Radios
    @FXML private ToggleGroup paymentMethodGroup;
    @FXML private VBox upiCardBox;
    @FXML private RadioButton upiRadio;
    @FXML private VBox cardCardBox;
    @FXML private RadioButton cardRadio;
    @FXML private VBox netBankCardBox;
    @FXML private RadioButton netBankRadio;
    @FXML private VBox cashCardBox;
    @FXML private RadioButton cashRadio;
    @FXML private VBox bankTransferCardBox;
    @FXML private RadioButton bankTransferRadio;

    // 5 Detail Panels
    @FXML private VBox upiPanel;
    @FXML private TextField upiIdField;

    @FXML private VBox cardPanel;
    @FXML private TextField cardNumberField;
    @FXML private TextField cardNameField;
    @FXML private TextField cardExpiryField;
    @FXML private TextField cardCvvField;

    @FXML private VBox netBankPanel;
    @FXML private ComboBox<String> bankSelectorCombo;

    @FXML private VBox cashPanel;

    @FXML private VBox bankTransferPanel;
    @FXML private TextField bankUtrField;

    // Summary & Feedback (Step 1)
    @FXML private Label selectedMethodDisplayLabel;
    @FXML private Label outcomeExplanationLabel;
    @FXML private Button proceedToConfirmBtn;

    // STEP 2: Confirmation & Review Screen
    @FXML private VBox confirmationContainer;
    @FXML private Label confirmStatusBadge;
    @FXML private Label confirmCustomerLabel;
    @FXML private Label confirmBillNumberLabel;
    @FXML private Label confirmMethodLabel;
    @FXML private Label confirmNatureLabel;
    @FXML private Label confirmAmountLabel;
    @FXML private Button confirmPaymentBtn;

    private MonthlyBill bill;
    private Customer customer;
    private Stage dialogStage;
    private Runnable onSuccessCallback;

    private final PaymentService paymentService;

    // Selected payment method identifier: 0=UPI, 1=Card, 2=NetBanking, 3=Cash/COD, 4=BankTransfer
    private int selectedMethodIndex = 0;
    private BigDecimal validatedAmount = BigDecimal.ZERO;

    public CustomerPaymentDialogController() {
        this.paymentService = new PaymentServiceImpl();
    }

    public CustomerPaymentDialogController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @FXML
    public void initialize() {
        hideError();
        setupBankSelector();
        setupMethodGroupListener();
        setupAmountFieldListener();

        // Default selection: UPI (Online Demo)
        if (upiRadio != null) {
            upiRadio.setSelected(true);
            applyMethodSelection(0);
        }
    }

    private void setupBankSelector() {
        if (bankSelectorCombo != null) {
            List<String> banks = Arrays.asList(
                    "HDFC Bank",
                    "State Bank of India (SBI)",
                    "ICICI Bank",
                    "Axis Bank",
                    "Kotak Mahindra Bank",
                    "Punjab National Bank (PNB)",
                    "Bank of Baroda"
            );
            bankSelectorCombo.setItems(FXCollections.observableArrayList(banks));
            bankSelectorCombo.setValue("HDFC Bank");
        }
    }

    private void setupMethodGroupListener() {
        if (paymentMethodGroup != null) {
            paymentMethodGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                hideError();
                if (newVal == upiRadio) applyMethodSelection(0);
                else if (newVal == cardRadio) applyMethodSelection(1);
                else if (newVal == netBankRadio) applyMethodSelection(2);
                else if (newVal == cashRadio) applyMethodSelection(3);
                else if (newVal == bankTransferRadio) applyMethodSelection(4);
                updateSummaryDisplay();
            });
        }
    }

    private void setupAmountFieldListener() {
        if (amountField != null) {
            amountField.textProperty().addListener((obs, oldVal, newVal) -> {
                hideError();
            });
        }
    }

    @FXML
    public void handleSelectUpi() {
        if (upiRadio != null) upiRadio.setSelected(true);
    }

    @FXML
    public void handleSelectCard() {
        if (cardRadio != null) cardRadio.setSelected(true);
    }

    @FXML
    public void handleSelectNetBank() {
        if (netBankRadio != null) netBankRadio.setSelected(true);
    }

    @FXML
    public void handleSelectCash() {
        if (cashRadio != null) cashRadio.setSelected(true);
    }

    @FXML
    public void handleSelectBankTransfer() {
        if (bankTransferRadio != null) bankTransferRadio.setSelected(true);
    }

    private void applyMethodSelection(int index) {
        this.selectedMethodIndex = index;

        // Toggle panel visibility
        setPanelVisible(upiPanel, index == 0);
        setPanelVisible(cardPanel, index == 1);
        setPanelVisible(netBankPanel, index == 2);
        setPanelVisible(cashPanel, index == 3);
        setPanelVisible(bankTransferPanel, index == 4);

        // Highlight selected method card
        resetCardStyle(upiCardBox, index == 0);
        resetCardStyle(cardCardBox, index == 1);
        resetCardStyle(netBankCardBox, index == 2);
        resetCardStyle(cashCardBox, index == 3);
        resetCardStyle(bankTransferCardBox, index == 4);
    }

    private void setPanelVisible(VBox panel, boolean visible) {
        if (panel != null) {
            panel.setVisible(visible);
            panel.setManaged(visible);
        }
    }

    private void resetCardStyle(VBox cardBox, boolean isSelected) {
        if (cardBox == null) return;
        cardBox.getStyleClass().remove("payment-tab-card-active");
        if (isSelected) {
            cardBox.getStyleClass().add("payment-tab-card-active");
        }
    }

    private void updateSummaryDisplay() {
        String methodName;
        String explanation;

        switch (selectedMethodIndex) {
            case 0:
                String upiId = (upiIdField != null && !upiIdField.getText().trim().isEmpty()) ? upiIdField.getText().trim() : "cust.rahul@okhdfcbank";
                methodName = "📱 UPI (Demo Simulation) — " + upiId;
                explanation = "Simulates instant UPI transfer. No real money deducted. Confirms demo clearance.";
                break;
            case 1:
                methodName = "💳 Debit/Credit Card (Demo Simulation) — 4242 •••• •••• 4242";
                explanation = "Safe sandbox card simulation. Updates invoice balance directly in database.";
                break;
            case 2:
                String bank = bankSelectorCombo != null && bankSelectorCombo.getValue() != null ? bankSelectorCombo.getValue() : "HDFC Bank";
                methodName = "🏦 Net Banking (Demo Simulation) — " + bank;
                explanation = "Simulated net banking checkout. Updates invoice balance directly in database.";
                break;
            case 3:
                methodName = "💵 Cash on Delivery (Doorstep Handover to Delivery Agent)";
                explanation = "Records doorstep cash collection request. Status will remain PENDING until agent confirms collection.";
                break;
            case 4:
                String utr = (bankUtrField != null && !bankUtrField.getText().trim().isEmpty()) ? bankUtrField.getText().trim() : "Pending UTR";
                methodName = "🏛️ Bank Transfer (NEFT/RTGS) — UTR: " + utr;
                explanation = "Direct transfer to dairy account. Status will remain PENDING VERIFICATION until approved by vendor/admin.";
                break;
            default:
                methodName = "Payment Method";
                explanation = "";
        }

        if (selectedMethodDisplayLabel != null) selectedMethodDisplayLabel.setText(methodName);
        if (outcomeExplanationLabel != null) outcomeExplanationLabel.setText(explanation);
    }

    // UPI App Quick Chips
    @FXML
    public void handleSelectGpayApp() {
        if (upiIdField != null) upiIdField.setText("cust.dairy@okhdfcbank");
        updateSummaryDisplay();
    }

    @FXML
    public void handleSelectPhonePeApp() {
        if (upiIdField != null) upiIdField.setText("cust.dairy@ybl");
        updateSummaryDisplay();
    }

    @FXML
    public void handleSelectPaytmApp() {
        if (upiIdField != null) upiIdField.setText("cust.dairy@paytm");
        updateSummaryDisplay();
    }

    @FXML
    public void handleSelectBhimApp() {
        if (upiIdField != null) upiIdField.setText("cust.dairy@upi");
        updateSummaryDisplay();
    }

    // Bank Chips
    @FXML
    public void handleSelectHdfc() {
        if (bankSelectorCombo != null) bankSelectorCombo.setValue("HDFC Bank");
        updateSummaryDisplay();
    }

    @FXML
    public void handleSelectSbi() {
        if (bankSelectorCombo != null) bankSelectorCombo.setValue("State Bank of India (SBI)");
        updateSummaryDisplay();
    }

    @FXML
    public void handleSelectIcici() {
        if (bankSelectorCombo != null) bankSelectorCombo.setValue("ICICI Bank");
        updateSummaryDisplay();
    }

    @FXML
    public void handleSelectAxis() {
        if (bankSelectorCombo != null) bankSelectorCombo.setValue("Axis Bank");
        updateSummaryDisplay();
    }

    public void setBillData(MonthlyBill bill, Customer customer, Stage dialogStage, Runnable onSuccessCallback) {
        this.bill = bill;
        this.customer = customer;
        this.dialogStage = dialogStage;
        this.onSuccessCallback = onSuccessCallback;

        if (bill != null) {
            if (billNumberLabel != null) billNumberLabel.setText(bill.getBillNumber());
            if (periodLabel != null) {
                periodLabel.setText(String.format("%02d/%d", bill.getBillingMonth(), bill.getBillingYear()));
            }
            if (totalAmountLabel != null) totalAmountLabel.setText("₹" + (bill.getNetPayable() != null ? bill.getNetPayable().toString() : "0.00"));
            if (paidAmountLabel != null) paidAmountLabel.setText("₹" + (bill.getPaidAmount() != null ? bill.getPaidAmount().toString() : "0.00"));
            if (balanceDueLabel != null) balanceDueLabel.setText("₹" + (bill.getBalanceDue() != null ? bill.getBalanceDue().toString() : "0.00"));

            BigDecimal due = bill.getBalanceDue() != null ? bill.getBalanceDue() : BigDecimal.ZERO;
            if (amountField != null) {
                amountField.setText(due.setScale(2, RoundingMode.HALF_UP).toString());
            }

            if (customer != null) {
                if (customerNameLabel != null) {
                    customerNameLabel.setText(customer.getFullName() != null ? customer.getFullName() : "Customer");
                }
                if (cardNameField != null) {
                    cardNameField.setText(customer.getFullName() != null ? customer.getFullName() : "Customer");
                }
            }

            if (due.compareTo(BigDecimal.ZERO) <= 0 || bill.getStatus() == PaymentStatus.PAID) {
                showError("This bill is already fully settled! No balance is due.");
                if (proceedToConfirmBtn != null) proceedToConfirmBtn.setDisable(true);
            }
        }
        updateSummaryDisplay();
    }

    @FXML
    public void handleFillFullAmount() {
        hideError();
        if (bill != null && amountField != null && bill.getBalanceDue() != null) {
            amountField.setText(bill.getBalanceDue().setScale(2, RoundingMode.HALF_UP).toString());
        }
    }

    @FXML
    public void handleFillHalfAmount() {
        hideError();
        if (bill != null && amountField != null && bill.getBalanceDue() != null) {
            BigDecimal half = bill.getBalanceDue().divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            amountField.setText(half.toString());
        }
    }

    /**
     * STEP 1 -> STEP 2: Validate inputs and transition to Confirmation Screen.
     */
    @FXML
    public void handleProceedToConfirmation() {
        hideError();

        if (bill == null) {
            showError("No bill selected for payment.");
            return;
        }

        if (bill.getBalanceDue() == null || bill.getBalanceDue().compareTo(BigDecimal.ZERO) <= 0
                || bill.getStatus() == PaymentStatus.PAID) {
            showError("Bill #" + bill.getBillNumber() + " is already fully settled. Duplicate payment rejected.");
            return;
        }

        // Validate Amount Input
        String amountText = amountField != null ? amountField.getText().trim() : "";
        if (amountText.isEmpty()) {
            showError("Please enter a payment amount.");
            return;
        }

        try {
            validatedAmount = new BigDecimal(amountText).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            showError("Invalid amount format. Please enter a numerical amount (e.g. 1500.00).");
            return;
        }

        if (validatedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            showError("Payment amount must be greater than zero.");
            return;
        }

        BigDecimal maxDue = bill.getBalanceDue();
        if (validatedAmount.compareTo(maxDue) > 0) {
            showError("Payment amount (₹" + validatedAmount + ") cannot exceed the remaining balance due (₹" + maxDue + ").");
            return;
        }

        // Validate Bank Transfer UTR if Bank Transfer selected
        if (selectedMethodIndex == 4) {
            String utr = bankUtrField != null ? bankUtrField.getText().trim() : "";
            if (utr.isEmpty()) {
                showError("Please enter your genuine Bank UTR/Transaction reference number for verification.");
                if (bankUtrField != null) bankUtrField.requestFocus();
                return;
            }
        }

        // Populate Confirmation Screen
        populateConfirmationScreen();

        // Switch Views
        if (formContainer != null) {
            formContainer.setVisible(false);
            formContainer.setManaged(false);
        }
        if (confirmationContainer != null) {
            confirmationContainer.setVisible(true);
            confirmationContainer.setManaged(true);
        }
    }

    private void populateConfirmationScreen() {
        String custName = customer != null && customer.getFullName() != null ? customer.getFullName() : "Customer";
        if (confirmCustomerLabel != null) confirmCustomerLabel.setText(custName);
        if (confirmBillNumberLabel != null) {
            confirmBillNumberLabel.setText(bill.getBillNumber() + " (Period " + String.format("%02d/%d", bill.getBillingMonth(), bill.getBillingYear()) + ")");
        }
        if (confirmAmountLabel != null) confirmAmountLabel.setText("₹" + validatedAmount);

        String methodText;
        String natureText;
        String badgeText;

        switch (selectedMethodIndex) {
            case 0:
                String upiId = upiIdField != null ? upiIdField.getText().trim() : "UPI";
                methodText = "📱 Online UPI (Demo) — " + upiId;
                natureText = "⚡ Demo Sandbox Simulation: Real payment gateway is not configured in this environment. Submitting records a demo transaction and updates ledger directly.";
                badgeText = "DEMO SIMULATION";
                break;
            case 1:
                methodText = "💳 Debit/Credit Card (Demo) — Visa / MasterCard";
                natureText = "⚡ Demo Sandbox Simulation: Real card gateway is not configured. Submitting records a demo transaction and updates ledger directly.";
                badgeText = "DEMO SIMULATION";
                break;
            case 2:
                String bank = bankSelectorCombo != null && bankSelectorCombo.getValue() != null ? bankSelectorCombo.getValue() : "HDFC Bank";
                methodText = "🏦 Net Banking (Demo) — " + bank;
                natureText = "⚡ Demo Sandbox Simulation: Real net banking gateway is not configured. Submitting records a demo transaction and updates ledger directly.";
                badgeText = "DEMO SIMULATION";
                break;
            case 3:
                methodText = "💵 Cash on Delivery / Direct Vendor Collection";
                natureText = "💵 Pending Doorstep Collection: Please hand over ₹" + validatedAmount + " directly to your delivery agent. This payment will remain PENDING until the vendor confirms collection.";
                badgeText = "PENDING COLLECTION";
                break;
            case 4:
                String utr = bankUtrField != null ? bankUtrField.getText().trim() : "Pending";
                methodText = "🏛️ Direct Bank Transfer (NEFT/IMPS) — UTR: " + utr;
                natureText = "⏳ Pending Verification: Your UTR reference (" + utr + ") will be submitted to the dairy office. Ledger balance will be credited after admin/vendor approval.";
                badgeText = "PENDING APPROVAL";
                break;
            default:
                methodText = "Payment";
                natureText = "";
                badgeText = "REVIEW";
        }

        if (confirmMethodLabel != null) confirmMethodLabel.setText(methodText);
        if (confirmNatureLabel != null) confirmNatureLabel.setText(natureText);
        if (confirmStatusBadge != null) confirmStatusBadge.setText(badgeText);
    }

    /**
     * STEP 2 -> STEP 1: Return to form edit view.
     */
    @FXML
    public void handleBackToForm() {
        hideError();
        if (confirmationContainer != null) {
            confirmationContainer.setVisible(false);
            confirmationContainer.setManaged(false);
        }
        if (formContainer != null) {
            formContainer.setVisible(true);
            formContainer.setManaged(true);
        }
    }

    /**
     * STEP 2: Confirm and process payment via PaymentService and MySQL database.
     */
    @FXML
    public void handleConfirmPayment() {
        hideError();

        if (bill == null || validatedAmount == null || validatedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            showError("Invalid payment state. Please return to the form and try again.");
            return;
        }

        Date paymentDate = new Date(System.currentTimeMillis());
        long timestamp = System.currentTimeMillis() % 10000000;

        try {
            if (selectedMethodIndex == 4) {
                // BANK TRANSFER (Pending Verification)
                String utr = bankUtrField != null ? bankUtrField.getText().trim() : "";
                if (utr.isEmpty()) {
                    showError("UTR number is required for bank transfer verification.");
                    handleBackToForm();
                    return;
                }

                Payment payment = paymentService.recordPendingPayment(
                        bill.getBillId(),
                        bill.getCustomerId(),
                        validatedAmount,
                        paymentDate,
                        PaymentMode.NET_BANKING,
                        utr
                );

                if (dialogStage != null) dialogStage.close();

                String dateFormatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
                String receiptMessage =
                        "==============================================\n"
                        + "    BANK TRANSFER SUBMISSION (PENDING)        \n"
                        + "==============================================\n"
                        + "• Transaction / UTR : " + utr + "\n"
                        + "• Payment Mode      : Direct Bank Transfer (NEFT/IMPS)\n"
                        + "• Amount Submitted  : ₹" + validatedAmount + "\n"
                        + "• Submission Date   : " + dateFormatted + "\n"
                        + "• Bill Number       : " + bill.getBillNumber() + "\n"
                        + "• Billing Period    : " + String.format("%02d/%d", bill.getBillingMonth(), bill.getBillingYear()) + "\n"
                        + "• Outstanding Due   : ₹" + bill.getBalanceDue() + "\n"
                        + "• Payment Status    : PENDING VERIFICATION\n"
                        + "==============================================\n\n"
                        + "Your payment reference has been recorded and is currently awaiting vendor/admin verification.\n"
                        + "Your account ledger balance will be credited once verified.";

                AlertUtils.showInfo("Bank Transfer Submitted", receiptMessage);

            } else if (selectedMethodIndex == 3) {
                // CASH ON DELIVERY (Pending Collection)
                String codRef = "COD-" + bill.getBillId() + "-" + timestamp;

                Payment payment = paymentService.recordPendingPayment(
                        bill.getBillId(),
                        bill.getCustomerId(),
                        validatedAmount,
                        paymentDate,
                        PaymentMode.CASH,
                        codRef
                );

                if (dialogStage != null) dialogStage.close();

                String dateFormatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
                String receiptMessage =
                        "==============================================\n"
                        + "    CASH ON DELIVERY REQUEST (PENDING)        \n"
                        + "==============================================\n"
                        + "• Request Reference : " + codRef + "\n"
                        + "• Collection Mode   : Cash on Delivery (Doorstep Handover)\n"
                        + "• Amount Due        : ₹" + validatedAmount + "\n"
                        + "• Request Date      : " + dateFormatted + "\n"
                        + "• Bill Number       : " + bill.getBillNumber() + "\n"
                        + "• Billing Period    : " + String.format("%02d/%d", bill.getBillingMonth(), bill.getBillingYear()) + "\n"
                        + "• Outstanding Due   : ₹" + bill.getBalanceDue() + "\n"
                        + "• Payment Status    : PENDING DOORSTEP COLLECTION\n"
                        + "==============================================\n\n"
                        + "Please hand over ₹" + validatedAmount + " directly to your milk delivery agent.\n"
                        + "Your invoice will be marked as settled once the delivery agent confirms cash collection.";

                AlertUtils.showInfo("Cash Collection Initiated", receiptMessage);

            } else {
                // ONLINE PAYMENT (Demo Sandbox Simulation)
                PaymentMode mode;
                String modeDisplayName;
                String refPrefix;

                if (selectedMethodIndex == 0) {
                    mode = PaymentMode.UPI;
                    modeDisplayName = "UPI (Demo) • " + (upiIdField != null ? upiIdField.getText().trim() : "UPI");
                    refPrefix = "DEMO-UPI-";
                } else if (selectedMethodIndex == 1) {
                    mode = PaymentMode.CREDIT;
                    modeDisplayName = "Debit/Credit Card (Demo)";
                    refPrefix = "DEMO-CARD-";
                } else {
                    mode = PaymentMode.NET_BANKING;
                    String bank = bankSelectorCombo != null && bankSelectorCombo.getValue() != null ? bankSelectorCombo.getValue() : "Bank";
                    modeDisplayName = "Net Banking (Demo) • " + bank;
                    refPrefix = "DEMO-NETBANK-";
                }

                String transactionRef = refPrefix + timestamp;

                Payment payment = paymentService.recordPayment(
                        bill.getBillId(),
                        bill.getCustomerId(),
                        validatedAmount,
                        paymentDate,
                        mode,
                        transactionRef
                );

                BigDecimal updatedBalance = bill.getBalanceDue().subtract(validatedAmount).setScale(2, RoundingMode.HALF_UP);
                if (updatedBalance.compareTo(BigDecimal.ZERO) < 0) updatedBalance = BigDecimal.ZERO;
                String newStatus = updatedBalance.compareTo(BigDecimal.ZERO) == 0 ? "PAID (Fully Settled)" : "PARTIALLY PAID (₹" + updatedBalance + " remaining)";

                if (dialogStage != null) dialogStage.close();

                String dateFormatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
                String receiptMessage =
                        "==============================================\n"
                        + "         TRANSACTION RECEIPT (DEMO)           \n"
                        + "==============================================\n"
                        + "• Transaction Ref : " + transactionRef + "\n"
                        + "• Payment Method  : " + modeDisplayName + "\n"
                        + "• Amount Paid     : ₹" + validatedAmount + "\n"
                        + "• Payment Date    : " + dateFormatted + "\n"
                        + "• Bill Number     : " + bill.getBillNumber() + "\n"
                        + "• Billing Period  : " + String.format("%02d/%d", bill.getBillingMonth(), bill.getBillingYear()) + "\n"
                        + "• Previous Due    : ₹" + bill.getBalanceDue() + "\n"
                        + "• Remaining Due   : ₹" + updatedBalance + "\n"
                        + "• Bill Status     : " + newStatus + "\n"
                        + "• Payment Result  : SUCCESS (Demo Sandbox)\n"
                        + "==============================================\n\n"
                        + "Your monthly bill and payment ledger have been updated successfully in the database.";

                AlertUtils.showInfo("Payment Successful — Receipt", receiptMessage);
            }

            // Trigger UI refresh callback
            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }

        } catch (Exception e) {
            logger.error("Failed to execute payment transaction", e);
            showError("Transaction error: " + e.getMessage());
        }
    }

    @FXML
    public void handleCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }
}
