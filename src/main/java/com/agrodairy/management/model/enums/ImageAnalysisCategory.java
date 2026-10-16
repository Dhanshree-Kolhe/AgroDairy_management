package com.agrodairy.management.model.enums;

/**
 * Categories available for visual image analysis in the Dairy AI Assistant.
 */
public enum ImageAnalysisCategory {
    ANIMAL_HEALTH("Cow/buffalo health concerns", "Animal Health & Visible Symptoms"),
    MILK_PROBLEMS("Milk-related problems", "Milk Physical Characteristics & Spoilage");

    private final String displayName;
    private final String description;

    ImageAnalysisCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
