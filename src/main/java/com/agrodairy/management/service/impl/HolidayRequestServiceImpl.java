package com.agrodairy.management.service.impl;

import com.agrodairy.management.config.AppConstants;
import com.agrodairy.management.dao.CustomerDAO;
import com.agrodairy.management.dao.HolidayRequestDAO;
import com.agrodairy.management.dao.impl.CustomerDAOImpl;
import com.agrodairy.management.dao.impl.HolidayRequestDAOImpl;
import com.agrodairy.management.exception.ResourceNotFoundException;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.HolidayRequest;
import com.agrodairy.management.model.enums.RequestStatus;
import com.agrodairy.management.service.HolidayRequestService;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class HolidayRequestServiceImpl implements HolidayRequestService {
    private final HolidayRequestDAO holidayRequestDAO;
    private final CustomerDAO customerDAO;

    public HolidayRequestServiceImpl() {
        this.holidayRequestDAO = new HolidayRequestDAOImpl();
        this.customerDAO = new CustomerDAOImpl();
    }

    public HolidayRequestServiceImpl(HolidayRequestDAO holidayRequestDAO, CustomerDAO customerDAO) {
        this.holidayRequestDAO = holidayRequestDAO;
        this.customerDAO = customerDAO;
    }

    @Override
    public HolidayRequest createHolidayRequest(Long customerId, Date startDate, Date endDate, String reason) {
        if (customerId == null || customerId <= 0) throw new ValidationException("Customer ID is required.");
        if (startDate == null || endDate == null) throw new ValidationException("Start date and end date are required.");
        if (startDate.after(endDate)) throw new ValidationException("Start date cannot be after end date.");

        customerDAO.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        // Enforce 10 PM cut-off rule for next morning pause requests
        LocalDate today = LocalDate.now();
        LocalDate startLocalDate = startDate.toLocalDate();
        if (startLocalDate.equals(today.plusDays(1)) && LocalTime.now().getHour() >= AppConstants.HOLIDAY_REQUEST_CUTOFF_HOUR) {
            throw new ValidationException(String.format("Holiday pause request for tomorrow morning must be submitted before %d:00 PM today.", AppConstants.HOLIDAY_REQUEST_CUTOFF_HOUR));
        }

        HolidayRequest request = new HolidayRequest();
        request.setCustomerId(customerId);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setReason(reason);
        request.setApprovalStatus(RequestStatus.PENDING);

        Long id = holidayRequestDAO.insert(request);
        request.setRequestId(id);
        return request;
    }

    @Override
    public HolidayRequest approveHolidayRequest(Long requestId, String vendorRemarks) {
        HolidayRequest req = getRequestById(requestId);
        holidayRequestDAO.updateStatus(requestId, RequestStatus.APPROVED, vendorRemarks);
        req.setApprovalStatus(RequestStatus.APPROVED);
        req.setVendorRemarks(vendorRemarks);
        return req;
    }

    @Override
    public HolidayRequest rejectHolidayRequest(Long requestId, String vendorRemarks) {
        HolidayRequest req = getRequestById(requestId);
        holidayRequestDAO.updateStatus(requestId, RequestStatus.REJECTED, vendorRemarks);
        req.setApprovalStatus(RequestStatus.REJECTED);
        req.setVendorRemarks(vendorRemarks);
        return req;
    }

    @Override
    public HolidayRequest getRequestById(Long requestId) {
        if (requestId == null || requestId <= 0) throw new ValidationException("Invalid request ID.");
        return holidayRequestDAO.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday request not found with ID: " + requestId));
    }

    @Override
    public List<HolidayRequest> getPendingRequests() {
        return holidayRequestDAO.findPendingRequests();
    }

    @Override
    public List<HolidayRequest> getCustomerHolidayHistory(Long customerId) {
        if (customerId == null || customerId <= 0) throw new ValidationException("Invalid customer ID.");
        return holidayRequestDAO.findByCustomerId(customerId);
    }

    @Override
    public boolean isCustomerOnHoliday(Long customerId, Date date) {
        if (customerId == null || date == null) return false;
        return holidayRequestDAO.isCustomerOnHoliday(customerId, date);
    }
}
