package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorCentroEducativoTest {

    @Test
    @DisplayName("Valida código de campus o sede institucional")
    void esCodigoCentroValido_casos() {
        assertTrue(ValidadorCentroEducativo.esCodigoCentroValido("SEDE-01"));
        assertTrue(ValidadorCentroEducativo.esCodigoCentroValido("CAMPUS-NORTE"));
        assertTrue(ValidadorCentroEducativo.esCodigoCentroValido("UNEV-01"));

        assertFalse(ValidadorCentroEducativo.esCodigoCentroValido(null));
        assertFalse(ValidadorCentroEducativo.esCodigoCentroValido("AB")); // Menor a 3 caracteres
    }

    @Test
    @DisplayName("Valida RNC institucional de 9 dígitos")
    void esRncInstitucionalValido_casos() {
        assertTrue(ValidadorCentroEducativo.esRncInstitucionalValido("101000000"));
        assertTrue(ValidadorCentroEducativo.esRncInstitucionalValido("130123456"));

        assertFalse(ValidadorCentroEducativo.esRncInstitucionalValido("12345678")); // 8 dígitos
        assertFalse(ValidadorCentroEducativo.esRncInstitucionalValido(null));
    }

    @Test
    @DisplayName("Valida siglas institucionales de la entidad")
    void esSiglasValidas_casos() {
        assertTrue(ValidadorCentroEducativo.esSiglasValidas("UNEV"));
        assertTrue(ValidadorCentroEducativo.esSiglasValidas("SIGE"));
        assertTrue(ValidadorCentroEducativo.esSiglasValidas("CAE"));

        assertFalse(ValidadorCentroEducativo.esSiglasValidas("A")); // Muy corta
        assertFalse(ValidadorCentroEducativo.esSiglasValidas("UNEV-1")); // Con números/guiones
        assertFalse(ValidadorCentroEducativo.esSiglasValidas(null));
    }

    @Test
    @DisplayName("Genera ficha de identidad institucional del plantel")
    void fichaIdentidad_generacion() {
        ValidadorCentroEducativo.CentroEducativo centro = new ValidadorCentroEducativo.CentroEducativo(
                "SEDE-CENTRAL", "Colegio Evangélico Central", "CEC", "Técnico-Profesional", "101234567"
        );
        assertEquals("Colegio Evangélico Central (CEC) - Sede: SEDE-CENTRAL [Modalidad: Técnico-Profesional]",
                centro.fichaIdentidad());
    }
}
