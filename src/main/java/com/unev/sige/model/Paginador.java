package com.unev.sige.model;

import java.text.Normalizer;
import java.util.Locale;

/** Calculos puros de paginacion y busqueda sin acentos, usados por las tablas de escritorio. */
public final class Paginador {

    private Paginador() {
    }

    /** Cantidad de paginas (minimo 1, aunque no haya filas). */
    public static int paginas(int total, int tamano) {
        int t = Math.max(1, tamano);
        return Math.max(1, (Math.max(0, total) + t - 1) / t);
    }

    /** Pagina valida (0 a paginas-1) mas cercana a la pedida. */
    public static int limitar(int pagina, int total, int tamano) {
        return Math.max(0, Math.min(pagina, paginas(total, tamano) - 1));
    }

    /** Indice inicial (incluido) de la pagina. */
    public static int desde(int pagina, int total, int tamano) {
        return Math.min(Math.max(0, total), limitar(pagina, total, tamano) * Math.max(1, tamano));
    }

    /** Indice final (excluido) de la pagina. */
    public static int hasta(int pagina, int total, int tamano) {
        return Math.min(Math.max(0, total), desde(pagina, total, tamano) + Math.max(1, tamano));
    }

    /** Minusculas, sin acentos y con espacios simples, para comparar textos al buscar. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinAcentos.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    /** Verdadero si cada palabra de la busqueda aparece en el texto (sin importar acentos ni mayusculas). */
    public static boolean coincide(String texto, String busqueda) {
        String q = normalizar(busqueda);
        if (q.isEmpty()) {
            return true;
        }
        String t = normalizar(texto);
        for (String palabra : q.split(" ")) {
            if (!t.contains(palabra)) {
                return false;
            }
        }
        return true;
    }
}
