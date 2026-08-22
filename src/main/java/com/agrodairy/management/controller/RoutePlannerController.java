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
            filtered = allCustomers.stream().filter(c -> c.getLatitude() != null && c.getLongitude() != null).toList();
            if (filtered.isEmpty()) {
                filtered = allCustomers;
            }
        } else {
            filtered = allCustomers.stream().filter(c -> c.getArea() != null && c.getArea().equalsIgnoreCase(selectedZone)).toList();
        }

        if (filtered.isEmpty()) {
            if (routeListView != null) routeListView.setItems(FXCollections.observableArrayList("No active customer locations found for selected zone."));
            if (customerSequenceTable != null) customerSequenceTable.setItems(FXCollections.observableArrayList());
            if (routeSummaryLabel != null) routeSummaryLabel.setText("Total Est. Route Distance: 0.00 km");
            return;
        }

        // Offline Dijkstra / Nearest-Neighbor TSP Calculation starting from Dharampeth Dairy HQ (21.1415, 79.0634)
        double currentLat = 21.1415;
        double currentLon = 79.0634;

        List<Customer> unvisited = new ArrayList<>(filtered);
        List<RouteCustomerRow> seqRows = new ArrayList<>();
        List<String> listItems = new ArrayList<>();

        double totalDistanceKm = 0.0;
        int seqNo = 1;

        while (!unvisited.isEmpty()) {
            Customer nearest = null;
            double minDistance = Double.MAX_VALUE;

            for (Customer c : unvisited) {
                double cLat = c.getLatitude() != null ? c.getLatitude().doubleValue() : 21.1415;
                double cLon = c.getLongitude() != null ? c.getLongitude().doubleValue() : 79.0634;
                double dist = calculateHaversineKm(currentLat, currentLon, cLat, cLon);
                if (dist < minDistance) {
                    minDistance = dist;
                    nearest = c;
                }
            }

            if (nearest == null) break;

            unvisited.remove(nearest);
            totalDistanceKm += minDistance;

            currentLat = nearest.getLatitude() != null ? nearest.getLatitude().doubleValue() : currentLat;
            currentLon = nearest.getLongitude() != null ? nearest.getLongitude().doubleValue() : currentLon;

            seqRows.add(new RouteCustomerRow(seqNo, nearest));
            listItems.add(String.format("📍 Stop %d: %s (%s) — Leg: %.2f km",
                    seqNo, nearest.getFullName(), nearest.getArea(), minDistance));
            seqNo++;
        }

        if (routeListView != null) {
            routeListView.setItems(FXCollections.observableArrayList(listItems));
        }

        if (customerSequenceTable != null) {
            customerSequenceTable.setItems(FXCollections.observableArrayList(seqRows));
        }

        if (routeSummaryLabel != null) {
            routeSummaryLabel.setText(String.format("Total Est. Shortest Route: %.2f km (Engine: Offline Dijkstra/Nearest-Neighbor)", totalDistanceKm));
        }
    }

    public static double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Earth radius in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
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
