package com.unev.sige.data;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Configuración propia de cada equipo: valida, guarda y importa sin tocar el repositorio ni mostrar claves. */
class ConfiguracionUsuarioTest {

    private static final String URL = "jdbc:mysql://servidor:4000/sige_db?sslMode=REQUIRED";
    private Path carpeta;

    @BeforeEach
    void preparar() throws Exception {
        carpeta = Files.createTempDirectory("sige-usuario");
        System.setProperty("sige.config.dir", carpeta.toString());
    }

    @AfterEach
    void limpiar() {
        System.clearProperty("sige.config.dir");
    }

    private static Map<String, String> datos(String clave) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(ConfiguracionUsuario.CLAVE_URL, URL);
        m.put(ConfiguracionUsuario.CLAVE_USUARIO, "sige_app");
        m.put(ConfiguracionUsuario.CLAVE_PASSWORD, clave);
        return m;
    }

    @Test
    void laCarpetaPuedeForzarsePorPropiedad() {
        assertEquals(carpeta.resolve("config.properties"), ConfiguracionUsuario.archivo());
    }

    @Test
    void validaDireccionUsuarioYClave2fa() {
        assertTrue(ConfiguracionUsuario.validar(URL, "u", "").isEmpty());
        assertFalse(ConfiguracionUsuario.validar("", "u", "").isEmpty());
        assertFalse(ConfiguracionUsuario.validar("http://x", "u", "").isEmpty());
        assertFalse(ConfiguracionUsuario.validar("jdbc:mysql://", "u", "").isEmpty());
        assertFalse(ConfiguracionUsuario.validar("jdbc:mysql://a b", "u", "").isEmpty());
        assertFalse(ConfiguracionUsuario.validar(URL, "  ", "").isEmpty());
        assertFalse(ConfiguracionUsuario.validar(URL, "u", "no-es-base64!!").isEmpty());
        assertFalse(ConfiguracionUsuario.validar(URL, "u", Base64.getEncoder().encodeToString(new byte[16])).isEmpty());
        assertTrue(ConfiguracionUsuario.validar(URL, "u", Base64.getEncoder().encodeToString(new byte[32])).isEmpty());
    }

    @Test
    void losMensajesDeErrorNoIncluyenLosValores() {
        String secreto = "ClaveSuperSecreta#1";
        String texto = String.join(" ", ConfiguracionUsuario.validar("ftp://" + secreto, "usuario-x", secreto));
        assertFalse(texto.contains(secreto));
        assertFalse(texto.contains("usuario-x"));
    }

    @Test
    void guardaYRecuperaClavesConCaracteresEspeciales() throws Exception {
        String clave = "p#ss=wo\\rd !:é";
        ConfiguracionUsuario.guardar(datos(clave));
        Properties p = ConfiguracionUsuario.actual();
        assertEquals(URL, p.getProperty("db.url"));
        assertEquals("sige_app", p.getProperty("db.user"));
        assertEquals(clave, p.getProperty("db.password"));
    }

    @Test
    void guardarConservaOtrasClavesYNoBorraConVacios() throws Exception {
        Files.writeString(ConfiguracionUsuario.archivo(), "mail.smtp.host=smtp.gmail.com\ndb.password=vieja\n");
        Map<String, String> m = datos("");
        m.put(ConfiguracionUsuario.CLAVE_2FA, "");
        ConfiguracionUsuario.guardar(m);
        Properties p = ConfiguracionUsuario.actual();
        assertEquals("smtp.gmail.com", p.getProperty("mail.smtp.host"));
        assertEquals("vieja", p.getProperty("db.password"), "un valor vacío no sobrescribe");
        assertNull(p.getProperty("2fa.encryption-key"));
        assertEquals(URL, p.getProperty("db.url"));
    }

    @Test
    void noDejaArchivosTemporales() throws Exception {
        ConfiguracionUsuario.guardar(datos("x"));
        try (var s = Files.list(carpeta)) {
            assertEquals(1, s.count());
        }
    }

    @Test
    void importaUnArchivoConBomYLoCopiaAlEquipo() throws Exception {
        Path origen = Files.createTempFile("origen", ".properties");
        Files.write(origen, ("﻿db.url=" + URL + "\ndb.user=otro\ndb.password=abc\n").getBytes(StandardCharsets.UTF_8));
        ConfiguracionUsuario.importar(origen);
        Properties p = ConfiguracionUsuario.actual();
        assertEquals("otro", p.getProperty("db.user"));
        assertEquals(URL, p.getProperty("db.url"));
    }

    @Test
    void noImportaUnArchivoIncompleto() throws Exception {
        Path origen = Files.createTempFile("origen", ".properties");
        Files.writeString(origen, "db.user=solo-usuario\n");
        assertThrows(IllegalArgumentException.class, () -> ConfiguracionUsuario.importar(origen));
        assertFalse(Files.exists(ConfiguracionUsuario.archivo()));
    }

    @Test
    void sinArchivoDevuelveVacio() {
        assertTrue(ConfiguracionUsuario.actual().isEmpty());
    }
}
