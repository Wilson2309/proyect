package org.example.services;

import org.example.models.Programa;
import org.example.models.ProgramaTipo;

import java.util.List;
import java.util.Locale;

/**
 * Clasifica programas instalados en aplicaciones de usuario o componentes de sistema.
 */
public final class ProgramaClassifier {

    private static final ProgramaClassifier INSTANCE = new ProgramaClassifier();

    private static final List<String> SYSTEM_KEYWORDS = List.of(
            "driver",
            "controlador",
            "runtime",
            "redistributable",
            "redist",
            "sdk",
            "update for",
            "chipset",
            "firmware",
            "bios",
            "intel(r)",
            "intel ",
            "nvidia",
            "geforce",
            "amd ",
            "ryzen",
            "realtek",
            "microsoft visual c++",
            "visual c++",
            "vc++",
            "framework",
            "package",
            ".net",
            "directx",
            "opencl",
            "cuda",
            "webview",
            "windows software development kit",
            "windows kit",
            "kb",
            "hotfix",
            "service pack",
            "redistributable",
            "xna",
            "physx",
            "bluetooth",
            "wireless",
            "ethernet controller",
            "audio driver",
            "graphics driver",
            "display driver",
            "mouse driver",
            "keyboard driver",
            "synaptics",
            "dolby",
            "killer ",
            "support assistant"
    );

    private ProgramaClassifier() {
    }

    public static ProgramaClassifier getInstance() {
        return INSTANCE;
    }

    public ProgramaTipo classify(Programa programa) {
        String haystack = buildHaystack(programa);

        for (String keyword : SYSTEM_KEYWORDS) {
            if (haystack.contains(keyword)) {
                return ProgramaTipo.SISTEMA;
            }
        }

        if (isMicrosoftSystemComponent(haystack)) {
            return ProgramaTipo.SISTEMA;
        }

        return ProgramaTipo.APLICACION;
    }

    private String buildHaystack(Programa programa) {
        return (safe(programa.getNombre()) + " "
                + safe(programa.getPublisher()) + " "
                + safe(programa.getDescripcion()) + " "
                + safe(programa.getVersion()))
                .toLowerCase(Locale.ROOT);
    }

    private boolean isMicrosoftSystemComponent(String haystack) {
        boolean microsoft = haystack.contains("microsoft");
        if (!microsoft) {
            return false;
        }

        return haystack.contains("runtime")
                || haystack.contains("redistributable")
                || haystack.contains("sdk")
                || haystack.contains(".net")
                || haystack.contains("update")
                || haystack.contains("visual c++")
                || haystack.contains("edge webview")
                || haystack.contains("windows kit");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

