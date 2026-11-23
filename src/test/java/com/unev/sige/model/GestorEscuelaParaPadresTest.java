package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GestorEscuelaParaPadresTest {

    @Test
    @DisplayName("Tutor que asiste a 3 o más talleres cumple compromiso de formación familiar")
    void cumpleCompromisoFamiliar_evaluacion() {
        List<GestorEscuelaParaPadres.AsistenciaTaller> talleres = List.of(
                new GestorEscuelaParaPadres.AsistenciaTaller("Pautas de Crianza", "2026-02-15", true),
                new GestorEscuelaParaPadres.AsistenciaTaller("Uso Seguro de Redes", "2026-05-10", true),
                new GestorEscuelaParaPadres.AsistenciaTaller("Orientación Vocacional", "2026-10-01", true)
        );
        assertTrue(GestorEscuelaParaPadres.cumpleCompromisoFamiliar(talleres));

        List<GestorEscuelaParaPadres.AsistenciaTaller> incompletos = List.of(
                new GestorEscuelaParaPadres.AsistenciaTaller("Pautas de Crianza", "2026-02-15", true)
        );
        assertFalse(GestorEscuelaParaPadres.cumpleCompromisoFamiliar(incompletos));
    }
}
