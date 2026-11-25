package com.unev.sige.data;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Configuración de conexión propia de cada equipo, fuera del ejecutable y del repositorio.
 *
 * <p>El ejecutable ya no lleva credenciales. La primera vez que se abre en un equipo, un asistente pide los
 * datos y los guarda aquí: {@code %APPDATA%\SIGE\config.properties} en Windows, o
 * {@code ~/.config/sige/config.properties} (o {@code $XDG_CONFIG_HOME/sige}) en otros sistemas.
 * La propiedad de sistema {@code sige.config.dir} reemplaza la carpeta (pruebas y despliegues especiales).
 * No depende de JavaFX ni del pool, así que se prueba sin base de datos.
 */
public final class ConfiguracionUsuario {

    public static final String ARCHIVO = "config.properties";
    public static final String CLAVE_URL = "db.url";
    public static final String CLAVE_USUARIO = "db.user";
    public static final String CLAVE_PASSWORD = "db.password";
    public static final String CLAVE_2FA = "2fa.encryption-key";

    private ConfiguracionUsuario() {
    }

    /** Carpeta de configuración del usuario actual. */
    public static Path carpeta() {
        String forzada = System.getProperty("sige.config.dir");
        if (forzada != null && !forzada.isBlank()) {
            return Path.of(forzada.trim());
        }
        String appdata = System.getenv("APPDATA");
        if (appdata != null && !appdata.isBlank()) {
            return Path.of(appdata, "SIGE");
        }
        String xdg = System.getenv("XDG_CONFIG_HOME");
        if (xdg != null && !xdg.isBlank()) {
            return Path.of(xdg, "sige");
        }
        return Path.of(System.getProperty("user.home", "."), ".config", "sige");
    }

    public static Path archivo() {
        return carpeta().resolve(ARCHIVO);
    }

    /** Errores de los datos de conexión (lista vacía = válidos). Los mensajes no incluyen los valores. */
    public static List<String> validar(String url, String usuario, String clave2fa) {
        List<String> errores = new ArrayList<>();
        String u = url == null ? "" : url.trim();
        if (u.isEmpty()) {
            errores.add("Escribe la dirección de la base de datos (db.url).");
        } else if (!u.startsWith("jdbc:mysql://")) {
            errores.add("La dirección debe empezar por jdbc:mysql://");
        } else if (u.length() <= "jdbc:mysql://".length() || u.contains(" ")) {
            errores.add("La dirección de la base de datos no es válida (sin espacios y con servidor).");
        }
        if (usuario == null || usuario.isBlank()) {
            errores.add("Escribe el usuario de la base de datos.");
        }
        if (clave2fa != null && !clave2fa.isBlank()) {
            try {
                int bytes = java.util.Base64.getDecoder().decode(clave2fa.trim()).length;
                if (bytes != 32) {
                    errores.add("La clave 2FA debe ser Base64 de 32 bytes (AES-256).");
                }
            } catch (IllegalArgumentException e) {
                errores.add("La clave 2FA no es Base64 válido.");
            }
        }
        return errores;
    }

    /** Lee un archivo de propiedades en UTF-8, ignorando la marca BOM de los editores de Windows. */
    public static Properties leer(Path origen) throws IOException {
        String texto = new String(Files.readAllBytes(origen), StandardCharsets.UTF_8);
        if (texto.startsWith("﻿")) {
            texto = texto.substring(1);
        }
        Properties p = new Properties();
        p.load(new StringReader(texto));
        return p;
    }

    /** Propiedades guardadas en este equipo; vacío si todavía no hay archivo. */
    public static Properties actual() {
        try {
            return Files.exists(archivo()) ? leer(archivo()) : new Properties();
        } catch (IOException e) {
            return new Properties();
        }
    }

    /**
     * Guarda los valores en el archivo del usuario, conservando las demás claves que ya tuviera (correo, etc.).
     * Un valor nulo o vacío no sobrescribe uno existente. La escritura es atómica y, donde el sistema lo permite,
     * el archivo queda legible solo por su dueño.
     */
    public static Path guardar(Map<String, String> valores) throws IOException {
        Properties props = actual();
        for (Map.Entry<String, String> e : valores.entrySet()) {
            String v = e.getValue();
            if (v != null && !v.isBlank()) {
                props.setProperty(e.getKey(), v.trim());
            }
        }
        StringWriter w = new StringWriter();
        props.store(w, "SIGE - configuracion de este equipo. No la compartas ni la subas a Git.");
        Path carpeta = carpeta();
        Files.createDirectories(carpeta);
        Path destino = archivo();
        Path tmp = Files.createTempFile(carpeta, "config", ".tmp");
        try {
            Files.write(tmp, w.toString().getBytes(StandardCharsets.UTF_8));
            try {
                Files.setPosixFilePermissions(tmp, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException | IOException ignorado) {
                // Windows: la carpeta de %APPDATA% ya es privada del usuario.
            }
            Files.move(tmp, destino, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
        return destino;
    }

    /**
     * Importa un config.properties existente (por ejemplo, el que se usaba en otro equipo).
     * Lanza IllegalArgumentException, sin mostrar valores, si le faltan datos o no son válidos.
     */
    public static Path importar(Path origen) throws IOException {
        Properties p = leer(origen);
        List<String> errores = validar(p.getProperty(CLAVE_URL), p.getProperty(CLAVE_USUARIO), p.getProperty(CLAVE_2FA));
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", errores));
        }
        Map<String, String> todo = new java.util.LinkedHashMap<>();
        for (String k : p.stringPropertyNames()) {
            todo.put(k, p.getProperty(k));
        }
        return guardar(todo);
    }
}
