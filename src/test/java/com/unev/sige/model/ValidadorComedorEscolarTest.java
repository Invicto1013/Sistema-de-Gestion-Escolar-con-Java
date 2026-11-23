package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorComedorEscolarTest {

    @Test
    @DisplayName("Plato sin alérgenos es apto para el estudiante")
    void esAptoMenu_sinAlergenos() {
        ValidadorComedorEscolar.FichaNutricional ficha = new ValidadorComedorEscolar.FichaNutricional(
                ValidadorComedorEscolar.PlanAlimentario.REGULAR, Set.of("MANI", "MARISCOS"), false
        );

        Set<String> plato = Set.of("ARROZ", "POLLO", "ENSALADA");
        assertTrue(ValidadorComedorEscolar.esAptoMenu(ficha, plato));
    }

    @Test
    @DisplayName("Detecta alérgeno peligroso en plato y rechaza servicio")
    void esAptoMenu_conAlergeno() {
        ValidadorComedorEscolar.FichaNutricional ficha = new ValidadorComedorEscolar.FichaNutricional(
                ValidadorComedorEscolar.PlanAlimentario.REGULAR, Set.of("MANI", "HUEVO"), true
        );

        Set<String> plato = Set.of("ARROZ", "SALSA_CON_MANI", "VEGETALES");
        assertFalse(ValidadorComedorEscolar.esAptoMenu(ficha, plato));
    }
}
