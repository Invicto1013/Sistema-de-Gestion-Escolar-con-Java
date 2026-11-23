package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorTransporteEscolarTest {

    @Test
    @DisplayName("Valida códigos de ruta de transporte escolar (RUTA-XX)")
    void esCodigoRutaValido_casos() {
        assertTrue(ValidadorTransporteEscolar.esCodigoRutaValido("RUTA-01"));
        assertTrue(ValidadorTransporteEscolar.esCodigoRutaValido("ruta-105"));

        assertFalse(ValidadorTransporteEscolar.esCodigoRutaValido(null));
        assertFalse(ValidadorTransporteEscolar.esCodigoRutaValido("BUS-01"));
    }

    @Test
    @DisplayName("Asignación de transporte completa con autorización firmada")
    void esAsignacionValida_evaluacion() {
        ValidadorTransporteEscolar.AsignacionTransporte asig = new ValidadorTransporteEscolar.AsignacionTransporte(
                "RUTA-05", 12, "Av. Winston Churchill con 27 de Febrero", "MATUTINO", true
        );
        assertTrue(ValidadorTransporteEscolar.esAsignacionValida(asig));

        ValidadorTransporteEscolar.AsignacionTransporte sinFirma = new ValidadorTransporteEscolar.AsignacionTransporte(
                "RUTA-05", 12, "Av. Principal", "MATUTINO", false
        );
        assertFalse(ValidadorTransporteEscolar.esAsignacionValida(sinFirma));
    }
}
