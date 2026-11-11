package com.unev.sige.web;

import com.unev.sige.model.AcercaDe;

/** Botón «?» de la barra superior y página «Acerca de SIGE». */
public final class AcercaWeb {
    public static final String RUTA = "/acerca";

    private static final String ICONO = "<svg viewBox='0 0 24 24' width='20' height='20' aria-hidden='true' focusable='false'>"
            + "<circle cx='12' cy='12' r='9.5' fill='none' stroke='currentColor' stroke-width='2'/>"
            + "<path d='M9.2 9.3a2.9 2.9 0 1 1 4.2 2.6c-.9.5-1.4 1-1.4 2' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round'/>"
            + "<circle cx='12' cy='17.3' r='1.2' fill='currentColor'/></svg>";

    private AcercaWeb() { }

    /** Enlace con el icono de interrogación para la barra superior. */
    public static String boton() {
        return "<a class='ayuda-link' href='" + RUTA + "' aria-label='Acerca de SIGE' title='Acerca de SIGE'>" + ICONO + "</a>";
    }

    static String esc(String t) {
        return t == null ? "" : t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    /** Contenido de la página (sin la estructura general). */
    public static String cuerpo() {
        StringBuilder sb = new StringBuilder("<div class='page-heading'><div><div class='eyebrow'>ACERCA DE</div><h1>")
                .append(esc(AcercaDe.NOMBRE)).append(" · ").append(esc(AcercaDe.NOMBRE_COMPLETO))
                .append("</h1><p>").append(esc(AcercaDe.resumen())).append("</p></div></div>")
                .append("<p><span class='role-badge'>Versión ").append(esc(AcercaDe.VERSION)).append("</span></p>")
                .append("<div class='acerca-grid'>");
        for (AcercaDe.Seccion s : AcercaDe.secciones()) {
            sb.append("<section class='card'><h2>").append(esc(s.titulo())).append("</h2><ul>");
            for (String l : s.lineas()) sb.append("<li>").append(esc(l)).append("</li>");
            sb.append("</ul></section>");
        }
        sb.append("</div><section class='card'><h2>Código fuente</h2><p><a href='").append(esc(AcercaDe.REPOSITORIO))
          .append("' target='_blank' rel='noopener noreferrer'>").append(esc(AcercaDe.REPOSITORIO)).append("</a></p></section>");
        return sb.toString();
    }
}
