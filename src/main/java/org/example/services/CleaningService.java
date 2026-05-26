package org.example.services;

import org.example.models.CleaningItem;
import org.example.models.CleaningItem.CleaningCategory;
import org.example.models.Programa;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class CleaningService {

    private static final CleaningService INSTANCE = new CleaningService();
    private static final int MAX_ITEMS_PER_LOCATION = 900;
    private static final int MAX_VISIBLE_ITEMS = 80;

    private CleaningScanResult lastResult = CleaningScanResult.empty();

    private CleaningService() {
    }

    public static CleaningService getInstance() {
        return INSTANCE;
    }

    public CleaningScanResult getLastResult() {
        return lastResult;
    }

    public CleaningScanResult analyzeSystem() {
        List<CleaningItem> items = new ArrayList<>();
        for (ScanLocation location : buildLocations()) {
            scanLocation(location, items);
        }
        items.sort(Comparator.comparingLong(CleaningItem::getSizeBytes).reversed());
        CleaningScanResult result = buildResult(items, 0, 0);
        lastResult = result;
        return result;
    }

    public CleaningResult cleanSafeItems(List<CleaningItem> items) {
        long released = 0;
        int deleted = 0;
        List<String> errors = new ArrayList<>();

        for (CleaningItem item : items) {
            if (!item.isDeletable() || item.getPath() == null || !isSafePath(item.getPath())) {
                continue;
            }
            try {
                if (Files.isRegularFile(item.getPath())) {
                    long size = Files.size(item.getPath());
                    Files.deleteIfExists(item.getPath());
                    released += size;
                    deleted++;
                }
            } catch (Exception e) {
                errors.add(item.getName() + ": " + e.getMessage());
            }
        }

        CleaningScanResult refreshed = analyzeSystem();
        lastResult = buildResult(refreshed.items(), deleted, released);
        return new CleaningResult(deleted, released, errors);
    }

    public void requestMemoryOptimization() {
        System.gc();
    }

    public boolean openWindowsUninstaller() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) {
            return false;
        }
        try {
            new ProcessBuilder("control.exe", "appwiz.cpl").start();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean openProgramsModuleFallback() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(java.net.URI.create("ms-settings:appsfeatures"));
                return true;
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public List<Programa> suggestedProgramsToReview() {
        return ProgramasService.getInstance().getProgramas().stream()
                .filter(program -> program.getNombre() != null && !program.getNombre().isBlank())
                .sorted(Comparator.comparing(
                        program -> program.getSizeMB() == null ? 0 : program.getSizeMB(),
                        Comparator.reverseOrder()
                ))
                .limit(6)
                .toList();
    }

    private CleaningScanResult buildResult(List<CleaningItem> items, int deletedCount, long releasedBytes) {
        long recoverable = items.stream()
                .filter(CleaningItem::isDeletable)
                .mapToLong(CleaningItem::getSizeBytes)
                .sum();
        long tempCount = items.stream().filter(item -> item.getCategory() == CleaningCategory.TEMPORARY).count();
        long cacheCount = items.stream().filter(item -> item.getCategory() == CleaningCategory.CACHE).count();

        Map<CleaningCategory, Long> bytesByCategory = new EnumMap<>(CleaningCategory.class);
        for (CleaningCategory category : CleaningCategory.values()) {
            bytesByCategory.put(category, 0L);
        }
        for (CleaningItem item : items) {
            bytesByCategory.merge(item.getCategory(), item.getSizeBytes(), Long::sum);
        }

        int score = calculateScore(recoverable, items.size());
        CleaningStatus status = score >= 82 ? CleaningStatus.OPTIMAL : score >= 55 ? CleaningStatus.RECOMMENDED : CleaningStatus.SATURATED;
        List<String> recommendations = buildRecommendations(recoverable, items.size(), cacheCount, tempCount);

        return new CleaningScanResult(
                status,
                score,
                items.stream().limit(MAX_VISIBLE_ITEMS).toList(),
                recommendations,
                recoverable,
                items.size(),
                tempCount,
                cacheCount,
                bytesByCategory,
                deletedCount,
                releasedBytes,
                LocalDateTime.now()
        );
    }

    private int calculateScore(long recoverableBytes, int itemCount) {
        int score = 100;
        long recoverableMb = recoverableBytes / 1024 / 1024;
        if (recoverableMb > 2048) {
            score -= 35;
        } else if (recoverableMb > 768) {
            score -= 22;
        } else if (recoverableMb > 256) {
            score -= 10;
        }
        if (itemCount > 1600) {
            score -= 22;
        } else if (itemCount > 700) {
            score -= 12;
        }
        return Math.max(0, Math.min(100, score));
    }

    private List<String> buildRecommendations(long recoverableBytes, int itemCount, long cacheCount, long tempCount) {
        List<String> recommendations = new ArrayList<>();
        long mb = recoverableBytes / 1024 / 1024;
        if (mb <= 64 && itemCount < 150) {
            recommendations.add("El sistema esta optimizado. No hay acumulacion importante de archivos temporales.");
        } else {
            recommendations.add("Puede liberar aproximadamente " + formatBytes(recoverableBytes) + " con una limpieza segura.");
        }
        if (cacheCount > 300) {
            recommendations.add("Existe cache acumulada. Limpie navegadores o aplicaciones que ya no usa.");
        }
        if (tempCount > 300) {
            recommendations.add("Se detectaron muchos temporales. Ejecute limpieza rapida para reducir ruido del sistema.");
        }
        recommendations.add("Revise programas pesados desde esta pantalla si necesita recuperar mas espacio.");
        recommendations.add("La limpieza evita rutas criticas y solo borra archivos dentro de ubicaciones temporales seguras.");
        return recommendations;
    }

    private void scanLocation(ScanLocation location, List<CleaningItem> items) {
        if (location.path() == null || !Files.isDirectory(location.path())) {
            return;
        }
        int[] added = {0};
        try (Stream<Path> stream = Files.walk(location.path(), 2, FileVisitOption.FOLLOW_LINKS)) {
            stream.filter(Files::isRegularFile)
                    .limit(MAX_ITEMS_PER_LOCATION)
                    .forEach(path -> {
                        if (added[0] >= MAX_ITEMS_PER_LOCATION) {
                            return;
                        }
                        try {
                            long size = Files.size(path);
                            if (size <= 0) {
                                return;
                            }
                            String fileName = path.getFileName() == null ? "Archivo" : path.getFileName().toString();
                            CleaningCategory category = classify(fileName, location.category());
                            boolean deletable = isSafePath(path);
                            items.add(new CleaningItem(fileName, path, size, category, Files.getLastModifiedTime(path).toInstant(), deletable));
                            added[0]++;
                        } catch (Exception ignored) {
                            // Some temp files are locked while applications are running.
                        }
                    });
        } catch (Exception ignored) {
            // Some system folders are unavailable without elevation; skip silently.
        }
    }

    private List<ScanLocation> buildLocations() {
        Set<Path> unique = new HashSet<>();
        List<ScanLocation> locations = new ArrayList<>();

        addLocation(locations, unique, Paths.get(System.getProperty("java.io.tmpdir", "")), CleaningCategory.TEMPORARY);
        addLocation(locations, unique, envPath("TEMP"), CleaningCategory.TEMPORARY);
        addLocation(locations, unique, envPath("TMP"), CleaningCategory.TEMPORARY);
        addLocation(locations, unique, envPath("LOCALAPPDATA", "Temp"), CleaningCategory.TEMPORARY);
        addLocation(locations, unique, envPath("LOCALAPPDATA", "Microsoft", "Windows", "INetCache"), CleaningCategory.CACHE);
        addLocation(locations, unique, envPath("LOCALAPPDATA", "Microsoft", "Windows", "Explorer"), CleaningCategory.CACHE);
        addLocation(locations, unique, envPath("LOCALAPPDATA", "Google", "Chrome", "User Data", "Default", "Cache"), CleaningCategory.CACHE);
        addLocation(locations, unique, envPath("APPDATA", "Mozilla", "Firefox", "Profiles"), CleaningCategory.CACHE);
        addLocation(locations, unique, Paths.get("C:", "Windows", "Temp"), CleaningCategory.TEMPORARY);
        addLocation(locations, unique, Paths.get("C:", "Windows", "Prefetch"), CleaningCategory.CACHE);

        return locations;
    }

    private void addLocation(List<ScanLocation> locations, Set<Path> unique, Path path, CleaningCategory category) {
        if (path == null) {
            return;
        }
        Path normalized = path.toAbsolutePath().normalize();
        if (unique.add(normalized)) {
            locations.add(new ScanLocation(normalized, category));
        }
    }

    private Path envPath(String key, String... children) {
        String base = System.getenv(key);
        if (base == null || base.isBlank()) {
            return null;
        }
        Path path = Paths.get(base);
        for (String child : children) {
            path = path.resolve(child);
        }
        return path;
    }

    private CleaningCategory classify(String fileName, CleaningCategory fallback) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".log") || lower.endsWith(".old")) {
            return CleaningCategory.LOGS;
        }
        if (lower.endsWith(".tmp") || lower.endsWith(".temp")) {
            return CleaningCategory.TEMPORARY;
        }
        if (lower.contains("cache") || lower.endsWith(".dat")) {
            return CleaningCategory.CACHE;
        }
        return fallback == null ? CleaningCategory.JUNK : fallback;
    }

    private boolean isSafePath(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        for (ScanLocation location : buildLocations()) {
            if (normalized.startsWith(location.path())) {
                return true;
            }
        }
        return false;
    }

    public String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "0 MB";
        }
        double gb = bytes / 1024.0 / 1024.0 / 1024.0;
        if (gb >= 1) {
            return String.format(Locale.US, "%.1f GB", gb);
        }
        return String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0);
    }

    private record ScanLocation(Path path, CleaningCategory category) {
    }

    public enum CleaningStatus {
        OPTIMAL("Optimo"),
        RECOMMENDED("Recomendado limpiar"),
        SATURATED("Saturado");

        private final String displayName;

        CleaningStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public record CleaningScanResult(
            CleaningStatus status,
            int score,
            List<CleaningItem> items,
            List<String> recommendations,
            long recoverableBytes,
            int filesAnalyzed,
            long temporaryFiles,
            long cacheFiles,
            Map<CleaningCategory, Long> bytesByCategory,
            int deletedCount,
            long releasedBytes,
            LocalDateTime analyzedAt
    ) {
        public static CleaningScanResult empty() {
            Map<CleaningCategory, Long> emptyBytes = new EnumMap<>(CleaningCategory.class);
            for (CleaningCategory category : CleaningCategory.values()) {
                emptyBytes.put(category, 0L);
            }
            return new CleaningScanResult(
                    CleaningStatus.RECOMMENDED,
                    0,
                    List.of(),
                    List.of("Ejecute un analisis para detectar archivos innecesarios."),
                    0,
                    0,
                    0,
                    0,
                    emptyBytes,
                    0,
                    0,
                    null
            );
        }
    }

    public record CleaningResult(int deletedCount, long releasedBytes, List<String> errors) {
    }
}
