package com.agrodairy.management.dao;

import com.agrodairy.management.model.HolidayRequest;
import com.agrodairy.management.model.enums.RequestStatus;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

public interface HolidayRequestDAO {
    Long insert(HolidayRequest request);
    boolean updateStatus(Long requestId, RequestStatus status, String vendorRemarks);
    Optional<HolidayRequest> findById(Long requestId);
    List<HolidayRequest> findByCustomerId(Long customerId);
    List<HolidayRequest> findPendingRequests();
    boolean isCustomerOnHoliday(Long customerId, Date date);
    List<HolidayRequest> findApprovedHolidaysForPeriod(Long customerId, Date startDate, Date endDate);
}
