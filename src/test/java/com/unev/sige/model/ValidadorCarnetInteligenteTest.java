package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorCarnetInteligenteTest {

    @Test
    @DisplayName("Genera y valida payload de carnet inteligente para código QR")
    void generarPayloadQr_generacionYValidacion() {
        String payload = ValidadorCarnetInteligente.generarPayloadQr("SIGE-2026-00042", 2026, "SEC99A1");
        assertEquals("SIGE-ID:SIGE-2026-00042:2026:SEC99A1", payload);
        assertTrue(ValidadorCarnetInteligente.esPayloadValido(payload));
    }

    @Test
    @DisplayName("Rechaza payloads de carnet malformados o nulos")
    void esPayloadValido_rechazaMalos() {
        assertFalse(ValidadorCarnetInteligente.esPayloadValido(null));
        assertFalse(ValidadorCarnetInteligente.esPayloadValido(""));
        assertFalse(ValidadorCarnetInteligente.esPayloadValido("CARNET:2026:001")); // Formato ajeno
    }

    @Test
    @DisplayName("Analiza vigencia del carnet contra el año lectivo")
    void analizar_vigencia() {
        String payload = "SIGE-ID:SIGE-2026-00042:2026:SEC99A1";
        ValidadorCarnetInteligente.CarnetDigital carnetVigente = ValidadorCarnetInteligente.analizar(payload, 2026);
        assertTrue(carnetVigente.estaVigente());

        ValidadorCarnetInteligente.CarnetDigital carnetVencido = ValidadorCarnetInteligente.analizar(payload, 2027);
        assertFalse(carnetVencido.estaVigente());
    }
}
