package com.unev.sige.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class LimitesCamposTest {
    @Test
    void aceptaValoresDentroDelLimite() {
        assertNull(LimitesCampos.validar("/registro",
                Map.of("nombre", "a".repeat(50), "correo", "b".repeat(100), "matricula", "12345")));
    }

    @Test
    void rechazaUnValorUnCaracterMasLargo() {
        String mensaje = LimitesCampos.validar("/registro", Map.of("nombre", "a".repeat(51)));
        assertNotNull(mensaje);
        assertTrue(mensaje.contains("nombre") && mensaje.contains("50"));
        assertNotNull(LimitesCampos.validar("/login", Map.of("correo", "x".repeat(101))));
    }

    @Test
    void elNombreDeUnaAsignaturaAdmiteCien() {
        assertEquals(100, LimitesCampos.maximo("/guardar-asignatura", "nombre"));
        assertEquals(50, LimitesCampos.maximo("/registro", "nombre"));
        assertNull(LimitesCampos.validar("/guardar-asignatura", Map.of("nombre", "m".repeat(100))));
        assertNotNull(LimitesCampos.validar("/guardar-asignatura", Map.of("nombre", "m".repeat(101))));
    }

    @Test
    void cuentaCaracteresYNoUnidadesUtf16() {
        // 50 emojis = 100 unidades UTF-16 pero 50 caracteres: MySQL utf8mb4 los acepta.
        String emojis = "\uD83D\uDE00".repeat(50);
        assertNull(LimitesCampos.validar("/registro", Map.of("nombre", emojis)));
        assertNotNull(LimitesCampos.validar("/registro", Map.of("nombre", "\uD83D\uDE00".repeat(51))));
    }

    @Test
    void loscamposDesconocidosTienenUnTopePorDefecto() {
        assertEquals(255, LimitesCampos.maximo("/x", "campoNuevo"));
        assertNotNull(LimitesCampos.validar("/x", Map.of("campoNuevo", "z".repeat(256))));
    }

    @Test
    void laDescripcionPermiteTextosLargosPeroNoIlimitados() {
        assertNull(LimitesCampos.validar("/aula-publicar", Map.of("descripcion", "d".repeat(5000))));
        assertNotNull(LimitesCampos.validar("/aula-publicar", Map.of("descripcion", "d".repeat(5001))));
    }

    @Test
    void ignoraValoresNulosYMensajeNoReflejaElValor() {
        java.util.HashMap<String, String> m = new java.util.HashMap<>();
        m.put("nombre", null);
        assertNull(LimitesCampos.validar("/registro", m));
        String msg = LimitesCampos.validar("/registro", Map.of("nombre", "<script>".repeat(10)));
        assertTrue(!msg.contains("<script>"));
    }
}
