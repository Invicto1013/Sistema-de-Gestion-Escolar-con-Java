package com.unev.sige.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConexionBDTest {

    @Test
    @DisplayName("Sin valor o vacío usa el tamaño de pool por defecto (5)")
    void tamanoPool_sinValor_devuelvePorDefecto() {
        assertEquals(5, ConexionBD.tamanoPool(null));
        assertEquals(5, ConexionBD.tamanoPool("  "));
    }

    @Test
    @DisplayName("Un número válido dentro del rango se respeta")
    void tamanoPool_valorValido_seRespeta() {
        assertEquals(8, ConexionBD.tamanoPool("8"));
        assertEquals(12, ConexionBD.tamanoPool(" 12 "));
    }

    @Test
    @DisplayName("Un texto no numérico vuelve al valor por defecto")
    void tamanoPool_textoInvalido_devuelvePorDefecto() {
        assertEquals(5, ConexionBD.tamanoPool("muchos"));
    }

    @Test
    @DisplayName("Los valores fuera de rango se limitan a 1 y 20")
    void tamanoPool_fueraDeRango_seLimita() {
        assertEquals(1, ConexionBD.tamanoPool("0"));
        assertEquals(1, ConexionBD.tamanoPool("-3"));
        assertEquals(20, ConexionBD.tamanoPool("500"));
    }
}
