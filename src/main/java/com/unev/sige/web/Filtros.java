package com.unev.sige.web;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

/**
 * Utilidades de filtrado para las pantallas de listado: búsqueda sin distinguir
 * mayúsculas ni acentos, validación de valores recibidos por la URL y la barra
 * de filtros en HTML (formulario GET, de modo que la búsqueda se pueda compartir).
 */
public final class Filtros {
    private Filtros() {}

    /** Minúsculas y sin acentos: "Matemática" y "matematica" coinciden. */
    public static String normalizar(String valor) {
        if (valor == null) return "";
        String sinAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinAcentos.toLowerCase(Locale.ROOT).trim();
    }

    /** Todas las palabras de la búsqueda deben aparecer en alguno de los campos. Búsqueda vacía = coincide. */
    public static boolean coincide(String busqueda, String... campos) {
        String q = normalizar(busqueda);
        if (q.isEmpty()) return true;
        StringBuilder pajar = new StringBuilder();
        for (String c : campos) pajar.append(normalizar(c)).append(' ');
        String texto = pajar.toString();
        for (String palabra : q.split("\\s+")) if (!texto.contains(palabra)) return false;
        return true;
    }

    /** Texto de búsqueda limpio: nunca nulo y con longitud máxima. */
    public static String texto(String valor, int max) {
        if (valor == null) return "";
        String v = valor.trim();
        return v.length() > max ? v.substring(0, max) : v;
    }

    /** Devuelve el valor solo si está entre los permitidos; si no, cadena vacía (sin filtro). */
    public static String opcion(String valor, Collection<String> permitidos) {
        return valor != null && permitidos.contains(valor) ? valor : "";
    }

    /** Entero positivo opcional del filtro; 0 significa "sin filtro". */
    public static int entero(String valor) {
        try { int n = Integer.parseInt(valor == null ? "" : valor.trim()); return Math.max(n, 0); }
        catch (NumberFormatException e) { return 0; }
    }

    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static String campoTexto(String nombre, String etiqueta, String valor, String ayuda) {
        return "<label>" + esc(etiqueta) + "<input type='search' name='" + esc(nombre) + "' maxlength='100' value='" + esc(valor)
                + "' placeholder='" + esc(ayuda) + "'></label>";
    }

    /** Lista desplegable con la opción "Todos" al inicio. {@code opciones}: valor → texto. */
    public static String campoLista(String nombre, String etiqueta, String valor, Map<String, String> opciones) {
        StringBuilder sb = new StringBuilder("<label>").append(esc(etiqueta)).append("<select name='").append(esc(nombre)).append("'>")
                .append("<option value=''>Todos</option>");
        opciones.forEach((v, t) -> sb.append("<option value='").append(esc(v)).append("'").append(v.equals(valor) ? " selected" : "")
                .append(">").append(esc(t)).append("</option>"));
        return sb.append("</select></label>").toString();
    }

    /**
     * Barra de filtros. {@code ocultos}: campos que deben conservarse (p. ej. la sección elegida).
     * Muestra "Mostrando X de Y" y un enlace para limpiar cuando hay filtros activos.
     */
    public static String barra(String accion, Map<String, String> ocultos, String campos, boolean hayFiltros, int mostrados, int total) {
        StringBuilder sb = new StringBuilder("<section class='card section-picker filtros' aria-label='Filtros de búsqueda'>")
                .append("<form method='get' action='").append(esc(accion)).append("' class='subject-fields' role='search'>");
        ocultos.forEach((k, v) -> sb.append("<input type='hidden' name='").append(esc(k)).append("' value='").append(esc(v)).append("'>"));
        sb.append(campos).append("<button class='primary' type='submit'>Filtrar</button>");
        if (hayFiltros) {
            sb.append("<a class='secondary-link' href='").append(esc(accion));
            if (!ocultos.isEmpty()) {
                StringBuilder qs = new StringBuilder();
                ocultos.forEach((k, v) -> qs.append(qs.length() == 0 ? '?' : '&').append(k).append('=').append(java.net.URLEncoder.encode(v, java.nio.charset.StandardCharsets.UTF_8)));
                sb.append(esc(qs.toString()));
            }
            sb.append("'>Limpiar filtros</a>");
        }
        sb.append("</form><p class='filtros-resumen muted' role='status'>").append(resumen(mostrados, total, hayFiltros)).append("</p></section>");
        return sb.toString();
    }

    public static String resumen(int mostrados, int total, boolean hayFiltros) {
        if (!hayFiltros) return "Mostrando " + total + (total == 1 ? " registro." : " registros.");
        return "Mostrando " + mostrados + " de " + total + (total == 1 ? " registro." : " registros.");
    }
}
