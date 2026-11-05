package com.unev.sige.web;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Añade un icono decorativo a cada enlace del menú web (oculto para lectores de pantalla). */
final class IconosWeb {
    private static final Pattern ENLACE = Pattern.compile("(<a\\b[^>]*\\bhref='(/[^']*)'[^>]*>)([^<]+)</a>");
    private static final Map<String, String> ICONOS = Map.ofEntries(
            Map.entry("/asignaturas", "📚"), Map.entry("/asistencia", "✅"), Map.entry("/asistencia-docente", "✅"),
            Map.entry("/auditoria", "🔍"), Map.entry("/aula", "🎓"), Map.entry("/boleta", "📄"),
            Map.entry("/calificaciones", "📊"), Map.entry("/cambiar-rol", "🔄"), Map.entry("/crear-profesor", "➕"),
            Map.entry("/curriculo", "🗂️"), Map.entry("/historial-sesiones", "🕘"), Map.entry("/inscripciones", "📝"),
            Map.entry("/materias", "📚"), Map.entry("/mis-hijos", "👪"), Map.entry("/periodos", "📅"),
            Map.entry("/reporte-asistencia", "📈"), Map.entry("/secciones", "🏫"), Map.entry("/solicitudes", "📨"),
            Map.entry("/usuarios", "👥"), Map.entry("/vinculaciones", "🔗"), Map.entry("/perfil", "👤"),
            Map.entry("/inicio", "🏠"));

    private IconosWeb() { }

    static String agregar(String html) {
        if (html == null) return null;
        Matcher m = ENLACE.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String icono = ICONOS.get(m.group(2));
            String reemplazo = icono == null ? m.group()
                    : m.group(1) + "<span class='ico' aria-hidden='true'>" + icono + "</span> " + m.group(3) + "</a>";
            m.appendReplacement(sb, Matcher.quoteReplacement(reemplazo));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
