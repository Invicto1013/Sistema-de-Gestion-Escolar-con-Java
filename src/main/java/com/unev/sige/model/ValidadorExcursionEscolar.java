package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador de Excursiones, Paseos Pedagógicos y Salidas de Campo Escolares.
 * Exige código de evento EXC-YYYY-XXX y verifica la tasa mínima de autorización parental para operar.
 */
public final class ValidadorExcursionEscolar {

    private static final Pattern PATRON_EXCURSION = Pattern.compile("^EXC-[0-9]{4}-[0-9]{3,5}$");

    public record EventoExcursion(
            String codigoExcursion,
            String destino,
            int totalEstudiantesConvocados,
            int autorizacionesFirmadas,
            boolean cuentaConSeguroAccidentes
    ) {
        public double porcentajeAutorizados() {
            return totalEstudiantesConvocados > 0 ? (autorizacionesFirmadas * 100.0) / totalEstudiantesConvocados : 0.0;
        }

        public boolean puedeRealizarse() {
            return cuentaConSeguroAccidentes && porcentajeAutorizados() >= 80.0;
        }
    }

    private ValidadorExcursionEscolar() {
    }

    public static boolean esCodigoExcursionValido(String codigo) {
        if (codigo == null) return false;
        return PATRON_EXCURSION.matcher(codigo.trim().toUpperCase()).matches();
    }
}
