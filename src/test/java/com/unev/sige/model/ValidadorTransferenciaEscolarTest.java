package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorTransferenciaEscolarTest {

    @Test
    @DisplayName("Valida formato de código de transferencia TRF-YYYY-XXXXX")
    void esCodigoTransferenciaValido_casos() {
        assertTrue(ValidadorTransferenciaEscolar.esCodigoTransferenciaValido("TRF-2026-AB123"));
        assertTrue(ValidadorTransferenciaEscolar.esCodigoTransferenciaValido("trf-2026-998877"));

        assertFalse(ValidadorTransferenciaEscolar.esCodigoTransferenciaValido(null));
        assertFalse(ValidadorTransferenciaEscolar.esCodigoTransferenciaValido("TRF-123"));
    }

    @Test
    @DisplayName("Solicitud con paz y salvo y récord es aprobada para traslado")
    void esSolicitudValida_aprobada() {
        ValidadorTransferenciaEscolar.SolicitudTransferencia sol = new ValidadorTransferenciaEscolar.SolicitudTransferencia(
                "TRF-2026-0012A", "Colegio Alfa", "Colegio Beta", "Cambio de domicilio", true, true
        );
        assertTrue(ValidadorTransferenciaEscolar.esSolicitudValida(sol));
        assertTrue(sol.puedeProceder());
    }

    @Test
    @DisplayName("Solicitud sin paz y salvo financiero no puede proceder")
    void esSolicitudValida_conDeuda_bloqueada() {
        ValidadorTransferenciaEscolar.SolicitudTransferencia sol = new ValidadorTransferenciaEscolar.SolicitudTransferencia(
                "TRF-2026-0012A", "Colegio Alfa", "Colegio Beta", "Cambio de domicilio", false, true
        );
        assertFalse(ValidadorTransferenciaEscolar.esSolicitudValida(sol));
        assertFalse(sol.puedeProceder());
    }
}
