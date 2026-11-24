package com.unev.sige.model;

import java.util.Map;

/** Icono (emoji) de cada herramienta y de cada grupo, y el color de acento de cada grupo. Compartido por web y escritorio. */
public final class IconosHerramientas {

    private static final Map<String, String> POR_HERRAMIENTA = Map.ofEntries(
            Map.entry("aptitud-fisica", "🏃"), Map.entry("promedio-grupal", "📊"), Map.entry("prueba-diagnostica", "🧪"),
            Map.entry("riesgo-desercion", "⚠️"), Map.entry("creditos", "🎓"), Map.entry("proyecto-integrador", "🧩"),
            Map.entry("plan-refuerzo", "🛟"), Map.entry("feria-cientifica", "🔬"), Map.entry("calendario", "📅"),
            Map.entry("carga-docente", "⚖️"), Map.entry("evaluacion-docente", "⭐"), Map.entry("titulo-docente", "📜"),
            Map.entry("reunion-consejo", "🏛️"), Map.entry("excusa", "🩺"), Map.entry("comedor", "🍽️"),
            Map.entry("retiro-estudiante", "🚪"), Map.entry("permiso-permanencia", "⏰"), Map.entry("transporte", "🚌"),
            Map.entry("enfermeria-codigo", "🏥"), Map.entry("excursion-codigo", "🧭"), Map.entry("citacion-codigo", "📨"),
            Map.entry("finanzas", "💰"), Map.entry("inventario", "📦"), Map.entry("banco-libros", "📚"),
            Map.entry("prestamo-dispositivo", "💻"), Map.entry("transferencia", "🔄"), Map.entry("admision-codigo", "📝"),
            Map.entry("escuela-padres", "👪"), Map.entry("club", "⚽"), Map.entry("voto-estudiantil", "🗳️"),
            Map.entry("egresado-codigo", "🎖️"), Map.entry("publicacion-codigo", "📰"), Map.entry("carnet", "🪪"),
            Map.entry("casillero", "🔐"), Map.entry("firma-digital", "✍️"), Map.entry("laboratorio-mac", "🖥️"));

    private static final Map<String, String> ICONO_GRUPO = Map.of(
            "Académico", "📘", "Docentes", "👩‍🏫", "Bienestar y seguridad", "🛡️",
            "Administración", "🗂️", "Familia y comunidad", "🏡", "Documentos y códigos", "🔖");

    private static final Map<String, String> ACENTO_GRUPO = Map.of(
            "Académico", "#2563eb", "Docentes", "#7c3aed", "Bienestar y seguridad", "#e11d48",
            "Administración", "#d97706", "Familia y comunidad", "#0f766e", "Documentos y códigos", "#475569");

    private IconosHerramientas() { }

    public static String icono(String idHerramienta) {
        return POR_HERRAMIENTA.getOrDefault(idHerramienta, "🧰");
    }

    public static String iconoGrupo(String grupo) {
        return ICONO_GRUPO.getOrDefault(grupo, "🧰");
    }

    /** Color hexadecimal (#RRGGBB) del grupo. */
    public static String acento(String grupo) {
        return ACENTO_GRUPO.getOrDefault(grupo, "#0f766e");
    }
}
