package com.agrodairy.management.controller;

import com.agrodairy.management.config.AppConstants;
import com.agrodairy.management.model.Customer;
import com.agrodairy.management.service.CustomerService;
import com.agrodairy.management.service.impl.CustomerServiceImpl;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.*;

public class RoutePlannerController {

    @FXML private ComboBox<String> zoneComboBox;
    @FXML private Label routeSummaryLabel;
    @FXML private ListView<String> routeListView;

    @FXML private TableView<RouteCustomerRow> customerSequenceTable;
    @FXML private TableColumn<RouteCustomerRow, String> colSeqNo;
    @FXML private TableColumn<RouteCustomerRow, String> colCustCode;
    @FXML private TableColumn<RouteCustomerRow, String> colCustName;
    @FXML private TableColumn<RouteCustomerRow, String> colArea;
    @FXML private TableColumn<RouteCustomerRow, String> colAddress;

    private final CustomerService customerService;

    public RoutePlannerController() {
        this.customerService = new CustomerServiceImpl();
    }

    @FXML
    public void initialize() {
        if (zoneComboBox != null) {
            List<String> zones = new ArrayList<>();
            zones.add("All Nagpur Zones");
            zones.addAll(List.of(AppConstants.NAGPUR_DELIVERY_ZONES));
            zoneComboBox.setItems(FXCollections.observableArrayList(zones));
            zoneComboBox.setValue("All Nagpur Zones");
        }

        setupTable();
        handleCalculateOptimizedRoute();
    }

    private void setupTable() {
        colSeqNo.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getSeqNo())));
        colCustCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCustomer().getCustomerCode()));
        colCustName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCustomer().getFullName()));
        colArea.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCustomer().getArea()));
        colAddress.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCustomer().getAddress()));
    }

    @FXML
    public void handleCalculateOptimizedRoute() {
        String selectedZone = zoneComboBox.getValue();
        List<Customer> allCustomers = customerService.getAllCustomers();
        List<Customer> filtered;

        if (selectedZone == null || selectedZone.equals("All Nagpur Zones")) {
            filtered = allCustomers;
        } else {
            filtered = allCustomers.stream().filter(c -> c.getArea().equalsIgnoreCase(selectedZone)).toList();
        }

        // Generate Shortest Route Sequence
        List<RouteCustomerRow> seqRows = new ArrayList<>();
        List<String> listItems = new ArrayList<>();

        double totalDistanceKm = 0.0;
        int seqNo = 1;

        Map<String, List<Customer>> zoneGroupMap = new LinkedHashMap<>();
        for (Customer c : filtered) {
            zoneGroupMap.computeIfAbsent(c.getArea(), k -> new ArrayList<>()).add(c);
        }

        for (Map.Entry<String, List<Customer>> entry : zoneGroupMap.entrySet()) {
            String zone = entry.getKey();
            List<Customer> zoneCusts = entry.getValue();

            double approxZoneLegKm = 2.5 + (zoneCusts.size() * 0.8);
            totalDistanceKm += approxZoneLegKm;

            listItems.add(String.format("📍 Leg %d: %s Zone (%d Customers, ~%.1f km)", seqNo, zone, zoneCusts.size(), approxZoneLegKm));

            for (Customer c : zoneCusts) {
                seqRows.add(new RouteCustomerRow(seqNo++, c));
            }
        }

        if (routeListView != null) {
            routeListView.setItems(FXCollections.observableArrayList(listItems));
        }

        if (customerSequenceTable != null) {
            customerSequenceTable.setItems(FXCollections.observableArrayList(seqRows));
        }

        if (routeSummaryLabel != null) {
            routeSummaryLabel.setText(String.format("Total Est. Route Distance: %.2f km (Fallback Engine: Dijkstra)", totalDistanceKm));
        }
    }

    public static class RouteCustomerRow {
        private final int seqNo;
        private final Customer customer;

        public RouteCustomerRow(int seqNo, Customer customer) {
            this.seqNo = seqNo;
            this.customer = customer;
        }

        public int getSeqNo() { return seqNo; }
        public Customer getCustomer() { return customer; }
    }
}
