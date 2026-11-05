package com.unev.sige.app;

import javafx.collections.ObservableList;

import java.util.prefs.Preferences;

/** Tema de color del escritorio (claro u oscuro). La elección se recuerda entre sesiones. */
public enum TemaApp {
    CLARO("Claro"),
    OSCURO("Oscuro");

    private static final String CLAVE = "tema.color";
    private static final String HOJA_OSCURA = "/com/unev/sige/view/tema-oscuro.css";

    private final String etiqueta;

    TemaApp(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public TemaApp alternar() {
        return this == CLARO ? OSCURO : CLARO;
    }

    public static TemaApp actual() {
        try {
            return valueOf(Preferences.userNodeForPackage(TemaApp.class).get(CLAVE, CLARO.name()));
        } catch (RuntimeException e) {
            return CLARO;
        }
    }

    public static void guardar(TemaApp tema) {
        try {
            Preferences.userNodeForPackage(TemaApp.class).put(CLAVE, tema.name());
        } catch (RuntimeException ignorada) {
            // si no se puede guardar, el tema sigue activo hasta cerrar la aplicación
        }
    }

    /** Quita la hoja oscura y la añade al final solo si el tema actual es oscuro. */
    public static void aplicar(ObservableList<String> hojas) {
        String url = TemaApp.class.getResource(HOJA_OSCURA).toExternalForm();
        hojas.remove(url);
        if (actual() == OSCURO) hojas.add(url);
    }
}
