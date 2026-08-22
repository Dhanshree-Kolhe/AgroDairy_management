package com.agrodairy.management.service;

public interface CloudBackupService {
    boolean isCloudBackupConfigured();
    boolean syncLocalDataToCloud();
    String getCloudBackupStatus();
}
