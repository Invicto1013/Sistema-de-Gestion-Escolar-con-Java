package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorLaboratorioInformaticaTest {

    @Test
    @DisplayName("Valida direcciones MAC de equipos del laboratorio escolar")
    void esDireccionMacValida_casos() {
        assertTrue(ValidadorLaboratorioInformatica.esDireccionMacValida("00:1A:2B:3C:4D:5E"));
        assertTrue(ValidadorLaboratorioInformatica.esDireccionMacValida("00-1B-44-11-3A-B7"));

        assertFalse(ValidadorLaboratorioInformatica.esDireccionMacValida("00:1A:2B:3C:4D")); // Incompleta
        assertFalse(ValidadorLaboratorioInformatica.esDireccionMacValida("00:1A:2B:3C:4D:5G")); // 'G' no hexadecimal
        assertFalse(ValidadorLaboratorioInformatica.esDireccionMacValida(null));
    }
}
