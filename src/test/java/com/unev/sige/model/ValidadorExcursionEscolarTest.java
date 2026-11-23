package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorExcursionEscolarTest {

    @Test
    @DisplayName("Valida código de excursión escolar")
    void esCodigoExcursionValido_casos() {
        assertTrue(ValidadorExcursionEscolar.esCodigoExcursionValido("EXC-2026-001"));
        assertFalse(ValidadorExcursionEscolar.esCodigoExcursionValido(null));
    }

    @Test
    @DisplayName("Excursión con más del 80% de autorizaciones y seguro es aprobada")
    void excursion_aprobada() {
        ValidadorExcursionEscolar.EventoExcursion exc = new ValidadorExcursionEscolar.EventoExcursion(
                "EXC-2026-001", "Jardín Botánico Nacional", 40, 36, true // 90%
        );
        assertTrue(exc.puedeRealizarse());
    }

    @Test
    @DisplayName("Excursión sin seguro de accidentes no puede realizarse")
    void excursion_sinSeguro() {
        ValidadorExcursionEscolar.EventoExcursion exc = new ValidadorExcursionEscolar.EventoExcursion(
                "EXC-2026-001", "Museo de Historia Natural", 40, 38, false
        );
        assertFalse(exc.puedeRealizarse());
    }
}
