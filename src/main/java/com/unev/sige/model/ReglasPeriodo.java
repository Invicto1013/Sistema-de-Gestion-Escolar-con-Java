package com.unev.sige.model;

import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reglas de periodos lectivos, cierre de notas y auditoria, TODAS en un solo lugar y sin acceso a la base
 * de datos, para poder probarlas con JUnit. Los DAO las aplican y MySQL las refuerza (restricciones CHECK,
 * claves unicas y bloqueos de fila).
 */
public final class ReglasPeriodo {

    public static final int MOTIVO_MINIMO = 10;
    public static final int MOTIVO_MAXIMO = 300;
    public static final int DETALLE_MAXIMO = 1000;

    /** Año y número de periodo: 2026-1, 2026-2, 2026-V... */
    private static final Pattern NOMBRE = Pattern.compile("[0-9]{4}-[0-9A-Za-z]{1,8}");

    private ReglasPeriodo() {
    }

    /** Devuelve el nombre sin espacios sobrantes o lanza IllegalArgumentException con un mensaje listo para mostrar. */
    public static String validarNombre(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim();
        if (!NOMBRE.matcher(limpio).matches()) {
            throw new IllegalArgumentException("El nombre del período debe tener el formato AAAA-N (por ejemplo 2026-2).");
        }
        return limpio;
    }

    public static void validarFechas(LocalDate inicio, LocalDate fin) {
        if (inicio != null && fin != null && inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin.");
        }
    }

    /** El motivo es obligatorio en cierres, reaperturas y correcciones excepcionales. */
    public static String validarMotivo(String motivo) {
        String limpio = motivo == null ? "" : motivo.trim().replaceAll("\\s+", " ");
        if (limpio.length() < MOTIVO_MINIMO) {
            throw new IllegalArgumentException("Escribe el motivo (al menos " + MOTIVO_MINIMO + " caracteres).");
        }
        if (limpio.length() > MOTIVO_MAXIMO) {
            throw new IllegalArgumentException("El motivo no puede superar " + MOTIVO_MAXIMO + " caracteres.");
        }
        return limpio;
    }

    /** Registro normal de notas: solo con el periodo abierto y solo profesores (de su seccion) o administradores. */
    public static boolean puedeEditarNotas(Rol rol, EstadoPeriodo estado) {
        return estado == EstadoPeriodo.ABIERTO && (rol == Rol.PROFESOR || rol == Rol.ADMIN);
    }

    /** Correccion excepcional: solo un administrador y solo cuando el periodo ya esta cerrado. */
    public static boolean puedeCorregir(Rol rol, EstadoPeriodo estado) {
        return estado == EstadoPeriodo.CERRADO && rol == Rol.ADMIN;
    }

    /** Resultado de un estudiante en un periodo a partir del estado de sus materias. */
    public static String resultado(int total, int aprobadas, int reprobadas) {
        if (total <= 0 || aprobadas + reprobadas < total) {
            return "INCOMPLETO";
        }
        return reprobadas > 0 ? "REPROBADO" : "APROBADO";
    }

    /** Hay algo que auditar: no existia fila de notas o alguna de las notas guardadas cambio. */
    public static boolean hayCambios(Calificacion antes, Calificacion despues) {
        return antes == null
                || Double.compare(antes.getNotaParcial1(), despues.getNotaParcial1()) != 0
                || Double.compare(antes.getNotaParcial2(), despues.getNotaParcial2()) != 0
                || Double.compare(antes.getNotaExamenFinal(), despues.getNotaExamenFinal()) != 0;
    }

    /** Texto para la auditoria: que notas cambiaron y de que valor a cual. */
    public static String describirCambio(Calificacion antes, Calificacion despues) {
        StringBuilder sb = new StringBuilder();
        agregar(sb, "Parcial 1", antes == null ? null : antes.getNotaParcial1(), despues.getNotaParcial1());
        agregar(sb, "Parcial 2", antes == null ? null : antes.getNotaParcial2(), despues.getNotaParcial2());
        agregar(sb, "Examen", antes == null ? null : antes.getNotaExamenFinal(), despues.getNotaExamenFinal());
        agregar(sb, "Final", antes == null ? null : antes.getNotaFinal(), despues.getNotaFinal());
        if (sb.length() == 0) {
            return "Sin cambios en las notas (final " + formato(despues.getNotaFinal()) + ").";
        }
        return sb.toString();
    }

    private static void agregar(StringBuilder sb, String nombre, Double antes, double despues) {
        if (antes != null && Double.compare(antes, despues) == 0) {
            return;
        }
        if (sb.length() > 0) {
            sb.append("; ");
        }
        sb.append(nombre).append(' ').append(antes == null ? "-" : formato(antes)).append(" -> ").append(formato(despues));
    }

    private static String formato(double valor) {
        return String.format(Locale.ROOT, "%.2f", valor);
    }

    /** Recorta un texto al maximo de la columna para que la auditoria nunca falle por longitud. */
    public static String recortar(String texto, int maximo) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 3) + "...";
    }
}
