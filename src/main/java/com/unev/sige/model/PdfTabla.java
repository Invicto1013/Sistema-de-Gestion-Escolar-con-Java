package com.unev.sige.model;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera un PDF de una tabla (A4 horizontal, encabezado repetido, numero de pagina) sin librerias externas.
 * Usa la fuente estandar Helvetica con codificacion WinAnsi, asi que los acentos y la enie se ven bien.
 */
public final class PdfTabla {

    private static final double ANCHO = 842, ALTO = 595, MARGEN = 36;
    private static final double TAM = 8.5, INTERLINEA = 15;
    private static final double ANCHO_CARACTER = TAM * 0.6; // promedio de Helvetica (mayusculas y guiones bajos son mas anchos)
    private static final Charset WIN = Charset.forName("windows-1252");

    private PdfTabla() {
    }

    /** Filas por pagina segun el area util (despues del titulo en la primera pagina). */
    static int filasPorPagina(boolean primera) {
        double util = ALTO - 2 * MARGEN - (primera ? 52 : 20) - 20;
        return Math.max(1, (int) (util / INTERLINEA));
    }

    /** Recorta con puntos suspensivos para que el texto quepa en el ancho dado. */
    static String ajustar(String texto, double ancho) {
        String t = texto == null ? "" : texto.replaceAll("[\\r\\n\\t]+", " ").trim();
        int max = Math.max(1, (int) (ancho / ANCHO_CARACTER));
        return t.length() <= max ? t : t.substring(0, Math.max(0, max - 1)) + "…";
    }

    /** Anchos de columna proporcionales al contenido (con minimo y maximo razonables). */
    static double[] anchos(List<String> encabezados, List<String[]> filas) {
        int n = encabezados.size();
        double[] peso = new double[n];
        for (int i = 0; i < n; i++) {
            int mayor = encabezados.get(i).length();
            for (String[] f : filas) {
                String v = i < f.length && f[i] != null ? f[i] : "";
                mayor = Math.max(mayor, Math.min(v.length(), 40));
            }
            peso[i] = Math.max(8, mayor + 3);
        }
        double suma = 0;
        for (double p : peso) {
            suma += p;
        }
        double util = ANCHO - 2 * MARGEN;
        double[] ancho = new double[n];
        for (int i = 0; i < n; i++) {
            ancho[i] = util * peso[i] / suma;
        }
        return ancho;
    }

    static String cadena(String texto) {
        byte[] bytes = (texto == null ? "" : texto).getBytes(WIN);
        StringBuilder sb = new StringBuilder("(");
        for (byte b : bytes) {
            int c = b & 0xFF;
            if (c == '(' || c == ')' || c == '\\') {
                sb.append('\\').append((char) c);
            } else if (c < 32 || c > 126) {
                sb.append(String.format("\\%03o", c));
            } else {
                sb.append((char) c);
            }
        }
        return sb.append(")").toString();
    }

