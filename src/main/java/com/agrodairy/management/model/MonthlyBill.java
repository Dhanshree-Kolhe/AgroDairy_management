package com.agrodairy.management.model;

import com.agrodairy.management.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Domain entity representing Monthly Billing Summaries.
 */
public class MonthlyBill {
    private Long billId;
    private String billNumber;
    private Long customerId;
    private Integer billingMonth;
    private Integer billingYear;
    private BigDecimal totalMilkQuantity;
    private BigDecimal milkCharges;
    private BigDecimal extraOrdersCharges;
    private BigDecimal holidayDeductions;
    private BigDecimal grossAmount;
    private BigDecimal previousBalance;
    private BigDecimal netPayable;
    private BigDecimal paidAmount;
    private BigDecimal balanceDue;
    private PaymentStatus status;
    private Date billDate;
    private Date dueDate;
    private Timestamp createdAt;

    // Transient reference for UI
    private Customer customer;

    public MonthlyBill() {}

    public MonthlyBill(Long billId, String billNumber, Long customerId, Integer billingMonth, Integer billingYear,
                       BigDecimal totalMilkQuantity, BigDecimal milkCharges, BigDecimal extraOrdersCharges,
                       BigDecimal holidayDeductions, BigDecimal grossAmount, BigDecimal previousBalance,
                       BigDecimal netPayable, BigDecimal paidAmount, BigDecimal balanceDue, PaymentStatus status,
                       Date billDate, Date dueDate) {
        this.billId = billId;
        this.billNumber = billNumber;
        this.customerId = customerId;
        this.billingMonth = billingMonth;
        this.billingYear = billingYear;
        this.totalMilkQuantity = totalMilkQuantity;
        this.milkCharges = milkCharges;
        this.extraOrdersCharges = extraOrdersCharges;
        this.holidayDeductions = holidayDeductions;
        this.grossAmount = grossAmount;
        this.previousBalance = previousBalance;
        this.netPayable = netPayable;
        this.paidAmount = paidAmount;
        this.balanceDue = balanceDue;
        this.status = status;
        this.billDate = billDate;
        this.dueDate = dueDate;
    }

    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }

    public String getBillNumber() { return billNumber; }
    public void setBillNumber(String billNumber) { this.billNumber = billNumber; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Integer getBillingMonth() { return billingMonth; }
    public void setBillingMonth(Integer billingMonth) { this.billingMonth = billingMonth; }

    public Integer getBillingYear() { return billingYear; }
    public void setBillingYear(Integer billingYear) { this.billingYear = billingYear; }

    public BigDecimal getTotalMilkQuantity() { return totalMilkQuantity; }
    public void setTotalMilkQuantity(BigDecimal totalMilkQuantity) { this.totalMilkQuantity = totalMilkQuantity; }

    public BigDecimal getMilkCharges() { return milkCharges; }
    public void setMilkCharges(BigDecimal milkCharges) { this.milkCharges = milkCharges; }

    public BigDecimal getExtraOrdersCharges() { return extraOrdersCharges; }
    public void setExtraOrdersCharges(BigDecimal extraOrdersCharges) { this.extraOrdersCharges = extraOrdersCharges; }

    public BigDecimal getHolidayDeductions() { return holidayDeductions; }
    public void setHolidayDeductions(BigDecimal holidayDeductions) { this.holidayDeductions = holidayDeductions; }

    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }

    public BigDecimal getPreviousBalance() { return previousBalance; }
    public void setPreviousBalance(BigDecimal previousBalance) { this.previousBalance = previousBalance; }

    public BigDecimal getNetPayable() { return netPayable; }
    public void setNetPayable(BigDecimal netPayable) { this.netPayable = netPayable; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BigDecimal getBalanceDue() { return balanceDue; }
    public void setBalanceDue(BigDecimal balanceDue) { this.balanceDue = balanceDue; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public Date getBillDate() { return billDate; }
    public void setBillDate(Date billDate) { this.billDate = billDate; }

    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MonthlyBill bill = (MonthlyBill) o;
        return Objects.equals(billId, bill.billId) && Objects.equals(billNumber, bill.billNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(billId, billNumber);
    }

    @Override
    public String toString() {
        return "MonthlyBill{" +
                "billId=" + billId +
                ", billNumber='" + billNumber + '\'' +
                ", customerId=" + customerId +
                ", billingMonth=" + billingMonth +
                ", billingYear=" + billingYear +
                ", netPayable=" + netPayable +
                ", balanceDue=" + balanceDue +
                ", status=" + status +
                '}';
    }
}
