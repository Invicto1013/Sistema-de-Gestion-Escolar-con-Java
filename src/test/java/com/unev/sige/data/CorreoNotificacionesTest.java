package com.unev.sige.data;

import static org.junit.jupiter.api.Assertions.*;

import com.unev.sige.model.Rol;
import com.unev.sige.model.Usuario;
import org.junit.jupiter.api.Test;

class CorreoNotificacionesTest {
    @Test void soloAceptaCorreosPersonalesDeGoogleYMicrosoft() {
        assertTrue(CorreoNotificaciones.aceptaDestinatario("ana@gmail.com"));
        assertTrue(CorreoNotificaciones.aceptaDestinatario("Ana@Outlook.com"));
        assertTrue(CorreoNotificaciones.aceptaDestinatario("ana@hotmail.es"));
        assertFalse(CorreoNotificaciones.aceptaDestinatario("ana@colegio.edu.do"));
        assertFalse(CorreoNotificaciones.aceptaDestinatario("sin-arroba"));
        assertFalse(CorreoNotificaciones.aceptaDestinatario("@gmail.com"));
        assertFalse(CorreoNotificaciones.aceptaDestinatario(null));
    }
    @Test void elAvisoDeCuentaNuncaIncluyeLaContrasena() {
        Usuario u = new Usuario(1, "Luisa", "Perez", "luisa@gmail.com", null, Rol.DIRECTOR);
        CorreoNotificaciones.Mensaje m = CorreoNotificaciones.mensajePerfilPersonal(u);
        assertTrue(m.asunto().contains("director/a"));
        assertTrue(m.cuerpo().contains("luisa@gmail.com"));
        assertTrue(m.cuerpo().contains("no incluye tu contraseña"));
        Usuario s = new Usuario(2, "Mario", "Diaz", "mario@outlook.com", null, Rol.SECRETARIA);
        assertTrue(CorreoNotificaciones.mensajePerfilPersonal(s).asunto().contains("secretario/a"));
    }
    @Test void enlaceMailtoCodificaElMensaje() {
        CorreoNotificaciones.Mensaje m = new CorreoNotificaciones.Mensaje("Hola mundo", "Línea 1\nLínea 2 & más");
        String enlace = CorreoNotificaciones.enlaceMailto("ana@gmail.com", m);
        assertTrue(enlace.startsWith("mailto:ana%40gmail.com?subject=Hola%20mundo&body="));
        assertFalse(enlace.contains(" "));
        assertFalse(enlace.contains("\n"));
        assertTrue(enlace.contains("%26"));
    }
    @Test void jsonDeLaApiEscapaComillasYSaltosDeLinea() {
        String json = CorreoNotificaciones.cuerpoJsonApi("sige@gmail.com", "SIGE", "ana@gmail.com", "Ana \"A\"", "Asunto", "Uno\nDos\\");
        assertTrue(json.contains("\"name\":\"Ana \\\"A\\\"\""));
        assertTrue(json.contains("Uno\\nDos\\\\"));
        assertTrue(json.startsWith("{\"sender\""));
    }
}
