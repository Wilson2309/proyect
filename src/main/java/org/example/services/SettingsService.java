package org.example.services;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class SettingsService {

    private static final String SETTINGS_FILE = resolveSettingsFile();
    private static final SettingsService INSTANCE = new SettingsService();

    private final BooleanProperty animacionesActivas = new SimpleBooleanProperty(true);
    private final BooleanProperty modoOscuro = new SimpleBooleanProperty(false);
    private final StringProperty densidadUi = new SimpleStringProperty("Normal");
    private final BooleanProperty escaneoAutomatico = new SimpleBooleanProperty(true);
    private final BooleanProperty incluirControladores = new SimpleBooleanProperty(false);
    private final BooleanProperty actualizacionAutomatica = new SimpleBooleanProperty(true);
    private final BooleanProperty mensajesSistema = new SimpleBooleanProperty(true);
    private final BooleanProperty alertasVisuales = new SimpleBooleanProperty(true);

    private SettingsService() {
        loadSettings();
        setupAutoSave();
    }

    public static SettingsService getInstance() {
        return INSTANCE;
    }

    private static String resolveSettingsFile() {
        String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) {
            home = System.getProperty("java.io.tmpdir", ".");
        }
        return home + File.separator + "optiscan_settings.properties";
    }

    private void loadSettings() {
        try {
            File file = new File(SETTINGS_FILE);
            if (file.exists()) {
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream(file)) {
                    props.load(fis);
                    animacionesActivas.set(Boolean.parseBoolean(props.getProperty("animacionesActivas", "true")));
                    modoOscuro.set(Boolean.parseBoolean(props.getProperty("modoOscuro", "false")));
                    densidadUi.set(props.getProperty("densidadUi", "Normal"));
                    escaneoAutomatico.set(Boolean.parseBoolean(props.getProperty("escaneoAutomatico", "true")));
                    incluirControladores.set(Boolean.parseBoolean(props.getProperty("incluirControladores", "false")));
                    actualizacionAutomatica.set(Boolean.parseBoolean(props.getProperty("actualizacionAutomatica", "true")));
                    mensajesSistema.set(Boolean.parseBoolean(props.getProperty("mensajesSistema", "true")));
                    alertasVisuales.set(Boolean.parseBoolean(props.getProperty("alertasVisuales", "true")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveSettings() {
        try {
            Properties props = new Properties();
            props.setProperty("animacionesActivas", String.valueOf(animacionesActivas.get()));
            props.setProperty("modoOscuro", String.valueOf(modoOscuro.get()));
            props.setProperty("densidadUi", densidadUi.get());
            props.setProperty("escaneoAutomatico", String.valueOf(escaneoAutomatico.get()));
            props.setProperty("incluirControladores", String.valueOf(incluirControladores.get()));
            props.setProperty("actualizacionAutomatica", String.valueOf(actualizacionAutomatica.get()));
            props.setProperty("mensajesSistema", String.valueOf(mensajesSistema.get()));
            props.setProperty("alertasVisuales", String.valueOf(alertasVisuales.get()));

            try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
                props.store(fos, "OptiScan Pro Settings");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupAutoSave() {
        animacionesActivas.addListener((obs, oldVal, newVal) -> saveSettings());
        modoOscuro.addListener((obs, oldVal, newVal) -> saveSettings());
        densidadUi.addListener((obs, oldVal, newVal) -> saveSettings());
        escaneoAutomatico.addListener((obs, oldVal, newVal) -> saveSettings());
        incluirControladores.addListener((obs, oldVal, newVal) -> saveSettings());
        actualizacionAutomatica.addListener((obs, oldVal, newVal) -> saveSettings());
        mensajesSistema.addListener((obs, oldVal, newVal) -> saveSettings());
        alertasVisuales.addListener((obs, oldVal, newVal) -> saveSettings());
    }

    // Getters for Properties
    public BooleanProperty animacionesActivasProperty() { return animacionesActivas; }
    public BooleanProperty modoOscuroProperty() { return modoOscuro; }
    public StringProperty densidadUiProperty() { return densidadUi; }
    public BooleanProperty escaneoAutomaticoProperty() { return escaneoAutomatico; }
    public BooleanProperty incluirControladoresProperty() { return incluirControladores; }
    public BooleanProperty actualizacionAutomaticaProperty() { return actualizacionAutomatica; }
    public BooleanProperty mensajesSistemaProperty() { return mensajesSistema; }
    public BooleanProperty alertasVisualesProperty() { return alertasVisuales; }

    // Convenience getters/setters
    public boolean isAnimacionesActivas() { return animacionesActivas.get(); }
    public void setAnimacionesActivas(boolean val) { animacionesActivas.set(val); }

    public boolean isModoOscuro() { return modoOscuro.get(); }
    public void setModoOscuro(boolean val) { modoOscuro.set(val); }

    public String getDensidadUi() { return densidadUi.get(); }
    public void setDensidadUi(String val) { densidadUi.set(val); }

    public boolean isEscaneoAutomatico() { return escaneoAutomatico.get(); }
    public void setEscaneoAutomatico(boolean val) { escaneoAutomatico.set(val); }

    public boolean isIncluirControladores() { return incluirControladores.get(); }
    public void setIncluirControladores(boolean val) { incluirControladores.set(val); }

    public boolean isActualizacionAutomatica() { return actualizacionAutomatica.get(); }
    public void setActualizacionAutomatica(boolean val) { actualizacionAutomatica.set(val); }

    public boolean isMensajesSistema() { return mensajesSistema.get(); }
    public void setMensajesSistema(boolean val) { mensajesSistema.set(val); }

    public boolean isAlertasVisuales() { return alertasVisuales.get(); }
    public void setAlertasVisuales(boolean val) { alertasVisuales.set(val); }
}
