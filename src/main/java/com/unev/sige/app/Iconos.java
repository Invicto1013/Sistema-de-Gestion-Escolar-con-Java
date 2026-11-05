package com.unev.sige.app;

import javafx.scene.control.Label;

import java.util.Map;

/** Iconos del menú lateral del escritorio (texto Unicode, sin archivos de imagen). */
public final class Iconos {
    private static final Map<String, String> POR_TEXTO = Map.ofEntries(
            Map.entry("Inicio", "🏠"), Map.entry("Mi perfil", "👤"), Map.entry("Usuarios", "👥"),
            Map.entry("Historial de sesiones", "🕘"), Map.entry("Crear profesor", "➕"),
            Map.entry("Asignaturas", "📚"), Map.entry("Grados y currículo", "🗂"), Map.entry("Períodos", "📅"),
            Map.entry("Auditoría", "🔍"), Map.entry("Secciones", "🏫"), Map.entry("Inscripciones", "📝"),
            Map.entry("Calificaciones", "📊"), Map.entry("Vincular padres", "🔗"), Map.entry("Boletas", "📄"),
            Map.entry("Reporte de inscripciones", "📈"), Map.entry("Mis secciones", "🏫"),
            Map.entry("Asistencia", "✅"), Map.entry("Mis materias", "📚"), Map.entry("Mis calificaciones", "📊"),
            Map.entry("Boleta", "📄"), Map.entry("Mi asistencia", "✅"), Map.entry("Mis hijos", "👪"));

    private Iconos() {
    }

    /** Símbolo para una opción de menú; "•" si no hay uno específico. */
    public static String para(String texto) {
        return POR_TEXTO.getOrDefault(texto, "•");
    }

    public static Label etiqueta(String texto) {
        Label icono = new Label(para(texto));
        icono.getStyleClass().add("sidebar-icon");
        icono.setFocusTraversable(false);
        return icono;
    }
}
