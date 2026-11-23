package com.unev.sige.model;

import java.time.LocalDate;
import java.time.Period;

/**
 * Validador de edad reglamentaria según nivel académico (MINERD / Educación Dominicana):
 * - Nivel Inicial / Preescolar: 3 a 5 años.
 * - Primaria: 6 a 11 años.
 * - Secundaria: 12 a 18 años.
 */
public final class ValidadorEdadEstudiante {

    public enum NivelEducativo {
        INICIAL(3, 5),
        PRIMARIA(6, 11),
        SECUNDARIA(12, 18);

        private final int edadMinima;
        private final int edadMaxima;

        NivelEducativo(int edadMinima, int edadMaxima) {
            this.edadMinima = edadMinima;
            this.edadMaxima = edadMaxima;
        }

        public int getEdadMinima() {
            return edadMinima;
        }

        public int getEdadMaxima() {
            return edadMaxima;
        }
    }

    public record EvaluacionEdad(
            int edadAnios,
            boolean edadAdecuada,
            String observacion
    ) { }

    private ValidadorEdadEstudiante() {
    }

    /**
     * Calcula la edad cumplida a una fecha de referencia.
     */
    public static int calcularEdad(LocalDate fechaNacimiento, LocalDate fechaReferencia) {
        if (fechaNacimiento == null) return 0;
        if (fechaReferencia == null) fechaReferencia = LocalDate.now();
        if (fechaNacimiento.isAfter(fechaReferencia)) return 0;
        return Period.between(fechaNacimiento, fechaReferencia).getYears();
    }

    /**
     * Evalúa si un estudiante tiene la edad idónea para el nivel escolar matriculado.
     */
    public static EvaluacionEdad evaluarNivel(LocalDate fechaNacimiento, NivelEducativo nivel, LocalDate fechaCorte) {
        if (fechaNacimiento == null || nivel == null) {
            return new EvaluacionEdad(0, false, "Fecha de nacimiento o nivel inválido.");
        }

        int edad = calcularEdad(fechaNacimiento, fechaCorte);
        if (edad < nivel.getEdadMinima()) {
            return new EvaluacionEdad(edad, false, String.format(
                    "Menor a la edad requerida para %s (edad: %d años, mínimo: %d años).",
                    nivel.name(), edad, nivel.getEdadMinima()
            ));
        }

        if (edad > nivel.getEdadMaxima()) {
            return new EvaluacionEdad(edad, false, String.format(
                    "Sobreedad para nivel regular %s (edad: %d años, máximo: %d años).",
                    nivel.name(), edad, nivel.getEdadMaxima()
            ));
        }

        return new EvaluacionEdad(edad, true, "Edad adecuada para el nivel escolar.");
    }
}
