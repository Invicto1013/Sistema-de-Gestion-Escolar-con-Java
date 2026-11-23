package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorAdmisionNuevoIngresoTest {

    @Test
    @DisplayName("Valida código de solicitud de admisión ADM-YYYY-XXXXX")
    void esCodigoSolicitudValido_casos() {
        assertTrue(ValidadorAdmisionNuevoIngreso.esCodigoSolicitudValido("ADM-2026-00142"));
        assertFalse(ValidadorAdmisionNuevoIngreso.esCodigoSolicitudValido(null));
        assertFalse(ValidadorAdmisionNuevoIngreso.esCodigoSolicitudValido("ADM-12"));
    }

    @Test
    @DisplayName("Evalúa completitud del expediente de admisión")
    void esExpedienteCompleto_casos() {
        ValidadorAdmisionNuevoIngreso.ExpedienteAdmision compl = new ValidadorAdmisionNuevoIngreso.ExpedienteAdmision(
                "ADM-2026-00142", "Lucas Díaz", true, true, true, true
        );
        assertTrue(compl.esExpedienteCompleto());

        ValidadorAdmisionNuevoIngreso.ExpedienteAdmision incomp = new ValidadorAdmisionNuevoIngreso.ExpedienteAdmision(
                "ADM-2026-00142", "Lucas Díaz", true, false, true, true
        );
        assertFalse(incomp.esExpedienteCompleto());
    }
}
