package org.example.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class SecurityThreat {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String id;
    private final String name;
    private final RiskLevel riskLevel;
    private final String description;
    private final String recommendation;
    private final LocalDateTime detectedAt;

    public SecurityThreat(
            String name,
            RiskLevel riskLevel,
            String description,
            String recommendation
    ) {
        this(UUID.randomUUID().toString(), name, riskLevel, description, recommendation, LocalDateTime.now());
    }

    public SecurityThreat(
            String id,
            String name,
            RiskLevel riskLevel,
            String description,
            String recommendation,
            LocalDateTime detectedAt
    ) {
        this.id = id;
        this.name = name == null ? "Amenaza sin nombre" : name;
        this.riskLevel = riskLevel == null ? RiskLevel.LOW : riskLevel;
        this.description = description == null ? "" : description;
        this.recommendation = recommendation == null ? "" : recommendation;
        this.detectedAt = detectedAt == null ? LocalDateTime.now() : detectedAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public String getDescription() {
        return description;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public String getDetectedAtFormatted() {
        return detectedAt.format(FORMATTER);
    }

    public enum RiskLevel {
        LOW("Bajo"),
        MEDIUM("Medio"),
        HIGH("Alto");

        private final String displayName;

        RiskLevel(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
