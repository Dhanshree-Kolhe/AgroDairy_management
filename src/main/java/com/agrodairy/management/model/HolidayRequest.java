package com.agrodairy.management.model;

import com.agrodairy.management.model.enums.RequestStatus;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Domain entity representing Customer Delivery Pause (Holiday) applications.
 */
public class HolidayRequest {
    private Long requestId;
    private Long customerId;
    private Date startDate;
    private Date endDate;
    private String reason;
    private RequestStatus approvalStatus;
    private String vendorRemarks;
    private Timestamp createdAt;

    // Transient reference for UI
    private Customer customer;

    public HolidayRequest() {}

    public HolidayRequest(Long requestId, Long customerId, Date startDate, Date endDate, String reason, RequestStatus approvalStatus, String vendorRemarks) {
        this.requestId = requestId;
        this.customerId = customerId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.approvalStatus = approvalStatus;
        this.vendorRemarks = vendorRemarks;
    }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public RequestStatus getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(RequestStatus approvalStatus) { this.approvalStatus = approvalStatus; }

    public String getVendorRemarks() { return vendorRemarks; }
    public void setVendorRemarks(String vendorRemarks) { this.vendorRemarks = vendorRemarks; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HolidayRequest that = (HolidayRequest) o;
        return Objects.equals(requestId, that.requestId) && Objects.equals(customerId, that.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, customerId);
    }

    @Override
    public String toString() {
        return "HolidayRequest{" +
                "requestId=" + requestId +
                ", customerId=" + customerId +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", approvalStatus=" + approvalStatus +
                '}';
    }
}
