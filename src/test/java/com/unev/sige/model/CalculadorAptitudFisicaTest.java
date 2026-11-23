package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CalculadorAptitudFisicaTest {

    @Test
    @DisplayName("Estudiante con marcas óptimas en los 4 tests obtiene Excelente Condición")
    void calificarDesempeno_excelente() {
        CalculadorAptitudFisica.EvaluacionFisica ev = new CalculadorAptitudFisica.EvaluacionFisica(
                7.8, 35, 18.0, 2000.0
        );
        assertEquals("EXCELENTE_CONDICION", CalculadorAptitudFisica.calificarDesempeno(ev));
    }

    @Test
    @DisplayName("Estudiante con marcas bajas obtiene Requiere Acondicionamiento")
    void calificarDesempeno_acondicionamiento() {
        CalculadorAptitudFisica.EvaluacionFisica ev = new CalculadorAptitudFisica.EvaluacionFisica(
                11.0, 15, 8.0, 1200.0
        );
        assertEquals("REQUIERE_ACONDICIONAMIENTO", CalculadorAptitudFisica.calificarDesempeno(ev));
    }
}
