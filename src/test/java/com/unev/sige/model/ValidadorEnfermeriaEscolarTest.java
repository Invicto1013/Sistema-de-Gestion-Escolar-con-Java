package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorEnfermeriaEscolarTest {

    @Test
    @DisplayName("Valida código oficial de atención de enfermería ENF-YYYY-XXXX")
    void esCodigoAtencionValido_casos() {
        assertTrue(ValidadorEnfermeriaEscolar.esCodigoAtencionValido("ENF-2026-00012"));
        assertFalse(ValidadorEnfermeriaEscolar.esCodigoAtencionValido(null));
        assertFalse(ValidadorEnfermeriaEscolar.esCodigoAtencionValido("ENF-12"));
    }
}
