package com.unev.sige.model;

import java.util.Collection;

/**
 * Validador de capacidad física y sobrecupo de estudiantes por sección escolar.
 * Asegura que no se inscriban estudiantes por encima del aforo máximo establecido
 * para cada aula/sección y clasifica el estado de disponibilidad de cupos.
 */
public final class ValidadorCapacidadSeccion {

    public static final int CAPACIDAD_DEFAULT = 30;

    public enum EstadoCupo {
        DISPONIBLE,
        ULTIMOS_CUPOS,
        LLENO,
        SOBRECUPO
    }

    public record EstadoCapacidad(
            int capacidadMaxima,
            int matriculados,
            int cuposDisponibles,
            EstadoCupo estado,
            boolean admiteInscripcion
    ) { }

    private ValidadorCapacidadSeccion() {
    }

    /**
     * Evalúa la capacidad de una sección dada su capacidad máxima y los alumnos inscritos actualmente.
     */
    public static EstadoCapacidad evaluar(int capacidadMaxima, int matriculadosActualmente) {
        int cap = capacidadMaxima > 0 ? capacidadMaxima : CAPACIDAD_DEFAULT;
        int mat = Math.max(0, matriculadosActualmente);
        int disponibles = Math.max(0, cap - mat);

        EstadoCupo estado;
        boolean admite;

        if (mat < cap) {
            if (disponibles <= 3) {
                estado = EstadoCupo.ULTIMOS_CUPOS;
            } else {
                estado = EstadoCupo.DISPONIBLE;
            }
            admite = true;
        } else if (mat == cap) {
            estado = EstadoCupo.LLENO;
            admite = false;
        } else {
            estado = EstadoCupo.SOBRECUPO;
            admite = false;
        }

        return new EstadoCapacidad(cap, mat, disponibles, estado, admite);
    }
}
