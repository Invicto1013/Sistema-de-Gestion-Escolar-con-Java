package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorRedEgresadosTest {

    @Test
    @DisplayName("Valida código oficial de egresado ALM-YYYY-XXXX")
    void esCodigoEgresadoValido_casos() {
        assertTrue(ValidadorRedEgresados.esCodigoEgresadoValido("ALM-2025-00124"));
        assertFalse(ValidadorRedEgresados.esCodigoEgresadoValido(null));
        assertFalse(ValidadorRedEgresados.esCodigoEgresadoValido("ALM-25-1"));
    }
}
