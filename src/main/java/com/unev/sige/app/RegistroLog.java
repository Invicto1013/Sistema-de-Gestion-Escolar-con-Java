package com.unev.sige.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Registro de eventos de SIGE en archivo, con rotación (5 archivos de hasta 1 MB: logs/sige-0.log ...).
 *
 * Además de lo que se escribe con java.util.logging (incluido HikariCP), copia al archivo todo lo que
 * salga por System.err, es decir, las trazas de printStackTrace() de toda la aplicación, sin tener
 * que modificar cada pantalla. La consola sigue mostrando lo mismo que antes.
 *
 * Configuración (variables de entorno): SIGE_LOG_FILE=false lo desactiva, SIGE_LOG_DIR cambia la carpeta
 * (por defecto "logs") y SIGE_LOG_LEVEL el nivel (por defecto INFO). Nunca se registran contraseñas ni
 * cuerpos de formularios.
 */
public final class RegistroLog {
    private static final Logger RAIZ = Logger.getLogger("");
    private static final Logger ERR = Logger.getLogger("sige.stderr");
    private static boolean iniciado;
    private static FileHandler archivo;
    private static PrintStream errOriginal;

    private RegistroLog() { }

    /** Inicia el registro una sola vez, leyendo la configuración del entorno. */
    public static synchronized void iniciar() {
        if (iniciado) return;
        boolean usarArchivo = !"false".equalsIgnoreCase(env("SIGE_LOG_FILE", "true"));
        configurar(Path.of(env("SIGE_LOG_DIR", "logs")), nivel(env("SIGE_LOG_LEVEL", "INFO")), usarArchivo);
    }

    static synchronized void configurar(Path carpeta, Level nivel, boolean usarArchivo) {
        if (iniciado) return;
        iniciado = true;
        RAIZ.setLevel(nivel);
        if (usarArchivo) {
            try {
                Files.createDirectories(carpeta);
                String patron = carpeta.resolve("sige-%g.log").toString().replace('\\', '/');
                FileHandler fh = new FileHandler(patron, 1_000_000, 5, true);
                fh.setEncoding("UTF-8");
                fh.setFormatter(new Formato());
                fh.setLevel(Level.ALL);
                RAIZ.addHandler(fh);
                ERR.setUseParentHandlers(false);
                ERR.addHandler(fh);
                archivo = fh;
                errOriginal = System.err;
                System.setErr(new PrintStream(new CopiaPorLinea(errOriginal), true, StandardCharsets.UTF_8));
            } catch (IOException | SecurityException e) {
                System.err.println("SIGE: no se pudo crear el archivo de registro (" + e.getMessage()
                        + "). Se usará solo la consola.");
            }
        }
        Thread.setDefaultUncaughtExceptionHandler((hilo, error) ->
                Logger.getLogger("sige").log(Level.SEVERE, "Excepción no controlada en el hilo " + hilo.getName(), error));
        Logger.getLogger("sige").info("Registro iniciado" + (archivo == null ? " (solo consola)" : " en " + carpeta.toAbsolutePath()));
    }

    /** Cierra el archivo y restaura System.err. Solo se usa en las pruebas. */
    static synchronized void detener() {
        if (archivo != null) {
            RAIZ.removeHandler(archivo);
            ERR.removeHandler(archivo);
            archivo.close();
            archivo = null;
        }
        if (errOriginal != null) {
            System.setErr(errOriginal);
            errOriginal = null;
        }
        Thread.setDefaultUncaughtExceptionHandler(null);
        iniciado = false;
    }

    static Level nivel(String texto) {
        try {
            return Level.parse(texto.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Level.INFO;
        }
    }

    private static String env(String clave, String porDefecto) {
        String valor = System.getenv(clave);
        return valor == null || valor.isBlank() ? porDefecto : valor.trim();
    }

    /** Formato de una línea: [fecha hora] NIVEL origen - mensaje (y la traza si hay excepción). */
    static final class Formato extends Formatter {
        private static final DateTimeFormatter FECHA =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

        @Override
        public String format(LogRecord r) {
            String origen = "sige.stderr".equals(r.getLoggerName()) ? "stderr" : String.valueOf(r.getLoggerName());
            StringBuilder sb = new StringBuilder();
            sb.append('[').append(FECHA.format(r.getInstant())).append("] ")
                    .append(r.getLevel().getName()).append(' ').append(origen).append(" - ")
                    .append(formatMessage(r)).append(System.lineSeparator());
            if (r.getThrown() != null) {
                StringWriter sw = new StringWriter();
                r.getThrown().printStackTrace(new PrintWriter(sw));
                sb.append(sw);
            }
            return sb.toString();
        }
    }

    /** Pasa los bytes a la consola original y envía cada línea completa al archivo. */
    private static final class CopiaPorLinea extends OutputStream {
        private final PrintStream destino;
        private final ByteArrayOutputStream linea = new ByteArrayOutputStream();

        CopiaPorLinea(PrintStream destino) {
            this.destino = destino;
        }

        @Override
        public synchronized void write(int b) {
            destino.write(b);
            if (b == '\n') {
                String texto = linea.toString(StandardCharsets.UTF_8).stripTrailing();
                linea.reset();
                if (!texto.isEmpty()) ERR.log(Level.WARNING, texto);
            } else {
                linea.write(b);
            }
        }

        @Override
        public synchronized void flush() {
            destino.flush();
        }
    }
}
