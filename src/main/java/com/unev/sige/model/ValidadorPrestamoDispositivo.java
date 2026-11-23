package com.unev.sige.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Control y Préstamo de Recursos Tecnológicos y Laboratorios (Laptops, Tablets, Proyectores).
 * Supervisa el tiempo de devolución y aplica sanciones por mora en la entrega de dispositivos.
 */
public final class ValidadorPrestamoDispositivo {

    public static final int PLAZO_MAXIMO_PRESTAMO_DIAS = 7;

    public record PrestamoDispositivo(
            String serialDispositivo,
            String tipoDispositivo,
            LocalDate fechaPrestamo,
            LocalDate fechaDevolucionEsperada
    ) { }

    private ValidadorPrestamoDispositivo() {
    }

    public static boolean estaEnMora(LocalDate fechaDevolucionEsperada, LocalDate fechaActual) {
        if (fechaDevolucionEsperada == null) return false;
        LocalDate hoy = fechaActual != null ? fechaActual : LocalDate.now();
        return hoy.isAfter(fechaDevolucionEsperada);
    }

    public static long diasRetraso(LocalDate fechaDevolucionEsperada, LocalDate fechaActual) {
        if (fechaDevolucionEsperada == null) return 0;
        LocalDate hoy = fechaActual != null ? fechaActual : LocalDate.now();
        if (!hoy.isAfter(fechaDevolucionEsperada)) return 0;
        return ChronoUnit.DAYS.between(fechaDevolucionEsperada, hoy);
    }
}
