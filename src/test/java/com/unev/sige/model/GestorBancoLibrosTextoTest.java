package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GestorBancoLibrosTextoTest {

    @Test
    @DisplayName("Libro devuelto en buen estado no requiere cobro")
    void requiereCobroReposicion_correcto() {
        assertFalse(GestorBancoLibrosTexto.requiereCobroReposicion(
                GestorBancoLibrosTexto.EstadoFisicoLibro.EXCELENTE,
                GestorBancoLibrosTexto.EstadoFisicoLibro.BUENO,
                true
        ));
    }

    @Test
    @DisplayName("Libro no devuelto o gravemente deteriorado exige reposición")
    void requiereCobroReposicion_danadoOExtraviado() {
        // Extraviado
        assertTrue(GestorBancoLibrosTexto.requiereCobroReposicion(
                GestorBancoLibrosTexto.EstadoFisicoLibro.BUENO, null, false
        ));

        // Deteriorado
        assertTrue(GestorBancoLibrosTexto.requiereCobroReposicion(
                GestorBancoLibrosTexto.EstadoFisicoLibro.EXCELENTE,
                GestorBancoLibrosTexto.EstadoFisicoLibro.DETERIORADO_REQUIERE_REEMPLAZO,
                true
        ));
    }
}
