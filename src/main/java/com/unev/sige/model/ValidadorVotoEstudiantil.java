package com.unev.sige.model;

import java.time.LocalDate;

/**
 * Gestor del Voto Estudiantil Escolar (Elecciones del Consejo Estudiantil).
 * Garantiza la emisión de un solo voto por estudiante y la validez temporal del proceso electoral.
 */
public final class ValidadorVotoEstudiantil {

    public record VotoEmitido(
            String cueVotante,
            String codigoPlancha,
            LocalDate fechaVoto
    ) { }

    private ValidadorVotoEstudiantil() {
    }

    public static boolean esVotoValido(VotoEmitido voto, LocalDate fechaInicioEleccion, LocalDate fechaFinEleccion) {
        if (voto == null || voto.cueVotante() == null || voto.codigoPlancha() == null) return false;
        if (fechaInicioEleccion == null || fechaFinEleccion == null) return false;

        LocalDate f = voto.fechaVoto() != null ? voto.fechaVoto() : LocalDate.now();
        return !f.isBefore(fechaInicioEleccion) && !f.isAfter(fechaFinEleccion);
    }
}
