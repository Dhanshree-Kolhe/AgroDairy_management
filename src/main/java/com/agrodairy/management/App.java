package com.agrodairy.management;

import com.agrodairy.management.config.DatabaseConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends Application {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Launching Smart Dairy Management System Desktop Application...");
            com.agrodairy.management.util.NavigationManager.init(primaryStage);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auth/LoginView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1100, 700);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            primaryStage.setTitle("Smart Dairy Management System");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (Exception e) {
            logger.error("Failed to start JavaFX Application", e);
        }
    }

    @Override
    public void stop() {
        logger.info("Shutting down application resources...");
        DatabaseConfig.closePool();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
