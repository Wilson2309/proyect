package org.example.services;

import org.example.models.Programa;
import org.example.models.ProgramaTipo;
import org.example.models.SecurityThreat;
import org.example.models.SecurityThreat.RiskLevel;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SecurityService {

    private static final SecurityService INSTANCE = new SecurityService();
    private static final long ONE_GB_MB = 1024;
    private static final Set<String> SUSPICIOUS_TERMS = Set.of(
            "crack", "keygen", "activator", "patcher", "hack", "injector", "miner",
            "crypt", "trojan", "malware", "spy", "stealer", "loader", "cheat"
    );

    private final ProgramasService programasService = ProgramasService.getInstance();
    private final ProgramaClassifier classifier = ProgramaClassifier.getInstance();
    private SecurityScanResult lastResult = SecurityScanResult.empty();

    private SecurityService() {
    }

    public static SecurityService getInstance() {
        return INSTANCE;
    }

    public SecurityScanResult getLastResult() {
        return lastResult;
    }

    public SecurityScanResult runQuickScan() {
        SecurityScanResult result = analyze(false);
        lastResult = result;
        return result;
    }

    public SecurityScanResult runDeepScan() {
        SecurityScanResult result = analyze(true);
        lastResult = result;
        return result;
    }

    private SecurityScanResult analyze(boolean deepScan) {
        List<Programa> programs = new ArrayList<>(programasService.getProgramas());
        List<SecurityThreat> threats = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        SystemStatsService.SystemSnapshot snapshot = SystemStatsService.getInstance().capture();
        int processCount = countProcesses();
        FirewallState firewallState = readFirewallState();
        long heavyPrograms = programs.stream()
                .filter(program -> program.getSizeMB() != null && program.getSizeMB() >= ONE_GB_MB)
                .count();
        long drivers = programs.stream()
                .filter(program -> classifier.classify(program) == ProgramaTipo.SISTEMA)
                .count();

        detectSuspiciousPrograms(programs, threats);
        detectSystemPressure(snapshot, processCount, threats);
        detectInventoryRisks(programs, heavyPrograms, drivers, deepScan, threats);
        detectFirewallRisk(firewallState, threats);

        buildRecommendations(threats, recommendations, snapshot, processCount, heavyPrograms, firewallState);

        int score = calculateScore(threats, snapshot, processCount, firewallState);
        SecurityStatus status = score >= 82 ? SecurityStatus.SAFE : score >= 58 ? SecurityStatus.ATTENTION : SecurityStatus.RISK;

        return new SecurityScanResult(
                status,
                score,
                threats,
                recommendations,
                programs.size(),
                processCount,
                heavyPrograms,
                drivers,
                firewallState,
                LocalDateTime.now()
        );
    }

    private void detectSuspiciousPrograms(List<Programa> programs, List<SecurityThreat> threats) {
        for (Programa program : programs) {
            String name = safe(program.getNombre()).toLowerCase(Locale.ROOT);
            String publisher = safe(program.getPublisher()).toLowerCase(Locale.ROOT);
            boolean suspicious = SUSPICIOUS_TERMS.stream().anyMatch(term -> name.contains(term) || publisher.contains(term));
            if (suspicious) {
                threats.add(new SecurityThreat(
                        program.getNombre(),
                        RiskLevel.HIGH,
                        "El nombre o editor coincide con patrones comunmente asociados a software no confiable.",
                        "Revise el origen del programa y considere desinstalarlo si no lo reconoce."
                ));
            }
        }
    }

    private void detectSystemPressure(
            SystemStatsService.SystemSnapshot snapshot,
            int processCount,
            List<SecurityThreat> threats
    ) {
        if (snapshot.memoryUsage() >= 0.90) {
            threats.add(new SecurityThreat(
                    "Consumo critico de RAM",
                    RiskLevel.HIGH,
                    "El sistema esta usando mas del 90% de la memoria fisica.",
                    "Cierre aplicaciones innecesarias y ejecute un analisis de programas en segundo plano."
            ));
        } else if (snapshot.memoryUsage() >= 0.76) {
            threats.add(new SecurityThreat(
                    "Presion elevada de memoria",
                    RiskLevel.MEDIUM,
                    "El consumo de RAM esta por encima del rango ideal.",
                    "Revise programas residentes y procesos que se ejecutan al inicio."
            ));
        }

        if (processCount >= 260) {
            threats.add(new SecurityThreat(
                    "Demasiados procesos activos",
                    RiskLevel.MEDIUM,
                    processCount + " procesos activos pueden reducir visibilidad y rendimiento.",
                    "Revise procesos desconocidos y programas de inicio automatico."
            ));
        }
    }

    private void detectInventoryRisks(
            List<Programa> programs,
            long heavyPrograms,
            long drivers,
            boolean deepScan,
            List<SecurityThreat> threats
    ) {
        if (programs.size() >= 140) {
            threats.add(new SecurityThreat(
                    "Inventario de software amplio",
                    RiskLevel.MEDIUM,
                    "Se detectaron " + programs.size() + " programas instalados.",
                    "Desinstale aplicaciones que ya no usa para reducir superficie de riesgo."
            ));
        }

        if (heavyPrograms >= 6) {
            threats.add(new SecurityThreat(
                    "Programas de gran tamano",
                    RiskLevel.LOW,
                    heavyPrograms + " programas superan 1 GB de tamano registrado.",
                    "Revise si todos son necesarios y mantengalos actualizados."
            ));
        }

        if (deepScan && drivers >= 45) {
            threats.add(new SecurityThreat(
                    "Alta cantidad de componentes de sistema",
                    RiskLevel.LOW,
                    "Se encontraron " + drivers + " controladores o paquetes de sistema.",
                    "Mantenga drivers desde fuentes oficiales y evite instaladores desconocidos."
            ));
        }
    }

    private void detectFirewallRisk(FirewallState firewallState, List<SecurityThreat> threats) {
        if (firewallState == FirewallState.DISABLED) {
            threats.add(new SecurityThreat(
                    "Firewall desactivado",
                    RiskLevel.HIGH,
                    "Windows Firewall parece estar desactivado en al menos un perfil.",
                    "Active el firewall para proteger conexiones entrantes no autorizadas."
            ));
        } else if (firewallState == FirewallState.UNKNOWN) {
            threats.add(new SecurityThreat(
                    "Firewall no verificado",
                    RiskLevel.LOW,
                    "No se pudo confirmar el estado del firewall desde el sistema.",
                    "Verifique manualmente Windows Security o ejecute como usuario con permisos suficientes."
            ));
        }
    }

    private void buildRecommendations(
            List<SecurityThreat> threats,
            List<String> recommendations,
            SystemStatsService.SystemSnapshot snapshot,
            int processCount,
            long heavyPrograms,
            FirewallState firewallState
    ) {
        if (threats.isEmpty()) {
            recommendations.add("Sistema funcionando correctamente. Mantenga escaneos periodicos activos.");
        }
        if (snapshot.memoryUsage() >= 0.76) {
            recommendations.add("Considere liberar RAM cerrando aplicaciones residentes o procesos no necesarios.");
        }
        if (processCount >= 220) {
            recommendations.add("Hay muchos procesos ejecutandose. Revise programas de inicio y servicios no usados.");
        }
        if (heavyPrograms >= 4) {
            recommendations.add("Se detectaron programas pesados. Compruebe que sean necesarios y confiables.");
        }
        if (firewallState != FirewallState.ENABLED) {
            recommendations.add("Confirme que el firewall de Windows este activo en redes privadas y publicas.");
        }
        recommendations.add("Mantenga OptiScan Pro y sus programas principales actualizados.");
    }

    private int calculateScore(
            List<SecurityThreat> threats,
            SystemStatsService.SystemSnapshot snapshot,
            int processCount,
            FirewallState firewallState
    ) {
        int score = 100;
        for (SecurityThreat threat : threats) {
            score -= switch (threat.getRiskLevel()) {
                case HIGH -> 18;
                case MEDIUM -> 10;
                case LOW -> 5;
            };
        }
        if (snapshot.memoryUsage() >= 0.85) {
            score -= 8;
        }
        if (processCount >= 280) {
            score -= 8;
        }
        if (firewallState == FirewallState.DISABLED) {
            score -= 15;
        }
        return Math.max(0, Math.min(100, score));
    }

    private int countProcesses() {
        try {
            return (int) ProcessHandle.allProcesses().count();
        } catch (Exception e) {
            return 0;
        }
    }

    private FirewallState readFirewallState() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return FirewallState.UNKNOWN;
        }

        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-NonInteractive",
                    "-Command",
                    "(Get-NetFirewallProfile | Select-Object -ExpandProperty Enabled) -join ','"
            );
            builder.redirectErrorStream(true);
            Process process = builder.start();
            boolean finished = process.waitFor(8, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return FirewallState.UNKNOWN;
            }
            try (InputStream stream = process.getInputStream()) {
                String output = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                        .trim()
                        .toLowerCase(Locale.ROOT);
                if (output.contains("false")) {
                    return FirewallState.DISABLED;
                }
                if (output.contains("true")) {
                    return FirewallState.ENABLED;
                }
            }
        } catch (Exception ignored) {
            return FirewallState.UNKNOWN;
        }
        return FirewallState.UNKNOWN;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public enum SecurityStatus {
        SAFE("Seguro"),
        ATTENTION("Atencion"),
        RISK("Riesgo");

        private final String displayName;

        SecurityStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum FirewallState {
        ENABLED("Activo"),
        DISABLED("Desactivado"),
        UNKNOWN("No verificado");

        private final String displayName;

        FirewallState(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public record SecurityScanResult(
            SecurityStatus status,
            int score,
            List<SecurityThreat> threats,
            List<String> recommendations,
            int programsAnalyzed,
            int processCount,
            long heavyPrograms,
            long driversAnalyzed,
            FirewallState firewallState,
            LocalDateTime scannedAt
    ) {
        public static SecurityScanResult empty() {
            return new SecurityScanResult(
                    SecurityStatus.ATTENTION,
                    0,
                    List.of(),
                    List.of("Ejecute un escaneo para generar recomendaciones."),
                    0,
                    0,
                    0,
                    0,
                    FirewallState.UNKNOWN,
                    null
            );
        }

        public long highThreats() {
            return threats.stream().filter(threat -> threat.getRiskLevel() == RiskLevel.HIGH).count();
        }

        public long mediumThreats() {
            return threats.stream().filter(threat -> threat.getRiskLevel() == RiskLevel.MEDIUM).count();
        }

        public List<SecurityThreat> sortedThreats() {
            return threats.stream()
                    .sorted(Comparator.comparing(SecurityThreat::getRiskLevel).reversed())
                    .toList();
        }
    }
}