    public static byte[] generar(String titulo, String subtitulo, List<String> encabezados, List<String[]> filas) {
        double[] ancho = anchos(encabezados, filas);
        List<String> paginas = new ArrayList<>();
        int indice = 0;
        int totalPaginas = 1;
        {
            int restantes = filas.size() - filasPorPagina(true);
            while (restantes > 0) {
                totalPaginas++;
                restantes -= filasPorPagina(false);
            }
        }
        for (int pagina = 1; pagina <= totalPaginas; pagina++) {
            boolean primera = pagina == 1;
            StringBuilder c = new StringBuilder();
            double y = ALTO - MARGEN;
            if (primera) {
                c.append("BT /F2 15 Tf ").append(MARGEN).append(' ').append(y - 12).append(" Td ").append(cadena(titulo)).append(" Tj ET\n");
                c.append("BT /F1 9 Tf 0.35 g ").append(MARGEN).append(' ').append(y - 28).append(" Td ").append(cadena(subtitulo)).append(" Tj ET 0 g\n");
                y -= 52;
            } else {
                y -= 8;
            }
            // Encabezado de la tabla.
            c.append("0.85 0.93 0.91 rg ").append(MARGEN).append(' ').append(y - 4).append(' ').append(ANCHO - 2 * MARGEN).append(' ').append(INTERLINEA).append(" re f 0 g\n");
            double x = MARGEN;
            for (int i = 0; i < encabezados.size(); i++) {
                c.append("BT /F2 ").append(TAM).append(" Tf ").append(x + 3).append(' ').append(y).append(" Td ")
                        .append(cadena(ajustar(encabezados.get(i), ancho[i] - 6))).append(" Tj ET\n");
                x += ancho[i];
            }
            y -= INTERLINEA;
            int cupo = filasPorPagina(primera);
            for (int k = 0; k < cupo && indice < filas.size(); k++, indice++) {
                String[] fila = filas.get(indice);
                if (k % 2 == 1) {
                    c.append("0.96 0.97 0.98 rg ").append(MARGEN).append(' ').append(y - 4).append(' ').append(ANCHO - 2 * MARGEN).append(' ').append(INTERLINEA).append(" re f 0 g\n");
                }
                x = MARGEN;
                for (int i = 0; i < encabezados.size(); i++) {
                    String v = i < fila.length ? fila[i] : "";
                    c.append("BT /F1 ").append(TAM).append(" Tf ").append(x + 3).append(' ').append(y).append(" Td ")
                            .append(cadena(ajustar(v, ancho[i] - 6))).append(" Tj ET\n");
                    x += ancho[i];
                }
                y -= INTERLINEA;
            }
            c.append("BT /F1 8 Tf 0.45 g ").append(MARGEN).append(' ').append(MARGEN - 14).append(" Td ")
                    .append(cadena("SIGE · " + filas.size() + " registro(s)")).append(" Tj ET\n");
            c.append("BT /F1 8 Tf ").append(ANCHO - MARGEN - 70).append(' ').append(MARGEN - 14).append(" Td ")
                    .append(cadena("Página " + pagina + " de " + totalPaginas)).append(" Tj ET 0 g\n");
            paginas.add(c.toString());
        }
        return ensamblar(paginas);
    }

    private static byte[] ensamblar(List<String> contenidos) {
        int n = contenidos.size();
        // Objetos: 1 catalogo, 2 paginas, 3 Helvetica, 4 Helvetica-Bold, luego (pagina, contenido) por pagina.
        List<byte[]> objetos = new ArrayList<>();
        objetos.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.ISO_8859_1));
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < n; i++) {
            kids.append(5 + 2 * i).append(" 0 R ");
        }
        objetos.add(("<< /Type /Pages /Kids [" + kids + "] /Count " + n + " >>").getBytes(StandardCharsets.ISO_8859_1));
        objetos.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>".getBytes(StandardCharsets.ISO_8859_1));
        objetos.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>".getBytes(StandardCharsets.ISO_8859_1));
        for (int i = 0; i < n; i++) {
            objetos.add(("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + (int) ANCHO + " " + (int) ALTO
                    + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + (6 + 2 * i) + " 0 R >>")
                    .getBytes(StandardCharsets.ISO_8859_1));
            byte[] flujo = contenidos.get(i).getBytes(StandardCharsets.ISO_8859_1);
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            o.writeBytes(("<< /Length " + flujo.length + " >>\nstream\n").getBytes(StandardCharsets.ISO_8859_1));
            o.writeBytes(flujo);
            o.writeBytes("\nendstream".getBytes(StandardCharsets.ISO_8859_1));
            objetos.add(o.toByteArray());
        }
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.writeBytes("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
        int[] pos = new int[objetos.size()];
        for (int i = 0; i < objetos.size(); i++) {
            pos[i] = pdf.size();
            pdf.writeBytes(((i + 1) + " 0 obj\n").getBytes(StandardCharsets.ISO_8859_1));
            pdf.writeBytes(objetos.get(i));
            pdf.writeBytes("\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        }
        int xref = pdf.size();
        StringBuilder x = new StringBuilder("xref\n0 " + (objetos.size() + 1) + "\n0000000000 65535 f \n");
        for (int p : pos) {
            x.append(String.format("%010d 00000 n \n", p));
        }
        x.append("trailer\n<< /Size ").append(objetos.size() + 1).append(" /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        pdf.writeBytes(x.toString().getBytes(StandardCharsets.ISO_8859_1));
        return pdf.toByteArray();
    }
}
