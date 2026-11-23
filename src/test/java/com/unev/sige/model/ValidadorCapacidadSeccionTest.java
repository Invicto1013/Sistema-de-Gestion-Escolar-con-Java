package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorCapacidadSeccionTest {

    @Test
    @DisplayName("Sección con cupos disponibles admite inscripción")
    void evaluar_conCupos_admiteInscripcion() {
        ValidadorCapacidadSeccion.EstadoCapacidad est = ValidadorCapacidadSeccion.evaluar(35, 20);

        assertEquals(35, est.capacidadMaxima());
        assertEquals(20, est.matriculados());
        assertEquals(15, est.cuposDisponibles());
        assertEquals(ValidadorCapacidadSeccion.EstadoCupo.DISPONIBLE, est.estado());
        assertTrue(est.admiteInscripcion());
    }

    @Test
    @DisplayName("Sección en últimos cupos (<= 3)")
    void evaluar_ultimosCupos() {
        ValidadorCapacidadSeccion.EstadoCapacidad est = ValidadorCapacidadSeccion.evaluar(30, 28);

        assertEquals(2, est.cuposDisponibles());
        assertEquals(ValidadorCapacidadSeccion.EstadoCupo.ULTIMOS_CUPOS, est.estado());
        assertTrue(est.admiteInscripcion());
    }

    @Test
    @DisplayName("Sección llena rechaza nuevas inscripciones")
    void evaluar_seccionLlena_rechaza() {
        ValidadorCapacidadSeccion.EstadoCapacidad est = ValidadorCapacidadSeccion.evaluar(30, 30);

        assertEquals(0, est.cuposDisponibles());
        assertEquals(ValidadorCapacidadSeccion.EstadoCupo.LLENO, est.estado());
        assertFalse(est.admiteInscripcion());
    }

    @Test
    @DisplayName("Sección con sobrecupo reporta estado crítico")
    void evaluar_sobrecupo_rechaza() {
        ValidadorCapacidadSeccion.EstadoCapacidad est = ValidadorCapacidadSeccion.evaluar(30, 32);

        assertEquals(0, est.cuposDisponibles());
        assertEquals(ValidadorCapacidadSeccion.EstadoCupo.SOBRECUPO, est.estado());
        assertFalse(est.admiteInscripcion());
    }
}
