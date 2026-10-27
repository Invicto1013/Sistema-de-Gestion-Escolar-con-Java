package com.unev.sige.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class RegistroLogTest {
    private static String contenido(Path carpeta) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Stream<Path> s = Files.list(carpeta)) {
            for (Path p : s.filter(f -> f.getFileName().toString().endsWith(".log")).toList()) {
                sb.append(Files.readString(p, StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }

    @Test
    void escribeEnArchivoLosMensajesYLasTrazasDeConsolaConAcentos() throws Exception {
        Path carpeta = Files.createTempDirectory("sige-log");
        try {
            RegistroLog.configurar(carpeta, Level.INFO, true);
            Logger.getLogger("sige.prueba").info("Inicio correcto: matrícula añadida");
            Logger.getLogger("sige.prueba").fine("detalle que no debe aparecer");
            new IllegalStateException("fallo de ejemplo").printStackTrace();
            System.err.flush();
        } finally {
            RegistroLog.detener();
        }
        String texto = contenido(carpeta);
        assertTrue(texto.contains("INFO sige.prueba - Inicio correcto: matrícula añadida"));
        assertFalse(texto.contains("detalle que no debe aparecer"));
        assertTrue(texto.contains("WARNING stderr - java.lang.IllegalStateException: fallo de ejemplo"));
        assertTrue(texto.contains("Registro iniciado"));
    }

    @Test
    void sinArchivoNoCreaNingunaCarpeta() throws Exception {
        Path carpeta = Files.createTempDirectory("sige-log").resolve("no-debe-existir");
        try {
            RegistroLog.configurar(carpeta, Level.INFO, false);
        } finally {
            RegistroLog.detener();
        }
        assertFalse(Files.exists(carpeta));
    }

    @Test
    void restauraLaSalidaDeErroresAlDetener() throws Exception {
        java.io.PrintStream antes = System.err;
        Path carpeta = Files.createTempDirectory("sige-log");
        RegistroLog.configurar(carpeta, Level.INFO, true);
        assertFalse(antes == System.err);
        RegistroLog.detener();
        assertTrue(antes == System.err);
    }

    @Test
    void interpretaElNivelYUsaINFOSiEsInvalido() {
        assertEquals(Level.WARNING, RegistroLog.nivel("warning"));
        assertEquals(Level.FINE, RegistroLog.nivel(" fine "));
        assertEquals(Level.INFO, RegistroLog.nivel("nada"));
    }
}
