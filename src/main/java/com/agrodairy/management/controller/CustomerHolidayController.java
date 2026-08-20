package com.agrodairy.management.controller;

import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.HolidayRequest;
import com.agrodairy.management.service.HolidayRequestService;
import com.agrodairy.management.service.impl.HolidayRequestServiceImpl;
import com.agrodairy.management.util.AlertUtils;
import com.agrodairy.management.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

public class CustomerHolidayController {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextArea reasonArea;

    @FXML private TableView<HolidayRequest> holidayTable;
    @FXML private TableColumn<HolidayRequest, String> colReqId;
    @FXML private TableColumn<HolidayRequest, String> colStartDate;
    @FXML private TableColumn<HolidayRequest, String> colEndDate;
    @FXML private TableColumn<HolidayRequest, String> colReason;
    @FXML private TableColumn<HolidayRequest, String> colStatus;

    private final HolidayRequestService holidayService;

    public CustomerHolidayController() {
        this.holidayService = new HolidayRequestServiceImpl();
    }

    @FXML
    public void initialize() {
        if (startDatePicker != null) startDatePicker.setValue(LocalDate.now().plusDays(1));
        if (endDatePicker != null) endDatePicker.setValue(LocalDate.now().plusDays(2));

        setupTable();
        loadHistory();
    }

    private void setupTable() {
        colReqId.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getRequestId()));
        colStartDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStartDate().toString()));
        colEndDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEndDate().toString()));
        colReason.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReason()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getApprovalStatus().name()));
    }

    private void loadHistory() {
        Customer c = SessionManager.getCurrentCustomer();
        Long cId = c != null ? c.getCustomerId() : 1L;

        List<HolidayRequest> list = holidayService.getCustomerHolidayHistory(cId);
        holidayTable.setItems(FXCollections.observableArrayList(list));
    }

    @FXML
    public void handleSubmitRequest() {
        try {
            Customer c = SessionManager.getCurrentCustomer();
            Long cId = c != null ? c.getCustomerId() : 1L;

            if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
                AlertUtils.showWarning("Missing Dates", "Please select start and end dates.");
                return;
            }

            Date start = Date.valueOf(startDatePicker.getValue());
            Date end = Date.valueOf(endDatePicker.getValue());
            String reason = reasonArea.getText() != null ? reasonArea.getText().trim() : "Out of town";

            holidayService.createHolidayRequest(cId, start, end, reason);
            AlertUtils.showInfo("Request Submitted", "Your delivery pause request has been submitted to your vendor for approval.");
            reasonArea.clear();
            loadHistory();
        } catch (Exception e) {
            AlertUtils.showError("Submission Error", e.getMessage());
        }
    }
}
