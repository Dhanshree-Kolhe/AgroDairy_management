package com.agrodairy.management.model;

import com.agrodairy.management.model.enums.PaymentMode;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Domain entity representing Payment Transactions.
 */
public class Payment {
    private Long paymentId;
    private Long billId;
    private Long customerId;
    private BigDecimal amountPaid;
    private Date paymentDate;
    private PaymentMode paymentMode;
    private String transactionReference;
    private String status; // SUCCESS, PENDING, FAILED
    private Timestamp createdAt;

    public Payment() {}

    public Payment(Long paymentId, Long billId, Long customerId, BigDecimal amountPaid, Date paymentDate, PaymentMode paymentMode, String transactionReference, String status) {
        this.paymentId = paymentId;
        this.billId = billId;
        this.customerId = customerId;
        this.amountPaid = amountPaid;
        this.paymentDate = paymentDate;
        this.paymentMode = paymentMode;
        this.transactionReference = transactionReference;
        this.status = status;
    }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public Date getPaymentDate() { return paymentDate; }
    public void setPaymentDate(Date paymentDate) { this.paymentDate = paymentDate; }

    public PaymentMode getPaymentMode() { return paymentMode; }
    public void setPaymentMode(PaymentMode paymentMode) { this.paymentMode = paymentMode; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(paymentId, payment.paymentId) && Objects.equals(billId, payment.billId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId, billId);
    }

    @Override
    public String toString() {
        return "Payment{" +
                "paymentId=" + paymentId +
                ", billId=" + billId +
                ", customerId=" + customerId +
                ", amountPaid=" + amountPaid +
                ", paymentDate=" + paymentDate +
                ", paymentMode=" + paymentMode +
                ", status='" + status + '\'' +
                '}';
    }
}
