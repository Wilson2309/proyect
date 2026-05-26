package org.example.models;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class CleaningItem {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String id;
    private final String name;
    private final Path path;
    private final long sizeBytes;
    private final CleaningCategory category;
    private final LocalDateTime modifiedAt;
    private final boolean deletable;

    public CleaningItem(
            String name,
            Path path,
            long sizeBytes,
            CleaningCategory category,
            Instant modifiedAt,
            boolean deletable
    ) {
        this.id = UUID.randomUUID().toString();
        this.name = name == null || name.isBlank() ? "Archivo temporal" : name;
        this.path = path;
        this.sizeBytes = Math.max(0, sizeBytes);
        this.category = category == null ? CleaningCategory.JUNK : category;
        this.modifiedAt = modifiedAt == null
                ? LocalDateTime.now()
                : LocalDateTime.ofInstant(modifiedAt, ZoneId.systemDefault());
        this.deletable = deletable;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Path getPath() {
        return path;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public CleaningCategory getCategory() {
        return category;
    }

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public String getModifiedAtFormatted() {
        return modifiedAt.format(FORMATTER);
    }

    public enum CleaningCategory {
        TEMPORARY("Temporales"),
        CACHE("Cache"),
        LOGS("Logs"),
        JUNK("Basura");

        private final String displayName;

        CleaningCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
