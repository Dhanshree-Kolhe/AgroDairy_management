package com.agrodairy.management.service;

import com.agrodairy.management.model.HolidayRequest;
import com.agrodairy.management.model.enums.RequestStatus;

import java.sql.Date;
import java.util.List;

public interface HolidayRequestService {
    HolidayRequest createHolidayRequest(Long customerId, Date startDate, Date endDate, String reason);
    HolidayRequest approveHolidayRequest(Long requestId, String vendorRemarks);
    HolidayRequest rejectHolidayRequest(Long requestId, String vendorRemarks);
    HolidayRequest getRequestById(Long requestId);
    List<HolidayRequest> getPendingRequests();
    List<HolidayRequest> getCustomerHolidayHistory(Long customerId);
    boolean isCustomerOnHoliday(Long customerId, Date date);
}
