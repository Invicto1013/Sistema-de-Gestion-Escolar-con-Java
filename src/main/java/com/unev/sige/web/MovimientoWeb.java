package com.unev.sige.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Preferencia de movimiento: por defecto se respeta la opción del sistema («reducir animaciones»);
 * con «siempre» se muestran las animaciones decorativas aunque el sistema pida reducirlas. Se guarda en una cookie.
 */
public final class MovimientoWeb {
    public static final String COOKIE = "sige_mov";
    public static final String PREDETERMINADO = "sistema";
    private static final String[] MODOS = {"sistema", "siempre"};
    private static final String[] NOMBRES = {"Según el sistema", "Siempre activas"};

    private MovimientoWeb() { }

    public static boolean valido(String modo) {
        for (String m : MODOS) if (m.equals(modo)) return true;
        return false;
    }

    public static String modo(String cabeceraCookie) {
        if (cabeceraCookie == null) return PREDETERMINADO;
        for (String par : cabeceraCookie.split(";")) {
            String p = par.trim();
            if (p.startsWith(COOKIE + "=")) {
                String v = p.substring(COOKIE.length() + 1).trim();
                return valido(v) ? v : PREDETERMINADO;
            }
        }
        return PREDETERMINADO;
    }

    public static String atributo(String modo) {
        return "siempre".equals(modo) ? " data-mov='siempre'" : "";
    }

    /** Sección del menú de apariencia para elegir cuándo se animan los elementos decorativos. */
    public static String seccionMenu(String modoActual, String rutaActual) {
        String volver = URLEncoder.encode(VisionWeb.volverSeguro(rutaActual), StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder("<strong>Animaciones</strong>");
        for (int i = 0; i < MODOS.length; i++) {
            sb.append("<a href='/movimiento?modo=").append(MODOS[i]).append("&amp;volver=").append(volver).append('\'')
              .append(MODOS[i].equals(modoActual) ? " aria-current='true'" : "").append('>').append(NOMBRES[i]).append("</a>");
        }
        return sb.toString();
    }
}
