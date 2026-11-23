package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorVotoEstudiantilTest {

    @Test
    @DisplayName("Voto emitido dentro del período electoral es válido")
    void esVotoValido_enPeriodo() {
        LocalDate ini = LocalDate.of(2026, 11, 20);
        LocalDate fin = LocalDate.of(2026, 11, 22);

        ValidadorVotoEstudiantil.VotoEmitido voto = new ValidadorVotoEstudiantil.VotoEmitido(
                "SIGE-2026-00042", "PLANCHA-AZUL", LocalDate.of(2026, 11, 21)
        );
        assertTrue(ValidadorVotoEstudiantil.esVotoValido(voto, ini, fin));
    }

    @Test
    @DisplayName("Voto fuera de fecha es rechazado")
    void esVotoValido_fueraDeFecha() {
        LocalDate ini = LocalDate.of(2026, 11, 20);
        LocalDate fin = LocalDate.of(2026, 11, 22);

        ValidadorVotoEstudiantil.VotoEmitido voto = new ValidadorVotoEstudiantil.VotoEmitido(
                "SIGE-2026-00042", "PLANCHA-AZUL", LocalDate.of(2026, 11, 25)
        );
        assertFalse(ValidadorVotoEstudiantil.esVotoValido(voto, ini, fin));
    }
}
