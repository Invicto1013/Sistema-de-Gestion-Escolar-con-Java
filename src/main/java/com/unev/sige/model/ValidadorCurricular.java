package com.unev.sige.model;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * Validador de prerrequisitos y progreso en la malla curricular (Plan de Estudio).
 * Asegura que un estudiante no se inscriba en una materia avanzada sin haber aprobado
 * sus prerrequisitos y valida la carga académica máxima autorizada por período.
 */
public final class ValidadorCurricular {

    /** Créditos o materias máximas por período escolar estándar. */
    public static final int MAX_MATERIAS_POR_PERIODO = 8;

    public record RequisitoMateria(
            String codigoMateria,
            String nombreMateria,
            int creditos,
            Set<String> prerrequisitos
    ) { }

    public record ResultadoValidacion(
            boolean esValida,
            String mensajeError
    ) {
        public static ResultadoValidacion ok() {
            return new ResultadoValidacion(true, null);
        }

        public static ResultadoValidacion error(String mensaje) {
            return new ResultadoValidacion(false, mensaje);
        }
    }

    private ValidadorCurricular() {
    }

    /**
     * Valida si un estudiante cumple los prerrequisitos para cursar una materia.
     * @param materia Materia a inscribir con sus prerrequisitos
     * @param codigosMateriasAprobadas Conjunto de códigos de asignaturas que el estudiante ya aprobó
     */
    public static ResultadoValidacion validarPrerrequisitos(RequisitoMateria materia, Set<String> codigosMateriasAprobadas) {
        if (materia == null) {
            return ResultadoValidacion.error("La materia no puede ser nula.");
        }

        if (materia.prerrequisitos() == null || materia.prerrequisitos().isEmpty()) {
            return ResultadoValidacion.ok();
        }

        if (codigosMateriasAprobadas == null) {
            codigosMateriasAprobadas = Set.of();
        }

        for (String req : materia.prerrequisitos()) {
            if (req != null && !codigosMateriasAprobadas.contains(req.trim().toUpperCase())) {
                return ResultadoValidacion.error(String.format(
                        "No cumple el prerrequisito %s para inscribir %s (%s).",
                        req, materia.codigoMateria(), materia.nombreMateria()
                ));
            }
        }

        return ResultadoValidacion.ok();
    }

    /**
     * Valida que la carga de materias solicitadas no exceda el límite reglamentario.
     */
    public static ResultadoValidacion validarCargaAcademica(int materiasActuales, int materiasNuevas) {
        int total = Math.max(0, materiasActuales) + Math.max(0, materiasNuevas);
        if (total > MAX_MATERIAS_POR_PERIODO) {
            return ResultadoValidacion.error(String.format(
                    "La carga académica excede el límite permitido de %d materias por período (solicitadas: %d).",
                    MAX_MATERIAS_POR_PERIODO, total
            ));
        }
        return ResultadoValidacion.ok();
    }
}
