package com.unev.sige.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Reglas de busqueda del archivo de configuracion: config.properties manda, db.properties es el antiguo. */
class ConexionBDConfiguracionTest {
    private static Path carpeta() throws Exception {
        return Files.createTempDirectory("sige-config");
    }

    @Test
    void sinArchivosDevuelveNull() throws Exception {
        assertNull(ConexionBD.localizar(List.of(carpeta())));
        assertNull(ConexionBD.localizar(List.of()));
    }

    @Test
    void usaConfigPropertiesCuandoExiste() throws Exception {
        Path c = carpeta();
        Files.writeString(c.resolve("config.properties"), "db.url=x");
        assertEquals(c.resolve("config.properties"), ConexionBD.localizar(List.of(c)));
    }

    @Test
    void conLosDosArchivosGanaConfigProperties() throws Exception {
        Path c = carpeta();
        Files.writeString(c.resolve("db.properties"), "db.url=viejo");
        Files.writeString(c.resolve("config.properties"), "db.url=nuevo");
        assertEquals(c.resolve("config.properties"), ConexionBD.localizar(List.of(c)));
    }

    @Test
    void elNombreAntiguoSigueFuncionandoSiEsElUnico() throws Exception {
        Path c = carpeta();
        Files.writeString(c.resolve("db.properties"), "db.url=viejo");
        assertEquals(c.resolve("db.properties"), ConexionBD.localizar(List.of(c)));
    }

    @Test
    void recorreLasCarpetasEnOrden() throws Exception {
        Path primera = carpeta();
        Path segunda = carpeta();
        Files.writeString(segunda.resolve("config.properties"), "db.url=x");
        assertEquals(segunda.resolve("config.properties"), ConexionBD.localizar(List.of(primera, segunda)));
        Files.writeString(primera.resolve("db.properties"), "db.url=y");
        assertEquals(primera.resolve("db.properties"), ConexionBD.localizar(List.of(primera, segunda)));
    }
}
