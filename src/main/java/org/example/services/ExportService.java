package org.example.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.example.models.HistorialItem;
import org.example.models.Programa;
import org.example.services.HistorialService;
import org.example.services.ProgramasService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportService {

    private static ExportService instance;

    private ExportService() {}

    public static ExportService getInstance() {
        if (instance == null) {
            instance = new ExportService();
        }
        return instance;
    }

    // --- MÉTODOS DE AYUDA ---

    private String sanitizeForPdf(String input) {
        if (input == null) return "";
        // Quita los acentos (á -> a) y elimina caracteres no ASCII que rompan PDFBox Standard14Fonts
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").replaceAll("[^\\x00-\\x7F]", "?");
    }

    private String escapeCsv(String data) {
        if (data == null) return "";
        data = data.replace("\"", "\"\"");
        if (data.contains(",") || data.contains("\"") || data.contains("\n")) {
            return "\"" + data + "\"";
        }
        return data;
    }

    private BufferedWriter createUtf8CsvWriter(File file) throws IOException {
        java.io.OutputStreamWriter osw = new java.io.OutputStreamWriter(new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8);
        osw.write('\ufeff'); // Escribir BOM para que Excel detecte UTF-8
        return new BufferedWriter(osw);
    }

    // --- PROGRAMAS ---

    public boolean exportarProgramas(File file, String format) {
        List<Programa> programas = ProgramasService.getInstance().getProgramas();
        try {
            switch (format.toUpperCase()) {
                case "TXT":
                    return writeProgramasTxt(file, programas);
                case "CSV":
                    return writeProgramasCsv(file, programas);
                case "PDF":
                    return writeProgramasPdf(file, programas);
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean writeProgramasTxt(File file, List<Programa> programas) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            bw.write("--- REPORTE DE PROGRAMAS INSTALADOS ---\n\n");
            for (Programa p : programas) {
                bw.write("Nombre: " + p.getNombre() + "\n");
                bw.write("Editor: " + p.getPublisher() + "\n");
                bw.write("Versión: " + p.getVersion() + "\n");
                bw.write("----------------------------------------\n");
            }
            return true;
        }
    }

    private boolean writeProgramasCsv(File file, List<Programa> programas) throws IOException {
        try (BufferedWriter bw = createUtf8CsvWriter(file)) {
            bw.write("Nombre,Editor,Version\n");
            for (Programa p : programas) {
                bw.write(escapeCsv(p.getNombre()) + "," +
                         escapeCsv(p.getPublisher()) + "," +
                         escapeCsv(p.getVersion()) + "\n");
            }
            return true;
        }
    }

    private boolean writeProgramasPdf(File file, List<Programa> programas) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
            contentStream.newLineAtOffset(50, 700);
            contentStream.showText("Reporte de Programas Instalados");
            contentStream.endText();

            int yPosition = 660;
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
            contentStream.newLineAtOffset(50, yPosition);

            for (Programa p : programas) {
                if (yPosition < 50) {
                    contentStream.endText();
                    contentStream.close();
                    
                    page = new PDPage();
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    
                    yPosition = 700;
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    contentStream.newLineAtOffset(50, yPosition);
                }
                String line = p.getNombre() + " | " + p.getVersion() + " | " + p.getPublisher();
                line = sanitizeForPdf(line);
                contentStream.showText(line);
                contentStream.newLineAtOffset(0, -15);
                yPosition -= 15;
            }
            contentStream.endText();
            contentStream.close();
            
            document.save(file);
            return true;
        }
    }

    // --- HISTORIAL ---

    public boolean exportarHistorial(File file, String format) {
        List<HistorialItem> eventos = HistorialService.getInstance().getEvents();
        try {
            switch (format.toUpperCase()) {
                case "TXT":
                    return writeHistorialTxt(file, eventos);
                case "CSV":
                    return writeHistorialCsv(file, eventos);
                case "PDF":
                    return writeHistorialPdf(file, eventos);
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean writeHistorialTxt(File file, List<HistorialItem> eventos) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            bw.write("--- REPORTE DE HISTORIAL ---\n\n");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (HistorialItem e : eventos) {
                bw.write("[" + e.getFechaHora().format(dtf) + "] " + e.getTipoEvento() + "\n");
                bw.write("Evento: " + e.getTipoEvento() + "\n");
                bw.write("Detalle: " + e.getDescripcion() + "\n");
                bw.write("----------------------------------------\n");
            }
            return true;
        }
    }

    private boolean writeHistorialCsv(File file, List<HistorialItem> eventos) throws IOException {
        try (BufferedWriter bw = createUtf8CsvWriter(file)) {
            bw.write("Fecha,Categoria,Evento,Descripcion\n");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (HistorialItem e : eventos) {
                bw.write(e.getFechaHora().format(dtf) + "," +
                         escapeCsv(e.getTipoEvento()) + "," +
                         escapeCsv(e.getTipoEvento()) + "," +
                         escapeCsv(e.getDescripcion()) + "\n");
            }
            return true;
        }
    }

    private boolean writeHistorialPdf(File file, List<HistorialItem> eventos) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
            contentStream.newLineAtOffset(50, 700);
            contentStream.showText("Reporte de Historial");
            contentStream.endText();

            int yPosition = 660;
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
            contentStream.newLineAtOffset(50, yPosition);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (HistorialItem e : eventos) {
                if (yPosition < 50) {
                    contentStream.endText();
                    contentStream.close();
                    
                    page = new PDPage();
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    
                    yPosition = 700;
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    contentStream.newLineAtOffset(50, yPosition);
                }
                String line = "[" + e.getFechaHora().format(dtf) + "] " + e.getTipoEvento();
                line = sanitizeForPdf(line);
                contentStream.showText(line);
                contentStream.newLineAtOffset(0, -15);
                yPosition -= 15;
            }
            contentStream.endText();
            contentStream.close();
            
            document.save(file);
            return true;
        }
    }

    // --- SISTEMA ---

    public boolean exportarSistema(File file, String format) {
        try {
            switch (format.toUpperCase()) {
                case "TXT":
                    return writeSistemaTxt(file);
                case "CSV":
                    return writeSistemaCsv(file);
                case "PDF":
                    return writeSistemaPdf(file);
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean writeSistemaTxt(File file) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            bw.write("--- REPORTE DEL SISTEMA (OptiScan Pro) ---\n\n");
            bw.write("Versión de OS: " + System.getProperty("os.name") + "\n");
            bw.write("Versión de Java: " + System.getProperty("java.version") + "\n");
            bw.write("Programas detectados: " + ProgramasService.getInstance().getProgramas().size() + "\n");
            bw.write("Eventos en historial: " + HistorialService.getInstance().getEvents().size() + "\n");
            return true;
        }
    }

    private boolean writeSistemaCsv(File file) throws IOException {
        try (BufferedWriter bw = createUtf8CsvWriter(file)) {
            bw.write("Atributo,Valor\n");
            bw.write("Versión de OS," + escapeCsv(System.getProperty("os.name")) + "\n");
            bw.write("Versión de Java," + escapeCsv(System.getProperty("java.version")) + "\n");
            bw.write("Programas detectados," + ProgramasService.getInstance().getProgramas().size() + "\n");
            bw.write("Eventos en historial," + HistorialService.getInstance().getEvents().size() + "\n");
            return true;
        }
    }

    private boolean writeSistemaPdf(File file) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Reporte del Sistema");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 660);
                contentStream.showText("Version de OS: " + sanitizeForPdf(System.getProperty("os.name")));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Version de Java: " + sanitizeForPdf(System.getProperty("java.version")));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Programas detectados: " + ProgramasService.getInstance().getProgramas().size());
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Eventos en historial: " + HistorialService.getInstance().getEvents().size());
                contentStream.endText();
            }
            document.save(file);
            return true;
        }
    }
}
