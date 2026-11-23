package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador de Carnets y Rutas de Transporte Escolar.
 * Controla la asignación de paradas, números de autobús escolar y contactos del chofer/monitor.
 */
public final class ValidadorTransporteEscolar {

    private static final Pattern PATRON_RUTA = Pattern.compile("^RUTA-[0-9]{2,3}$");

    public record AsignacionTransporte(
            String codigoRuta,
            int numeroAutobus,
            String paradaAsignada,
            String turno,
            boolean autorizacionPadresFirmada
    ) { }

    private ValidadorTransporteEscolar() {
    }

    public static boolean esCodigoRutaValido(String codigo) {
        if (codigo == null) return false;
        return PATRON_RUTA.matcher(codigo.trim().toUpperCase()).matches();
    }

    public static boolean esAsignacionValida(AsignacionTransporte asignacion) {
        if (asignacion == null) return false;
        return esCodigoRutaValido(asignacion.codigoRuta())
                && asignacion.numeroAutobus() > 0
                && asignacion.paradaAsignada() != null && !asignacion.paradaAsignada().isBlank()
                && asignacion.autorizacionPadresFirmada();
    }
}
