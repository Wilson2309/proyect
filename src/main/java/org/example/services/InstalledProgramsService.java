package org.example.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.models.Programa;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class InstalledProgramsService {

    private static final boolean DEBUG = true;
    private static final int TIMEOUT_SECONDS = 120;
    private static final int JSON_LOG_MAX_CHARS = 2000;

    private static final Set<String> BLOCKED_NAMES = Set.of(
            "write-output",
            "displayname",
            "displayversion",
            "publisher",
            "estimatedsize",
            "select-object",
            "convertTo-json",
            "convertto-json",
            "format-table",
            "get-itemproperty",
            "where-object",
            "foreach-object"
    );

    private static final String POWERSHELL_SCRIPT = """
            $ErrorActionPreference = 'SilentlyContinue'
            $WarningPreference = 'SilentlyContinue'
            [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)

            $paths = @(
                'HKLM:\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*',
                'HKLM:\\Software\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*',
                'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*'
            )

            $items = foreach ($path in $paths) {
                Get-ItemProperty -Path $path -ErrorAction SilentlyContinue
            }

            $apps = @(
                $items |
                    Where-Object {
                        $_.DisplayName -and ($_.DisplayName.ToString().Trim().Length -gt 0)
                    } |
                    ForEach-Object {
                        [PSCustomObject]@{
                            DisplayName    = $_.DisplayName.ToString().Trim()
                            DisplayVersion = if ($_.DisplayVersion) { $_.DisplayVersion.ToString().Trim() } else { '' }
                            Publisher      = if ($_.Publisher) { $_.Publisher.ToString().Trim() } else { '' }
                            EstimatedSize  = if ($_.EstimatedSize) { [int]$_.EstimatedSize } else { 0 }
                        }
                    }
            )

            $json = @($apps) | ConvertTo-Json -Depth 3 -Compress
            if ([string]::IsNullOrWhiteSpace($json)) {
                Write-Output '[]'
            } else {
                Write-Output $json
            }
            """;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

    public ScanResult scanInstalledPrograms() {
        if (!isWindows()) {
            return ScanResult.failure("El escaneo solo está disponible en Windows.");
        }

        try {
            String encoded = encodePowerShell(POWERSHELL_SCRIPT);
            ProcessBuilder builder = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-NonInteractive",
                    "-ExecutionPolicy", "Bypass",
                    "-EncodedCommand", encoded
            );
            builder.redirectErrorStream(true);

            Process process = builder.start();
            String rawOutput = readStream(process.getInputStream());

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ScanResult.failure("El escaneo excedió el tiempo límite.");
            }

            log("PowerShell exit code: " + process.exitValue());
            log("Raw output length: " + rawOutput.length());
            logRawJsonPreview(rawOutput);

            if (rawOutput.isBlank()) {
                return ScanResult.failure("PowerShell no devolvió datos.");
            }

            String json = extractJsonPayload(rawOutput);
            log("Extracted JSON length: " + json.length());
            logRawJsonPreview(json);

            ParseReport report = parseJson(json);
            log("JSON nodes read: " + report.nodesRead);
            log("Programs accepted: " + report.programas.size());
            log("Programs skipped: " + report.skipped);

            if (report.parseError != null) {
                log("Parse error: " + report.parseError);
                return ScanResult.failure("Error al interpretar JSON: " + report.parseError);
            }

            if (report.programas.isEmpty()) {
                return ScanResult.failure(
                        "No se encontraron programas instalados válidos (nodos leídos: "
                                + report.nodesRead + ")."
                );
            }

            // No fallar solo por exit code si ya tenemos datos válidos
            return ScanResult.success(report.programas);
        } catch (Exception e) {
            log("Scan exception: " + e.getMessage());
            e.printStackTrace();
            return ScanResult.failure("Error al escanear: " + e.getMessage());
        }
    }

    private static String encodePowerShell(String script) {
        byte[] bytes = script.getBytes(StandardCharsets.UTF_16LE);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String readStream(InputStream inputStream) throws Exception {
        byte[] bytes = inputStream.readAllBytes();
        String text = new String(bytes, StandardCharsets.UTF_8);
        return stripBom(text).trim();
    }

    private String stripBom(String text) {
        if (text.startsWith("\uFEFF")) {
            return text.substring(1);
        }
        return text;
    }

    private String extractJsonPayload(String raw) {
        String trimmed = raw.trim();
        int arrayStart = trimmed.indexOf('[');
        int arrayEnd = trimmed.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return trimmed.substring(arrayStart, arrayEnd + 1);
        }

        int objectStart = trimmed.indexOf('{');
        int objectEnd = trimmed.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return trimmed.substring(objectStart, objectEnd + 1);
        }

        return trimmed;
    }

    private ParseReport parseJson(String json) {
        ParseReport report = new ParseReport();

        if (json.isBlank() || "[]".equals(json.trim())) {
            log("JSON vacío o array sin elementos.");
            return report;
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            Map<String, Programa> unique = new LinkedHashMap<>();

            if (root.isArray()) {
                for (JsonNode node : root) {
                    report.nodesRead++;
                    if (addIfValid(unique, node)) {
                        report.skipped++;
                    }
                }
            } else if (root.isObject()) {
                report.nodesRead = 1;
                if (addIfValid(unique, root)) {
                    report.skipped++;
                }
            } else {
                report.parseError = "Formato JSON no reconocido.";
                return report;
            }

            report.programas = new ArrayList<>(unique.values());
            return report;
        } catch (Exception e) {
            report.parseError = e.getMessage();
            return report;
        }
    }

    private boolean addIfValid(Map<String, Programa> unique, JsonNode node) {
        String displayName = readDisplayName(node);
        if (!isValidDisplayName(displayName)) {
            if (DEBUG && reportableSkip(displayName)) {
                log("Skipped entry, displayName='" + displayName + "'");
            }
            return true;
        }

        String version = textValue(node, "DisplayVersion");
        String publisher = textValue(node, "Publisher");
        Integer sizeKb = intValue(node, "EstimatedSize");

        Programa programa = Programa.fromInstalled(displayName, version, publisher, sizeKb);
        String key = (displayName + "|" + publisher).toLowerCase(Locale.ROOT);
        unique.putIfAbsent(key, programa);
        return false;
    }

    private boolean reportableSkip(String displayName) {
        return displayName != null && !displayName.isBlank();
    }

    private String readDisplayName(JsonNode node) {
        if (node.hasNonNull("DisplayName")) {
            return node.get("DisplayName").asText().trim();
        }
        Iterator<String> fields = node.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if ("displayname".equalsIgnoreCase(field)) {
                return node.get(field).asText().trim();
            }
        }
        return "";
    }

    private String textValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText().trim();
    }

    private Integer intValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return 0;
        }
        if (value.isNumber()) {
            return Math.max(value.asInt(0), 0);
        }
        try {
            return Math.max(Integer.parseInt(value.asText().trim()), 0);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean isValidDisplayName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }

        String normalized = name.trim().toLowerCase(Locale.ROOT);
        if (BLOCKED_NAMES.contains(normalized)) {
            return false;
        }
        return !normalized.startsWith("write-") && !normalized.startsWith("format-");
    }

    private boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win");
    }

    private void log(String message) {
        if (DEBUG) {
            System.out.println("[InstalledPrograms] " + message);
        }
    }

    private void logRawJsonPreview(String json) {
        if (!DEBUG) {
            return;
        }
        if (json.length() <= JSON_LOG_MAX_CHARS) {
            log("JSON preview: " + json);
        } else {
            log("JSON preview (first " + JSON_LOG_MAX_CHARS + " chars): "
                    + json.substring(0, JSON_LOG_MAX_CHARS) + "...");
        }
    }

    private static final class ParseReport {
        private List<Programa> programas = List.of();
        private int nodesRead;
        private int skipped;
        private String parseError;
    }

    public record ScanResult(boolean ok, List<Programa> programas, String errorMessage) {

        public static ScanResult success(List<Programa> programas) {
            return new ScanResult(true, programas, null);
        }

        public static ScanResult failure(String message) {
            return new ScanResult(false, List.of(), message);
        }
    }
}

