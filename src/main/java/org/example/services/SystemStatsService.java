package org.example.services;

import com.sun.management.OperatingSystemMXBean;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

public class SystemStatsService {

    private static final SystemStatsService INSTANCE = new SystemStatsService();
    private final OperatingSystemMXBean osBean;

    private SystemStatsService() {
        osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    public static SystemStatsService getInstance() {
        return INSTANCE;
    }

    public SystemSnapshot capture() {
        double cpuUsage = osBean.getCpuLoad();
        if (cpuUsage < 0) {
            cpuUsage = osBean.getProcessCpuLoad();
        }

        long totalMemory = osBean.getTotalMemorySize();
        long freeMemory = osBean.getFreeMemorySize();
        long usedMemory = Math.max(0, totalMemory - freeMemory);

        DiskStats diskStats = captureDiskStats();

        return new SystemSnapshot(
                clamp(cpuUsage),
                usedMemory,
                totalMemory,
                diskStats.usedBytes(),
                diskStats.totalBytes(),
                diskStats.freeBytes()
        );
    }

    private DiskStats captureDiskStats() {
        File[] roots = File.listRoots();
        if (roots == null || roots.length == 0) {
            return new DiskStats(0, 0, 0);
        }

        long total = Arrays.stream(roots).mapToLong(File::getTotalSpace).sum();
        long free = Arrays.stream(roots).mapToLong(File::getUsableSpace).sum();
        long used = Math.max(0, total - free);
        return new DiskStats(used, total, free);
    }

    private double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0;
        }
        return Math.max(0, Math.min(1, value));
    }

    private record DiskStats(long usedBytes, long totalBytes, long freeBytes) {
    }

    public record SystemSnapshot(
            double cpuUsage,
            long usedMemoryBytes,
            long totalMemoryBytes,
            long usedDiskBytes,
            long totalDiskBytes,
            long freeDiskBytes
    ) {
        public double memoryUsage() {
            return totalMemoryBytes <= 0 ? 0 : (double) usedMemoryBytes / totalMemoryBytes;
        }

        public double diskUsage() {
            return totalDiskBytes <= 0 ? 0 : (double) usedDiskBytes / totalDiskBytes;
        }
    }
}
