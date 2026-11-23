package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorCertificadoSaludEscolarTest {

    @Test
    @DisplayName("Valida formato de exequátur médico dominicano")
    void esExequaturValido_evaluacion() {
        assertTrue(ValidadorCertificadoSaludEscolar.esExequaturValido("12345-20"));
        assertTrue(ValidadorCertificadoSaludEscolar.esExequaturValido("452-18"));

        assertFalse(ValidadorCertificadoSaludEscolar.esExequaturValido(null));
        assertFalse(ValidadorCertificadoSaludEscolar.esExequaturValido("12-2")); // Muy corto
        assertFalse(ValidadorCertificadoSaludEscolar.esExequaturValido("ABC-12"));
    }

    @Test
    @DisplayName("Evalúa si el certificado de salud es apto para el registro escolar")
    void esExpedienteApto_evaluacion() {
        ValidadorCertificadoSaludEscolar.CertificadoSalud certApto = new ValidadorCertificadoSaludEscolar.CertificadoSalud(
                "MED-2026-001", "1234-18", "Hospital Robert Reid Cabral", true, true
        );
        assertTrue(ValidadorCertificadoSaludEscolar.esExpedienteApto(certApto));

        ValidadorCertificadoSaludEscolar.CertificadoSalud certSinVacunas = new ValidadorCertificadoSaludEscolar.CertificadoSalud(
                "MED-2026-002", "1234-18", "Centro Médico", false, true
        );
        assertFalse(ValidadorCertificadoSaludEscolar.esExpedienteApto(certSinVacunas));
    }
}
