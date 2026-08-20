package com.agrodairy.management.controller;

import com.agrodairy.management.model.Notification;
import com.agrodairy.management.model.UserLogin;
import com.agrodairy.management.service.NotificationService;
import com.agrodairy.management.service.impl.NotificationServiceImpl;
import com.agrodairy.management.util.AlertUtils;
import com.agrodairy.management.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class NotificationsController {

    @FXML private TableView<Notification> notificationsTable;
    @FXML private TableColumn<Notification, String> colCategory;
    @FXML private TableColumn<Notification, String> colTitle;
    @FXML private TableColumn<Notification, String> colMessage;
    @FXML private TableColumn<Notification, String> colDate;
    @FXML private TableColumn<Notification, String> colStatus;

    private final NotificationService notificationService;

    public NotificationsController() {
        this.notificationService = new NotificationServiceImpl();
    }

    @FXML
    public void initialize() {
        setupTable();
        refreshTable();
    }

    private void setupTable() {
        colCategory.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory().name()));
        colTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        colMessage.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMessage()));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCreatedAt() != null ? data.getValue().getCreatedAt().toString() : ""));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(Boolean.TRUE.equals(data.getValue().getRead()) ? "READ" : "UNREAD"));
    }

    @FXML
    public void refreshTable() {
        UserLogin user = SessionManager.getCurrentUser();
        Long userId = user != null ? user.getUserId() : 1L;

        List<Notification> list = notificationService.getUserNotifications(userId);
        notificationsTable.setItems(FXCollections.observableArrayList(list));
    }

    @FXML
    public void handleMarkAsRead() {
        Notification sel = notificationsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.showWarning("Selection Required", "Please select a notification to mark as read.");
            return;
        }

        notificationService.markAsRead(sel.getNotificationId());
        AlertUtils.showInfo("Notification Updated", "Notification marked as READ.");
        refreshTable();
    }
}
