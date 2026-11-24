package com.unev.sige.data;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Las tablas de las herramientas escolares deben estar en el script de fase y en el esquema completo. */
class ScriptsSqlHerramientasTest {

    private static String leer(String nombre) throws Exception {
        return Files.readString(Path.of("sql", nombre), StandardCharsets.UTF_8);
    }

    @Test
    void laFaseCreaLasDosTablasDeFormaIdempotente() throws Exception {
        String s = leer("fase_herramientas_escolares.sql");
        assertTrue(s.contains("CREATE TABLE IF NOT EXISTS herramienta_registros"));
        assertTrue(s.contains("CREATE TABLE IF NOT EXISTS herramienta_registro_datos"));
        assertTrue(!s.toUpperCase().contains("DROP TABLE"), "la fase no debe borrar tablas");
        assertTrue(s.contains("utf8mb4"));
    }

    @Test
    void elEsquemaCompletoTambienLasIncluye() throws Exception {
        String s = leer("sige_completo.sql");
        assertTrue(s.contains("CREATE TABLE IF NOT EXISTS herramienta_registros"));
        assertTrue(s.contains("CREATE TABLE IF NOT EXISTS herramienta_registro_datos"));
    }

    @Test
    void elBorradoDeUsuariosLimpiaLasTablasNuevas() throws Exception {
        String s = Files.readString(Path.of("src/main/java/com/unev/sige/data/EliminacionUsuarioDAO.java"), StandardCharsets.UTF_8);
        assertTrue(s.contains("DELETE FROM herramienta_registro_datos"));
        assertTrue(s.contains("DELETE FROM herramienta_registros"));
    }
}
