package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorCasilleroEscolarTest {

    @Test
    @DisplayName("Genera y valida códigos de casillero estándar CAS-X-000")
    void casillero_generacionYValidacion() {
        String codigo = ValidadorCasilleroEscolar.generarCodigo('A', 45);
        assertEquals("CAS-A-045", codigo);
        assertTrue(ValidadorCasilleroEscolar.esCodigoCasilleroValido(codigo));

        assertFalse(ValidadorCasilleroEscolar.esCodigoCasilleroValido(null));
        assertFalse(ValidadorCasilleroEscolar.esCodigoCasilleroValido("CAS-12"));
    }
}
