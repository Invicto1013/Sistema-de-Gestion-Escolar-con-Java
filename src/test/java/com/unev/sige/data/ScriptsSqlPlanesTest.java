package com.unev.sige.data;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ScriptsSqlPlanesTest {

    private static String leer(String n) throws Exception {
        return Files.readString(Path.of("sql", n), StandardCharsets.UTF_8);
    }

    @Test
    void laFaseEsIdempotenteYNoBorra() throws Exception {
        String s = leer("fase_planes_servicio.sql");
        assertTrue(s.contains("CREATE TABLE IF NOT EXISTS suscripcion_centro"));
        assertTrue(s.contains("WHERE NOT EXISTS"), "la semilla no debe duplicarse");
        assertTrue(!s.toUpperCase().contains("DROP TABLE"));
    }

    @Test
    void elEsquemaCompletoLaIncluyeSinUse() throws Exception {
        String s = leer("sige_completo.sql");
        assertTrue(s.contains("CREATE TABLE IF NOT EXISTS suscripcion_centro"));
    }
}
