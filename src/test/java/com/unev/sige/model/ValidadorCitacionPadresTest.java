package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorCitacionPadresTest {

    @Test
    @DisplayName("Valida código oficial de citación a padres CIT-YYYY-XXXX")
    void esCodigoCitacionValido_casos() {
        assertTrue(ValidadorCitacionPadres.esCodigoCitacionValido("CIT-2026-0045"));
        assertFalse(ValidadorCitacionPadres.esCodigoCitacionValido(null));
        assertFalse(ValidadorCitacionPadres.esCodigoCitacionValido("CIT-26-1"));
    }
}
