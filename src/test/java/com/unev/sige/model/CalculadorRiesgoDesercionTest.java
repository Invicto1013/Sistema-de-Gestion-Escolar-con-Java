package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CalculadorRiesgoDesercionTest {

    @Test
    @DisplayName("Estudiante destacado presenta riesgo bajo de deserción")
    void evaluar_riesgoBajo() {
        // 5% inasistencias, promedio 92, 0 faltas
        CalculadorRiesgoDesercion.DiagnosticoRiesgo diag = CalculadorRiesgoDesercion.evaluar(5.0, 92.0, 0);

        assertEquals(CalculadorRiesgoDesercion.NivelRiesgo.BAJO, diag.nivel());
        assertFalse(diag.requiereIntervencionInmediata());
        assertTrue(diag.indiceRiesgoPonderado() < 20.0);
    }

    @Test
    @DisplayName("Estudiante con ausencias y bajo rendimiento activa alerta de riesgo alto")
    void evaluar_riesgoAlto() {
        // 60% inasistencias, promedio 40, 3 faltas: 60*0.4 + 60*0.4 + 60*0.2 = 60 -> ALTO
        CalculadorRiesgoDesercion.DiagnosticoRiesgo diag = CalculadorRiesgoDesercion.evaluar(60.0, 40.0, 3);

        assertEquals(CalculadorRiesgoDesercion.NivelRiesgo.ALTO, diag.nivel());
        assertTrue(diag.requiereIntervencionInmediata());
        assertTrue(diag.indiceRiesgoPonderado() >= 60.0);
    }
}
