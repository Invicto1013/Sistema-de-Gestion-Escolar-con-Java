package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador de la Red de Egresados y Registro de Exalumnos Destacados (Alumni).
 * Controla el código de egresado (ALM-YYYY-XXXX) y el seguimiento de inserción universitaria y laboral.
 */
public final class ValidadorRedEgresados {

    private static final Pattern PATRON_EGRESADO = Pattern.compile("^ALM-[0-9]{4}-[0-9]{4,6}$");

    public record FichaEgresado(
            String codigoEgresado,
            String nombreCompleto,
            int anioGraduacion,
            String universidadDestino,
            String carreraOProfesion
    ) { }

    private ValidadorRedEgresados() {
    }

    public static boolean esCodigoEgresadoValido(String codigo) {
        if (codigo == null) return false;
        return PATRON_EGRESADO.matcher(codigo.trim().toUpperCase()).matches();
    }
}
