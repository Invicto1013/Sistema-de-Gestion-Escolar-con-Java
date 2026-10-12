package com.unev.sige.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Entrega conexiones JDBC a MySQL (sige_db).
 * Las credenciales NO estan en el codigo. Se buscan, en este orden:
 * 1) variables de entorno SIGE_DB_URL, SIGE_DB_USER y SIGE_DB_PASSWORD;
 * 2) archivo db.properties en la carpeta desde donde se ejecuta la aplicacion;
 * 3) archivo db.properties dentro del classpath (por ejemplo src/main/resources/db.properties),
 *    que sirve aunque el IDE ejecute desde otra carpeta.
 * db.properties esta en .gitignore; usa db.properties.example como plantilla.
 */
public final class ConexionBD {

    private static final String ARCHIVO = "db.properties";
    private static final Properties PROPIEDADES = cargarPropiedades();

    private ConexionBD() {
    }

    private static Properties cargarPropiedades() {
        Properties props = new Properties();

        Path ruta = Path.of(ARCHIVO).toAbsolutePath();
        if (Files.exists(ruta)) {
            try {
                leer(props, Files.readAllBytes(ruta));
                return props;
            } catch (IOException e) {
                System.err.println("No se pudo leer " + ruta + ": " + e.getMessage());
            }
        }

        try (InputStream in = ConexionBD.class.getResourceAsStream("/" + ARCHIVO)) {
            if (in != null) {
                leer(props, in.readAllBytes());
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer " + ARCHIVO + " del classpath: " + e.getMessage());
        }
        return props;
    }

    /**
     * Lee el contenido como UTF-8 ignorando la marca BOM que algunos editores de Windows
     * agregan al inicio (si no se quita, la primera clave "db.url" no se reconoce).
     */
    private static void leer(Properties props, byte[] contenido) throws IOException {
        String texto = new String(contenido, StandardCharsets.UTF_8);
        if (texto.startsWith("﻿")) {
            texto = texto.substring(1);
        }
        props.load(new StringReader(texto));
    }

    private static String valor(String variableEntorno, String clave) {
        String env = System.getenv(variableEntorno);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String valor = PROPIEDADES.getProperty(clave);
        return valor == null ? null : valor.trim();
    }

    /**
     * Abre una conexion nueva. Quien la llama debe cerrarla (try-with-resources).
     */
    public static Connection obtener() throws SQLException {
        String url = valor("SIGE_DB_URL", "db.url");
        String usuario = valor("SIGE_DB_USER", "db.user");
        String clave = valor("SIGE_DB_PASSWORD", "db.password");

        if (url == null || url.isEmpty() || usuario == null || usuario.isEmpty()) {
            List<String> faltan = new ArrayList<>();
            if (url == null || url.isEmpty()) {
                faltan.add("db.url");
            }
            if (usuario == null || usuario.isEmpty()) {
                faltan.add("db.user");
            }
            throw new SQLException("Falta configurar la conexion (" + String.join(", ", faltan) + "). "
                    + "No se encontro un db.properties valido en la carpeta de ejecucion ("
                    + Path.of("").toAbsolutePath() + ") ni en el classpath. "
                    + "Copia db.properties.example como db.properties en esa carpeta "
                    + "(o en src/main/resources) y completa tus datos de MySQL.");
        }
        return DriverManager.getConnection(url, usuario, clave == null ? "" : clave);
    }
}
