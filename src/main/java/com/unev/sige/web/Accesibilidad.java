package com.unev.sige.web;

/**
 * Mejoras de accesibilidad (WCAG 2.1 AA) aplicadas al HTML que genera SIGE Web, en un solo lugar:
 * así todas las pantallas las reciben sin editar cada formulario o tabla.
 */
final class Accesibilidad {
    private Accesibilidad() { }

    /** Marca con aria-current="page" el enlace del menú que apunta a la página actual. */
    static String marcarPaginaActual(String nav, String ruta) {
        if (nav == null || ruta == null || ruta.isBlank()) return nav;
        String enlace = "<a href='" + ruta + "'>";
        if (!nav.contains(enlace)) return nav;
        return nav.replace(enlace, "<a href='" + ruta + "' aria-current='page'>");
    }

    /**
     * Cabeceras de columna con scope="col" (los lectores de pantalla relacionan cada celda con su
     * encabezado) y contenedores de tablas con desplazamiento enfocables con teclado.
     */
    static String mejorar(String html) {
        if (html == null) return null;
        return html.replace("<th>", "<th scope='col'>")
                .replace("<div class='table-wrap'>",
                        "<div class='table-wrap' role='region' aria-label='Tabla de datos, desplazable' tabindex='0'>");
    }
}
