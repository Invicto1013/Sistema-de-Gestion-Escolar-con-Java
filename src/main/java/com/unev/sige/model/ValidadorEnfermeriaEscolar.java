package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador de Números de Emergencia del Botiquín y Registro de Incidentes de Enfermería.
 * Registra atenciones de primeros auxilios y control de medicamentos administrados en el colegio.
 */
public final class ValidadorEnfermeriaEscolar {

    private static final Pattern PATRON_ATENCION = Pattern.compile("^ENF-[0-9]{4}-[0-9]{4,6}$");

    public record RegistroAtencion(
            String codigoAtencion,
            String matriculaEstudiante,
            String sintomaPrincipal,
            String medicamentoSuministrado,
            boolean notificadoATutores
    ) { }

    private ValidadorEnfermeriaEscolar() {
    }

    public static boolean esCodigoAtencionValido(String codigo) {
        if (codigo == null) return false;
        return PATRON_ATENCION.matcher(codigo.trim().toUpperCase()).matches();
    }
}
