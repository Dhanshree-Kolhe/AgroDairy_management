package com.agrodairy.management.controller;

import com.agrodairy.management.config.AppConstants;
import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.enums.DeliveryShift;
import com.agrodairy.management.model.enums.MilkType;
import com.agrodairy.management.model.enums.SubscriptionStatus;
import com.agrodairy.management.service.CustomerService;
import com.agrodairy.management.service.impl.CustomerServiceImpl;
import com.agrodairy.management.util.AlertUtils;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.util.List;

public class CustomerManagementController {

    @FXML private TextField searchField;
    @FXML private TextField fullNameField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> areaComboBox;
    @FXML private TextField houseStreetField;
    @FXML private ComboBox<MilkType> milkTypeComboBox;
    @FXML private ComboBox<DeliveryShift> shiftComboBox;
    @FXML private TextField morningQtyField;
    @FXML private TextField eveningQtyField;
    @FXML private TextField priceField;
    @FXML private ComboBox<SubscriptionStatus> statusComboBox;

    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, String> colCode;
    @FXML private TableColumn<Customer, String> colName;
    @FXML private TableColumn<Customer, String> colPhone;
    @FXML private TableColumn<Customer, String> colArea;
    @FXML private TableColumn<Customer, String> colMilk;
    @FXML private TableColumn<Customer, String> colMorn;
    @FXML private TableColumn<Customer, String> colEve;
    @FXML private TableColumn<Customer, String> colRate;
    @FXML private TableColumn<Customer, String> colStatus;

    private final CustomerService customerService;
    private Customer selectedCustomer = null;

    public CustomerManagementController() {
        this.customerService = new CustomerServiceImpl();
    }

    @FXML
    public void initialize() {
        populateDropdowns();
        setupTable();
        loadCustomers();

        customerTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedCustomer = newSel;
                populateForm(newSel);
            }
        });
    }

    private void populateDropdowns() {
        if (areaComboBox != null) areaComboBox.setItems(FXCollections.observableArrayList(AppConstants.NAGPUR_DELIVERY_ZONES));
        if (milkTypeComboBox != null) milkTypeComboBox.setItems(FXCollections.observableArrayList(MilkType.values()));
        if (shiftComboBox != null) shiftComboBox.setItems(FXCollections.observableArrayList(DeliveryShift.values()));
        if (statusComboBox != null) statusComboBox.setItems(FXCollections.observableArrayList(SubscriptionStatus.values()));
    }

    private void setupTable() {
        colCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCustomerCode()));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        colPhone.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhone()));
        colArea.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getArea()));
        colMilk.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMilkType().name()));
        colMorn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMorningQuantity().toString()));
        colEve.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEveningQuantity().toString()));
        colRate.setCellValueFactory(data -> new SimpleStringProperty("₹" + data.getValue().getCurrentPricePerLiter().toString()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
    }

    private void loadCustomers() {
        List<Customer> list = customerService.getAllCustomers();
        customerTable.setItems(FXCollections.observableArrayList(list));
    }

    @FXML
    public void handleSearch() {
        String q = searchField.getText();
        List<Customer> list = customerService.searchCustomers(q);
        customerTable.setItems(FXCollections.observableArrayList(list));
    }

    @FXML
    public void handleResetSearch() {
        searchField.clear();
        loadCustomers();
    }

    @FXML
    public void handleClearForm() {
        selectedCustomer = null;
        customerTable.getSelectionModel().clearSelection();
        fullNameField.clear();
        phoneField.clear();
        if (areaComboBox != null) areaComboBox.getSelectionModel().clearSelection();
        houseStreetField.clear();
        if (milkTypeComboBox != null) milkTypeComboBox.getSelectionModel().clearSelection();
        if (shiftComboBox != null) shiftComboBox.getSelectionModel().clearSelection();
        morningQtyField.clear();
        eveningQtyField.clear();
        priceField.clear();
        if (statusComboBox != null) statusComboBox.getSelectionModel().clearSelection();
    }

    @FXML
    public void handleSaveCustomer() {
        try {
            Customer c = (selectedCustomer != null) ? selectedCustomer : new Customer();
            c.setVendorId(1L); // Default vendor
            c.setFullName(fullNameField.getText());
            c.setPhone(phoneField.getText());
            c.setArea(areaComboBox.getValue());
            c.setStreet(houseStreetField.getText());
            c.setHouseNumber(houseStreetField.getText());
            c.setAddress(houseStreetField.getText() + ", " + areaComboBox.getValue() + ", Nagpur");
            c.setCity("Nagpur");
            c.setMilkType(milkTypeComboBox.getValue());
            c.setDeliveryShift(shiftComboBox.getValue());

            c.setMorningQuantity(new BigDecimal(morningQtyField.getText().trim()));
            c.setEveningQuantity(new BigDecimal(eveningQtyField.getText().trim()));
            c.setCurrentPricePerLiter(new BigDecimal(priceField.getText().trim()));
            c.setStatus(statusComboBox.getValue() != null ? statusComboBox.getValue() : SubscriptionStatus.ACTIVE);

            if (selectedCustomer == null) {
                customerService.addCustomer(c);
                AlertUtils.showInfo("Success", "New Customer added successfully!");
            } else {
                customerService.updateCustomer(c);
                AlertUtils.showInfo("Success", "Customer updated successfully!");
            }

            handleClearForm();
            loadCustomers();
        } catch (Exception e) {
            AlertUtils.showError("Validation / Database Error", e.getMessage());
        }
    }

    @FXML
    public void handleDeleteCustomer() {
        if (selectedCustomer == null) {
            AlertUtils.showWarning("Selection Required", "Please select a customer from table to deactivate.");
            return;
        }

        if (AlertUtils.showConfirmation("Confirm Deactivation", "Deactivate subscription for " + selectedCustomer.getFullName() + "?")) {
            customerService.deleteCustomer(selectedCustomer.getCustomerId());
            AlertUtils.showInfo("Success", "Customer status updated to INACTIVE.");
            handleClearForm();
            loadCustomers();
        }
    }

    private void populateForm(Customer c) {
        fullNameField.setText(c.getFullName());
        phoneField.setText(c.getPhone());
        areaComboBox.setValue(c.getArea());
        houseStreetField.setText(c.getAddress());
        milkTypeComboBox.setValue(c.getMilkType());
        shiftComboBox.setValue(c.getDeliveryShift());
        morningQtyField.setText(c.getMorningQuantity().toString());
        eveningQtyField.setText(c.getEveningQuantity().toString());
        priceField.setText(c.getCurrentPricePerLiter().toString());
        statusComboBox.setValue(c.getStatus());
    }
}
