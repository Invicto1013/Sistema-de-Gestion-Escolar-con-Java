package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UsuarioTest {
    private static Usuario ana(String clave) {
        return new Usuario(1, "Ana", "Pérez", "ana@sige.edu.do", Usuario.hashear(clave), Rol.ESTUDIANTE);
    }

    @Test
    void hashearGeneraBcryptDistintoEnCadaLlamada() {
        String h1 = Usuario.hashear("Clave123");
        String h2 = Usuario.hashear("Clave123");
        assertTrue(h1.startsWith("$2a$"));
        assertNotEquals("Clave123", h1);
        assertNotEquals(h1, h2);
    }

    @Test
    void autenticaConCorreoYContrasenaCorrectos() {
        assertTrue(ana("Clave123").autenticar("ana@sige.edu.do", "Clave123"));
    }

    @Test
    void ignoraMayusculasYEspaciosEnElCorreoPeroNoEnLaClave() {
        Usuario u = ana("Clave123");
        assertTrue(u.autenticar("  ANA@SIGE.EDU.DO ", "Clave123"));
        assertFalse(u.autenticar("ana@sige.edu.do", "clave123"));
    }

    @Test
    void rechazaCorreoDistintoOContrasenaIncorrecta() {
        Usuario u = ana("Clave123");
        assertFalse(u.autenticar("otra@sige.edu.do", "Clave123"));
        assertFalse(u.autenticar("ana@sige.edu.do", "Incorrecta1"));
    }

    @Test
    void entradasNulasOHashInvalidoNoLanzanExcepcion() {
        Usuario u = ana("Clave123");
        assertFalse(u.autenticar(null, "Clave123"));
        assertFalse(u.autenticar("ana@sige.edu.do", null));
        Usuario roto = new Usuario(2, "Luis", "Gil", "luis@sige.edu.do", "no-es-un-hash", Rol.PROFESOR);
        assertFalse(roto.autenticar("luis@sige.edu.do", "lo-que-sea"));
        assertFalse(new Usuario().autenticar("a@b.c", "x"));
    }

    @Test
    void cambiarContrasenaIgnoraValoresVaciosYActualizaConValidos() {
        Usuario u = ana("Clave123");
        u.cambiarContrasena("   ");
        u.cambiarContrasena(null);
        assertTrue(u.autenticar("ana@sige.edu.do", "Clave123"));
        u.cambiarContrasena("Nueva4567");
        assertFalse(u.autenticar("ana@sige.edu.do", "Clave123"));
        assertTrue(u.autenticar("ana@sige.edu.do", "Nueva4567"));
    }

    @Test
    void laIdentidadSeBasaEnElIdYLaCuentaIniciaActiva() {
        Usuario a = new Usuario(5, "A", "B", "a@x.do", "h", Rol.ADMIN);
        Usuario b = new Usuario(5, "Otro", "Nombre", "b@x.do", "h2", Rol.PADRE);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new Usuario(6, "A", "B", "a@x.do", "h", Rol.ADMIN));
        assertTrue(a.isActivo());
        assertEquals("A B", a.getNombreCompleto());
    }
}
