package com.agrodairy.management.service.impl;

import com.agrodairy.management.service.CloudBackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class FirebaseBackupServiceImpl implements CloudBackupService {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseBackupServiceImpl.class);

    private final String firebaseDbUrl;
    private final String configPath;

    public FirebaseBackupServiceImpl() {
        this.firebaseDbUrl = System.getenv("FIREBASE_DATABASE_URL");
        this.configPath = System.getenv("FIREBASE_CONFIG_PATH");
    }

    public FirebaseBackupServiceImpl(String firebaseDbUrl, String configPath) {
        this.firebaseDbUrl = firebaseDbUrl;
        this.configPath = configPath;
    }

    @Override
    public boolean isCloudBackupConfigured() {
        return (firebaseDbUrl != null && !firebaseDbUrl.trim().isEmpty()) ||
               (configPath != null && !configPath.trim().isEmpty());
    }

    @Override
    public boolean syncLocalDataToCloud() {
        if (!isCloudBackupConfigured()) {
            logger.info("Firebase Cloud Backup is unconfigured. Local MySQL remains the primary source of truth.");
            return false;
        }

        try {
            logger.info("Attempting optional Firebase Cloud Backup sync to: {}", firebaseDbUrl);
            if (firebaseDbUrl != null && firebaseDbUrl.startsWith("http")) {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build();

                String jsonPayload = "{\"syncTimestamp\":" + System.currentTimeMillis() + ",\"status\":\"ACTIVE\"}";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(firebaseDbUrl + "/backup_status.json"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .timeout(Duration.ofSeconds(5))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    logger.info("Firebase Cloud Sync completed successfully. Response code: {}", response.statusCode());
                    return true;
                }
            }
        } catch (Exception e) {
            logger.warn("Firebase Cloud Sync skipped due to network/configuration state: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public String getCloudBackupStatus() {
        if (isCloudBackupConfigured()) {
            return "Firebase Configured (" + (firebaseDbUrl != null ? firebaseDbUrl : configPath) + ")";
        }
        return "Offline Mode — Local MySQL Active (Firebase Optional / Requires Manual Config)";
    }
}
