package com.unev.sige.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

/**
 * Entrega conexiones JDBC a MySQL (sige_db) a traves de un pool HikariCP.
 * El pool se crea la primera vez que se pide una conexion y se reutiliza despues: abrir una
 * conexion TLS nueva contra un MySQL en la nube en cada consulta es lento y falla mas.
 * Tamano maximo: variable SIGE_DB_POOL_SIZE (por defecto 5, entre 1 y 20).
 * Las credenciales NO estan en el codigo. Se buscan, en este orden:
 * 1) variables de entorno SIGE_DB_URL, SIGE_DB_USER y SIGE_DB_PASSWORD;
 * 2) archivo config.properties (o db.properties) en la carpeta desde donde se ejecuta la aplicacion
 *    o, si no esta alli, en la carpeta donde esta el .jar;
 * 3) archivo dentro del classpath (solo desarrollo; el JAR de entrega no lo incluye).
 * config.properties y db.properties estan en .gitignore; usa config.example.properties como plantilla.
 */
public final class ConexionBD {

    /** Nombres aceptados, en orden de prioridad (config.properties es el nombre que pide la rubrica). */
    private static final String[] ARCHIVOS = {"config.properties", "db.properties"};
    private static final Properties PROPIEDADES = cargarPropiedades();

    private ConexionBD() {
    }

    /** Carpeta donde esta el .jar (o las clases, si se ejecuta desde el IDE); null si no se puede determinar. */
    private static Path carpetaDeLaAplicacion() {
        try {
            Path origen = Path.of(ConexionBD.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return Files.isDirectory(origen) ? origen : origen.getParent();
        } catch (Exception e) {
            return null;
        }
    }

    private static Properties cargarPropiedades() {
        Properties props = new Properties();

        // 1) Carpeta desde donde se ejecuta; 2) carpeta del .jar (doble clic, jpackage, acceso directo).
        List<Path> carpetas = new ArrayList<>();
        carpetas.add(Path.of("").toAbsolutePath());
        Path app = carpetaDeLaAplicacion();
        if (app != null) {
            carpetas.add(app);
        }
        for (Path carpeta : carpetas) {
            for (String archivo : ARCHIVOS) {
                Path ruta = carpeta.resolve(archivo);
                if (Files.exists(ruta)) {
                    try {
                        leer(props, Files.readAllBytes(ruta));
                        return props;
                    } catch (IOException e) {
                        System.err.println("No se pudo leer " + ruta + ": " + e.getMessage());
                    }
                }
            }
        }

        // 3) Dentro del classpath (solo para desarrollo; el JAR de entrega no lo incluye).
        for (String archivo : ARCHIVOS) {
            try (InputStream in = ConexionBD.class.getResourceAsStream("/" + archivo)) {
                if (in != null) {
                    leer(props, in.readAllBytes());
                    return props;
                }
            } catch (IOException e) {
                System.err.println("No se pudo leer " + archivo + " del classpath: " + e.getMessage());
            }
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

    private static final int POOL_POR_DEFECTO = 5;
    private static final int POOL_MAXIMO = 20;
    private static volatile HikariDataSource pool;
    private static boolean cierreRegistrado;

    /** Interpreta el tamano del pool: valor invalido o vacio = 5; se limita al rango 1 a 20. */
    static int tamanoPool(String texto) {
        if (texto == null || texto.isBlank()) {
            return POOL_POR_DEFECTO;
        }
        try {
            int n = Integer.parseInt(texto.trim());
            return Math.max(1, Math.min(POOL_MAXIMO, n));
        } catch (NumberFormatException e) {
            return POOL_POR_DEFECTO;
        }
    }

    private static HikariDataSource crearPool() throws SQLException {
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
                    + "No se encontro un config.properties valido en la carpeta de ejecucion ("
                    + Path.of("").toAbsolutePath() + ") ni junto al programa. "
                    + "Copia config.example.properties como config.properties en la carpeta "
                    + "del .jar y completa los datos de MySQL.");
        }

        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("SIGE-Pool");
        cfg.setJdbcUrl(url);
        cfg.setUsername(usuario);
        cfg.setPassword(clave == null ? "" : clave);
        int maximo = tamanoPool(valor("SIGE_DB_POOL_SIZE", "db.pool.size"));
        cfg.setMaximumPoolSize(maximo);
        cfg.setMinimumIdle(Math.min(1, maximo));
        cfg.setConnectionTimeout(10_000);      // espera maxima por una conexion libre
        cfg.setValidationTimeout(5_000);
        cfg.setIdleTimeout(300_000);           // cierra conexiones sin uso tras 5 min
        cfg.setKeepaliveTime(120_000);         // evita que el servidor cierre conexiones ociosas
        cfg.setMaxLifetime(1_500_000);         // renueva cada 25 min (antes de cortes del servidor)
        cfg.setInitializationFailTimeout(-1);  // la app abre aunque la base este dormida; falla al consultar
        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "100");
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return new HikariDataSource(cfg);
    }

    private static HikariDataSource pool() throws SQLException {
        HikariDataSource actual = pool;
        if (actual != null && !actual.isClosed()) {
            return actual;
        }
        synchronized (ConexionBD.class) {
            if (pool == null || pool.isClosed()) {
                pool = crearPool();
                if (!cierreRegistrado) {
                    cierreRegistrado = true;
                    Runtime.getRuntime().addShutdownHook(new Thread(ConexionBD::cerrar, "SIGE-Pool-Cierre"));
                }
            }
            return pool;
        }
    }

    /**
     * Entrega una conexion del pool. Quien la llama debe cerrarla (try-with-resources):
     * close() la devuelve al pool, no la destruye.
     */
    public static Connection obtener() throws SQLException {
        try {
            return pool().getConnection();
        } catch (SQLTransientConnectionException e) {
            throw new SQLException("No se pudo conectar con la base de datos a tiempo. Revisa que MySQL "
                    + "este encendido (en Aiven puede estar apagado por inactividad), la red y las "
                    + "credenciales. Detalle: " + e.getMessage(), e);
        }
    }

    /** Cierra el pool y libera sus conexiones. Se invoca solo al terminar la aplicacion. */
    public static synchronized void cerrar() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }

    /** Resumen del estado del pool, util para diagnostico y para el informe. */
    public static String estadoPool() {
        HikariDataSource actual = pool;
        if (actual == null || actual.isClosed()) {
            return "Pool sin iniciar";
        }
        HikariPoolMXBean m = actual.getHikariPoolMXBean();
        if (m == null) {
            return "Pool iniciando";
        }
        return "Activas " + m.getActiveConnections() + ", libres " + m.getIdleConnections()
                + ", total " + m.getTotalConnections() + ", esperando " + m.getThreadsAwaitingConnection();
    }

    /** Lee una configuración desde una variable de entorno o config.properties/db.properties. */
    public static String configuracion(String variableEntorno, String clave) {
        return valor(variableEntorno, clave);
    }

    /** Clave AES-256 de 2FA, definida fuera del código por variable de entorno o db.properties. */
    public static String claveCifrado2FA() {
        return valor("SIGE_2FA_ENCRYPTION_KEY", "2fa.encryption-key");
    }
}
