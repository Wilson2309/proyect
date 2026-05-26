package org.example.services;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SystemMonitorService {

    private static final SystemMonitorService INSTANCE = new SystemMonitorService();

    private final SystemStatsService statsService = SystemStatsService.getInstance();
    private final ObjectProperty<SystemStatsService.SystemSnapshot> snapshot =
            new SimpleObjectProperty<>(statsService.capture());

    private ScheduledExecutorService executor;

    private SystemMonitorService() {
        SettingsService.getInstance().actualizacionAutomaticaProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                Platform.runLater(() -> snapshot.set(statsService.capture()));
            }
        });
    }

    public static SystemMonitorService getInstance() {
        return INSTANCE;
    }

    public ReadOnlyObjectProperty<SystemStatsService.SystemSnapshot> snapshotProperty() {
        return snapshot;
    }

    public SystemStatsService.SystemSnapshot getSnapshot() {
        return snapshot.get();
    }

    public void refreshNow() {
        SystemStatsService.SystemSnapshot current = statsService.capture();
        if (Platform.isFxApplicationThread()) {
            snapshot.set(current);
        } else {
            Platform.runLater(() -> snapshot.set(current));
        }
    }

    public synchronized void start() {
        if (executor != null && !executor.isShutdown()) {
            return;
        }

        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "optiscan-system-monitor");
            thread.setDaemon(true);
            return thread;
        });

        executor.scheduleAtFixedRate(() -> {
            SystemStatsService.SystemSnapshot current = statsService.capture();
            Platform.runLater(() -> snapshot.set(current));
        }, 0, 2, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }
}
