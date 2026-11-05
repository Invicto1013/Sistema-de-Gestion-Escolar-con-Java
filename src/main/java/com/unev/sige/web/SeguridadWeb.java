package com.unev.sige.web;

import com.sun.net.httpserver.Headers;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Reglas de seguridad reutilizables y probables sin servidor: tipos de archivo permitidos y cabeceras. */
final class SeguridadWeb {

    private SeguridadWeb() {
    }

    /** Extensiones de documentos escolares permitidas en el Aula. Todo lo demas se rechaza. */
    static final Set<String> EXTENSIONES = Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp",
            "txt", "csv", "md", "rtf", "png", "jpg", "jpeg", "gif", "zip");

    /** Cabecera de firma (primeros bytes) que debe tener cada formato binario. Los de texto no la exigen. */
    private static final Map<String, byte[][]> FIRMAS = Map.ofEntries(
            Map.entry("pdf", new byte[][] {{'%', 'P', 'D', 'F'}}),
            Map.entry("png", new byte[][] {{(byte) 0x89, 'P', 'N', 'G'}}),
            Map.entry("jpg", new byte[][] {{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}}),
            Map.entry("jpeg", new byte[][] {{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}}),
            Map.entry("gif", new byte[][] {{'G', 'I', 'F', '8'}}),
            Map.entry("zip", new byte[][] {{'P', 'K'}}),
            Map.entry("docx", new byte[][] {{'P', 'K'}}),
            Map.entry("xlsx", new byte[][] {{'P', 'K'}}),
            Map.entry("pptx", new byte[][] {{'P', 'K'}}),
            Map.entry("odt", new byte[][] {{'P', 'K'}}),
            Map.entry("ods", new byte[][] {{'P', 'K'}}),
            Map.entry("odp", new byte[][] {{'P', 'K'}}),
            Map.entry("doc", new byte[][] {{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}}),
            Map.entry("xls", new byte[][] {{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}}),
            Map.entry("ppt", new byte[][] {{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}}));

    static String extension(String nombre) {
        if (nombre == null) {
            return "";
        }
        int punto = nombre.lastIndexOf('.');
        return punto < 0 || punto == nombre.length() - 1 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT).trim();
    }

    /** Devuelve null si el archivo es aceptable o el motivo del rechazo para mostrarlo al usuario. */
    static String validarArchivo(String nombre, byte[] datos) {
        String ext = extension(nombre);
        if (!EXTENSIONES.contains(ext)) {
            return "Tipo de archivo no permitido. Usa PDF, Word, Excel, PowerPoint, texto, imagen (PNG, JPG, GIF) o ZIP.";
        }
        // Un nombre con doble extension como "tarea.pdf.exe" ya cae arriba; aqui se evita "tarea.exe.pdf" disfrazado.
        byte[][] firmas = FIRMAS.get(ext);
        if (firmas != null && datos != null) {
            boolean coincide = false;
            for (byte[] firma : firmas) {
                if (empieza(datos, firma)) {
                    coincide = true;
                    break;
                }
            }
            if (!coincide) {
                return "El contenido del archivo no corresponde a su extensión ." + ext + ".";
            }
        }
        return null;
    }

    private static boolean empieza(byte[] datos, byte[] firma) {
        if (datos.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if (datos[i] != firma[i]) {
                return false;
            }
        }
        return true;
    }

    /** Cabeceras defensivas comunes. HSTS solo si la conexion llega por HTTPS (directa o tras un proxy de confianza). */
    static void aplicarCabeceras(Headers h, boolean https) {
        h.set("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=(), usb=()");
        h.set("Cross-Origin-Opener-Policy", "same-origin");
        h.set("Cross-Origin-Resource-Policy", "same-origin");
        if (https) {
            h.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
    }
}
