package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CalculadorFinanzasEscolaresTest {

    @Test
    @DisplayName("Aplica descuento de pronto pago y hermano matriculado")
    void calcularCuota_conDescuentos() {
        // Base 5000, 2 hermanos (+10%), pronto pago (+5%) = 15% desc. = 750 -> Pagar 4250
        CalculadorFinanzasEscolares.EstadoCuentaEstudiante ec = CalculadorFinanzasEscolares.calcularCuota(
                5000.0, 2, true, 0
        );

        assertEquals(5000.0, ec.montoBase(), 0.001);
        assertEquals(750.0, ec.descuentoAplicado(), 0.001);
        assertEquals(0.0, ec.recargoMora(), 0.001);
        assertEquals(4250.0, ec.saldoFinalPagar(), 0.001);
        assertTrue(ec.alDia());
    }

    @Test
    @DisplayName("Calcula recargo por mora mensual en cuotas vencidas")
    void calcularCuota_conMora() {
        // Base 5000, 2 meses vencidos (5% * 2 = 10% mora) = +500 -> Pagar 5500
        CalculadorFinanzasEscolares.EstadoCuentaEstudiante ec = CalculadorFinanzasEscolares.calcularCuota(
                5000.0, 1, false, 2
        );

        assertEquals(500.0, ec.recargoMora(), 0.001);
        assertEquals(5500.0, ec.saldoFinalPagar(), 0.001);
        assertFalse(ec.alDia());
    }
}
