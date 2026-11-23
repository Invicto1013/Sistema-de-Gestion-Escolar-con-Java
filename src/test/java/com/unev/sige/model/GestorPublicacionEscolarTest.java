package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GestorPublicacionEscolarTest {

    @Test
    @DisplayName("Valida código de publicación escolar PUB-XXX-YYYY-NNN")
    void esCodigoPublicacionValido_casos() {
        assertTrue(GestorPublicacionEscolar.esCodigoPublicacionValido("PUB-REV-2026-001"));
        assertFalse(GestorPublicacionEscolar.esCodigoPublicacionValido(null));
        assertFalse(GestorPublicacionEscolar.esCodigoPublicacionValido("PUB-2026-01"));
    }
}
