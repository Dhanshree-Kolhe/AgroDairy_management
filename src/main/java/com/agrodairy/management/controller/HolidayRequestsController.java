package com.agrodairy.management.controller;

import com.agrodairy.management.dao.CustomerDAO;
import com.agrodairy.management.dao.impl.CustomerDAOImpl;
import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.HolidayRequest;
import com.agrodairy.management.service.HolidayRequestService;
import com.agrodairy.management.service.impl.HolidayRequestServiceImpl;
import com.agrodairy.management.util.AlertUtils;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.Optional;

public class HolidayRequestsController {

    @FXML private TableView<HolidayRow> holidayTable;
    @FXML private TableColumn<HolidayRow, String> colReqId;
    @FXML private TableColumn<HolidayRow, String> colCustName;
    @FXML private TableColumn<HolidayRow, String> colStartDate;
    @FXML private TableColumn<HolidayRow, String> colEndDate;
    @FXML private TableColumn<HolidayRow, String> colReason;
    @FXML private TableColumn<HolidayRow, String> colStatus;

    private final HolidayRequestService holidayService;
    private final CustomerDAO customerDAO;

    public HolidayRequestsController() {
        this.holidayService = new HolidayRequestServiceImpl();
        this.customerDAO = new CustomerDAOImpl();
    }

    @FXML
    public void initialize() {
        setupTable();
        refreshTable();
    }

    private void setupTable() {
        colReqId.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getRequest().getRequestId()));
        colCustName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCustomerName()));
        colStartDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRequest().getStartDate().toString()));
        colEndDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRequest().getEndDate().toString()));
        colReason.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRequest().getReason()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRequest().getApprovalStatus().name()));
    }

    @FXML
    public void refreshTable() {
        List<HolidayRequest> pending = holidayService.getPendingRequests();
        List<HolidayRow> rows = pending.stream().map(req -> {
            Optional<Customer> c = customerDAO.findById(req.getCustomerId());
            String cName = c.isPresent() ? c.get().getFullName() : "Customer #" + req.getCustomerId();
            return new HolidayRow(req, cName);
        }).toList();

        holidayTable.setItems(FXCollections.observableArrayList(rows));
    }

    @FXML
    public void handleApproveHoliday() {
        HolidayRow sel = holidayTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.showWarning("Selection Required", "Please select a holiday request to approve.");
            return;
        }

        holidayService.approveHolidayRequest(sel.getRequest().getRequestId(), "Approved by vendor");
        AlertUtils.showInfo("Holiday Approved", "Holiday pause request approved! Deliveries on paused dates will automatically be marked PAUSED.");
        refreshTable();
    }

    @FXML
    public void handleRejectHoliday() {
        HolidayRow sel = holidayTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.showWarning("Selection Required", "Please select a holiday request to reject.");
            return;
        }

        holidayService.rejectHolidayRequest(sel.getRequest().getRequestId(), "Rejected by vendor");
        AlertUtils.showInfo("Holiday Rejected", "Request rejected.");
        refreshTable();
    }

    public static class HolidayRow {
        private final HolidayRequest request;
        private final String customerName;

        public HolidayRow(HolidayRequest request, String customerName) {
            this.request = request;
            this.customerName = customerName;
        }

        public HolidayRequest getRequest() { return request; }
        public String getCustomerName() { return customerName; }
    }
}
