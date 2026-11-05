package com.unev.sige.app;

import javafx.collections.ObservableList;

import java.util.prefs.Preferences;

/**
 * Modos de visión de la aplicación de escritorio: normal, daltonismo (azul y naranja, paleta Okabe-Ito)
 * y alto contraste. La elección se recuerda entre sesiones y solo guarda el nombre del modo.
 */
public enum ModoVision {
    NORMAL("Normal", null),
    DALTONISMO("Daltonismo", "modo-daltonismo.css"),
    ALTO_CONTRASTE("Alto contraste", "modo-alto-contraste.css");

    private static final String CLAVE = "modo.vision";
    private static final String RUTA_VISTAS = "/com/unev/sige/view/";

    private final String etiqueta;
    private final String hoja;

    ModoVision(String etiqueta, String hoja) {
        this.etiqueta = etiqueta;
        this.hoja = hoja;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public ModoVision siguiente() {
        ModoVision[] todos = values();
        return todos[(ordinal() + 1) % todos.length];
    }

    public static ModoVision actual() {
        try {
            return valueOf(Preferences.userNodeForPackage(ModoVision.class).get(CLAVE, NORMAL.name()));
        } catch (RuntimeException e) {
            return NORMAL;
        }
    }

    public static void guardar(ModoVision modo) {
        try {
            Preferences.userNodeForPackage(ModoVision.class).put(CLAVE, modo.name());
        } catch (RuntimeException ignorada) {
            // si no se puede guardar, el modo sigue activo hasta cerrar la aplicación
        }
    }

    /** Quita las hojas de los modos y añade la del modo actual al final, para que prevalezca sobre styles.css. */
    public static void aplicar(ObservableList<String> hojas) {
        for (ModoVision m : values()) {
            if (m.hoja != null) hojas.remove(url(m));
        }
        ModoVision actual = actual();
        if (actual.hoja != null) hojas.add(url(actual));
    }

    private static String url(ModoVision m) {
        return ModoVision.class.getResource(RUTA_VISTAS + m.hoja).toExternalForm();
    }
}
