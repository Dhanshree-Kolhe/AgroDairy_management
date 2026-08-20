package com.agrodairy.management.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Global Navigation Manager for scene switching and dynamic view loading in JavaFX.
 */
public class NavigationManager {

    private static final Logger logger = LoggerFactory.getLogger(NavigationManager.class);
    private static Stage primaryStage;
    private static BorderPane mainLayoutPane;

    private NavigationManager() {}

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    public static void setMainLayoutPane(BorderPane pane) {
        mainLayoutPane = pane;
    }

    public static void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1100, 700);
            scene.getStylesheets().add(NavigationManager.class.getResource("/styles/main.css").toExternalForm());
            
            if (primaryStage != null) {
                primaryStage.setTitle("Smart Dairy Management System - " + title);
                primaryStage.setScene(scene);
                primaryStage.centerOnScreen();
                primaryStage.show();
            }
        } catch (IOException e) {
            logger.error("Failed to load scene from FXML path: {}", fxmlPath, e);
            AlertUtils.showError("Navigation Error", "Could not load screen: " + fxmlPath + "\n" + e.getMessage());
        }
    }

    public static void loadCenterView(String fxmlPath) {
        if (mainLayoutPane == null) {
            logger.warn("Main layout pane is null. Falling back to scene switch.");
            switchScene(fxmlPath, "View");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(NavigationManager.class.getResource(fxmlPath));
            Parent view = loader.load();
            mainLayoutPane.setCenter(view);
        } catch (IOException e) {
            logger.error("Failed to load view into center pane: {}", fxmlPath, e);
            AlertUtils.showError("Navigation Error", "Could not load view content: " + fxmlPath + "\n" + e.getMessage());
        }
    }
}
