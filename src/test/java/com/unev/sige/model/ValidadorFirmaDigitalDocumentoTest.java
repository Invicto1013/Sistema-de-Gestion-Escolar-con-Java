package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorFirmaDigitalDocumentoTest {

    @Test
    @DisplayName("Valida códigos de verificación de documento con formato DOC-XXXX-XXXX-XXXX")
    void esCodigoValido_casos() {
        assertTrue(ValidadorFirmaDigitalDocumento.esCodigoValido("DOC-2026-A1B2-C3D4"));
        assertTrue(ValidadorFirmaDigitalDocumento.esCodigoValido("doc-abcd-1234-ef56"));

        assertFalse(ValidadorFirmaDigitalDocumento.esCodigoValido(null));
        assertFalse(ValidadorFirmaDigitalDocumento.esCodigoValido("DOC-2026-123")); // Muy corto
        assertFalse(ValidadorFirmaDigitalDocumento.esCodigoValido("2026-A1B2-C3D4")); // Falta DOC
    }

    @Test
    @DisplayName("Genera URL de validación institucional segura")
    void generarUrlVerificacion_generacion() {
        String url = ValidadorFirmaDigitalDocumento.generarUrlVerificacion("colegio.edu.do", "DOC-2026-A1B2-C3D4");
        assertEquals("https://colegio.edu.do/verificar/DOC-2026-A1B2-C3D4", url);
    }

    @Test
    @DisplayName("Construye el expediente de certificación de documento")
    void certificar_construccion() {
        ValidadorFirmaDigitalDocumento.DocumentoCertificado doc = ValidadorFirmaDigitalDocumento.certificar(
                "DOC-2026-A1B2-C3D4", "RECORD_NOTAS", "2026-0042", "HASH-SHA256-OK"
        );

        assertEquals("DOC-2026-A1B2-C3D4", doc.codigoDocumento());
        assertEquals("RECORD_NOTAS", doc.tipoDocumento());
        assertEquals("https://sige.edu.do/verificar/DOC-2026-A1B2-C3D4", doc.urlVerificacion());
    }
}
