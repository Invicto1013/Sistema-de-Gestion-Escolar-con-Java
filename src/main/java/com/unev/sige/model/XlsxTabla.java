package com.unev.sige.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Genera un libro de Excel (.xlsx) con formato ejecutivo sin librerias externas: titulo, encabezado oscuro,
 * filas alternadas, bordes finos, filtros, fila de encabezado fija, impresion horizontal y una hoja de resumen.
 * Todo el texto se guarda como texto (nunca como formula), asi que el contenido no puede ejecutar nada en Excel.
 */
public final class XlsxTabla {

    /** Un indicador para la hoja de resumen. */
    public record Indicador(String etiqueta, String valor) { }

    // Indices de estilo (ver estilos()).
    private static final int TITULO = 1, SUBTITULO = 2, ENCABEZADO = 3, CUERPO = 4, CUERPO_ALT = 5, OK = 6, DENEGADO = 7,
            ETIQUETA = 8, VALOR = 9, PORCENTAJE = 10, ENTERO = 11, ENTERO_ALT = 12;
    private static final int MAX_ANCHO = 60;

    private XlsxTabla() {
    }

    /**
     * @param columnaResultado indice (desde 0) de la columna que lleva "Correcto" o "Denegado" para colorearla; -1 si no hay.
     * @param resumen          filas de la hoja de resumen: texto y cantidad (se muestra tambien el porcentaje del total).
     */
    public static byte[] generar(String hoja, String titulo, String subtitulo, List<String> encabezados,
                                 List<String[]> filas, int columnaResultado, String tituloResumen,
                                 List<Indicador> indicadores, String encabezadoResumen, List<Object[]> resumen) {
        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(salida, StandardCharsets.UTF_8)) {
                parte(zip, "[Content_Types].xml", tiposDeContenido());
                parte(zip, "_rels/.rels", relacionesRaiz());
                parte(zip, "xl/workbook.xml", libro(hoja, encabezados.size(), filas.size()));
                parte(zip, "xl/_rels/workbook.xml.rels", relacionesLibro());
                parte(zip, "xl/styles.xml", estilos());
                parte(zip, "xl/worksheets/sheet1.xml", hojaRegistros(titulo, subtitulo, encabezados, filas, columnaResultado));
                parte(zip, "xl/worksheets/sheet2.xml", hojaResumen(tituloResumen, subtitulo, indicadores, encabezadoResumen, resumen));
            }
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel.", e);
        }
    }

    // ----- Utilidades -----

    private static void parte(ZipOutputStream zip, String nombre, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    /** Escapa para XML y elimina caracteres de control que Excel no admite. */
    static String xml(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(texto.length() + 8);
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '\t' || c == '\n' || c == '\r' || (c >= 0x20 && c <= 0xD7FF) || (c >= 0xE000 && c <= 0xFFFD)) {
                switch (c) {
                    case '&' -> sb.append("&amp;");
                    case '<' -> sb.append("&lt;");
                    case '>' -> sb.append("&gt;");
                    case '"' -> sb.append("&quot;");
                    case '\'' -> sb.append("&apos;");
                    default -> sb.append(c);
                }
            } else if (Character.isHighSurrogate(c) && i + 1 < texto.length() && Character.isLowSurrogate(texto.charAt(i + 1))) {
                sb.append(c).append(texto.charAt(++i));
            }
        }
        return sb.toString();
    }

    /** Letra de columna: 0 = A, 25 = Z, 26 = AA. */
    static String columna(int indice) {
        StringBuilder sb = new StringBuilder();
        int n = indice;
        do {
            sb.insert(0, (char) ('A' + n % 26));
            n = n / 26 - 1;
        } while (n >= 0);
        return sb.toString();
    }

    /** Digitos sin ceros a la izquierda (hasta 15), para guardarlos como numero y no como texto. */
    static boolean esEntero(String valor) {
        return valor != null && valor.length() <= 15 && valor.matches("[1-9][0-9]*|0");
    }

    private static String celdaTexto(int col, int fila, String texto, int estilo) {
        return "<c r=\"" + columna(col) + fila + "\" s=\"" + estilo + "\" t=\"inlineStr\"><is><t xml:space=\"preserve\">"
                + xml(texto) + "</t></is></c>";
    }

    private static String celdaNumero(int col, int fila, double valor, int estilo) {
        return "<c r=\"" + columna(col) + fila + "\" s=\"" + estilo + "\"><v>" + valor + "</v></c>";
    }

    private static String celdaVacia(int col, int fila, int estilo) {
        return "<c r=\"" + columna(col) + fila + "\" s=\"" + estilo + "\"/>";
    }

    // ----- Partes fijas -----

    private static String tiposDeContenido() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                + "</Types>";
    }

    private static String relacionesRaiz() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private static String relacionesLibro() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>"
                + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
                + "</Relationships>";
    }

    private static String libro(String hoja, int columnas, int filas) {
        String nombre = xml(hoja);
        int ultima = 4 + Math.max(1, filas);
        String rango = "'" + nombre + "'!$A$4:$" + columna(columnas - 1) + "$" + ultima;
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<bookViews><workbookView activeTab=\"0\"/></bookViews>"
                + "<sheets><sheet name=\"" + nombre + "\" sheetId=\"1\" r:id=\"rId1\"/><sheet name=\"Resumen\" sheetId=\"2\" r:id=\"rId2\"/></sheets>"
                + "<definedNames>"
                + "<definedName name=\"_xlnm._FilterDatabase\" localSheetId=\"0\" hidden=\"1\">" + rango + "</definedName>"
                + "<definedName name=\"_xlnm.Print_Titles\" localSheetId=\"0\">'" + nombre + "'!$4:$4</definedName>"
                + "</definedNames></workbook>";
    }

    private static String estilos() {
        String borde = "<border><left style=\"thin\"><color rgb=\"FFD0D7E2\"/></left><right style=\"thin\"><color rgb=\"FFD0D7E2\"/></right>"
                + "<top style=\"thin\"><color rgb=\"FFD0D7E2\"/></top><bottom style=\"thin\"><color rgb=\"FFD0D7E2\"/></bottom><diagonal/></border>";
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<fonts count=\"8\">"
                + "<font><sz val=\"10\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"16\"/><color rgb=\"FF1F3A5F\"/><name val=\"Calibri\"/></font>"
                + "<font><i/><sz val=\"9\"/><color rgb=\"FF666666\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"10\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"10\"/><color rgb=\"FF1B6B3A\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"10\"/><color rgb=\"FF9B1C1C\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"10\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"12\"/><color rgb=\"FF1F3A5F\"/><name val=\"Calibri\"/></font>"
                + "</fonts>"
                + "<fills count=\"6\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1F3A5F\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF3F6FA\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE3F4EA\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFBE4E4\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>"
                + "<borders count=\"2\"><border><left/><right/><top/><bottom/><diagonal/></border>" + borde + "</borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"13\">"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"><alignment vertical=\"center\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"><alignment vertical=\"center\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"4\" fillId=\"4\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"top\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"5\" fillId=\"5\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"top\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"6\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"center\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"7\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"left\" vertical=\"center\"/></xf>"
                + "<xf numFmtId=\"10\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf>"
                + "<xf numFmtId=\"1\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf>"
                + "<xf numFmtId=\"1\" fontId=\"0\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"top\"/></xf>"
                + "</cellXfs>"
                + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>";
    }

    // ----- Hojas -----

    private static double[] anchos(List<String> encabezados, List<String[]> filas) {
        double[] ancho = new double[encabezados.size()];
        for (int i = 0; i < ancho.length; i++) {
            int mayor = encabezados.get(i).length() + 2;
            for (String[] f : filas) {
                String v = i < f.length && f[i] != null ? f[i] : "";
                mayor = Math.max(mayor, Math.min(v.length(), MAX_ANCHO));
            }
            ancho[i] = Math.min(MAX_ANCHO, Math.max(8, mayor + 2));
        }
        return ancho;
    }

    private static String hojaRegistros(String titulo, String subtitulo, List<String> encabezados, List<String[]> filas, int columnaResultado) {
        int n = encabezados.size();
        int ultima = 4 + Math.max(1, filas.size());
        double[] ancho = anchos(encabezados, filas);
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr>"
                + "<dimension ref=\"A1:" + columna(n - 1) + ultima + "\"/>"
                + "<sheetViews><sheetView showGridLines=\"0\" tabSelected=\"1\" workbookViewId=\"0\">"
                + "<pane ySplit=\"4\" topLeftCell=\"A5\" activePane=\"bottomLeft\" state=\"frozen\"/>"
                + "<selection pane=\"bottomLeft\" activeCell=\"A5\" sqref=\"A5\"/></sheetView></sheetViews>"
                + "<sheetFormatPr defaultRowHeight=\"15\"/><cols>");
        for (int i = 0; i < n; i++) {
            sb.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1).append("\" width=\"").append(ancho[i]).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols><sheetData>");
        sb.append("<row r=\"1\" ht=\"28\" customHeight=\"1\">").append(celdaTexto(0, 1, titulo, TITULO)).append("</row>");
        sb.append("<row r=\"2\" ht=\"16\" customHeight=\"1\">").append(celdaTexto(0, 2, subtitulo, SUBTITULO)).append("</row>");
        sb.append("<row r=\"3\" ht=\"6\" customHeight=\"1\"/>");
        sb.append("<row r=\"4\" ht=\"22\" customHeight=\"1\">");
        for (int i = 0; i < n; i++) {
            sb.append(celdaTexto(i, 4, encabezados.get(i), ENCABEZADO));
        }
        sb.append("</row>");
        int fila = 5;
        for (int k = 0; k < filas.size(); k++, fila++) {
            String[] datos = filas.get(k);
            boolean alterna = k % 2 == 1;
            sb.append("<row r=\"").append(fila).append("\">");
            for (int i = 0; i < n; i++) {
                String valor = i < datos.length && datos[i] != null ? datos[i] : "";
                int estilo = alterna ? CUERPO_ALT : CUERPO;
                if (i == columnaResultado) {
                    estilo = "Denegado".equalsIgnoreCase(valor) ? DENEGADO : "Correcto".equalsIgnoreCase(valor) ? OK : estilo;
                }
                if (esEntero(valor) && i != columnaResultado) {
                    sb.append(celdaNumero(i, fila, Long.parseLong(valor), alterna ? ENTERO_ALT : ENTERO));
                } else {
                    sb.append(celdaTexto(i, fila, valor, estilo));
                }
            }
            sb.append("</row>");
        }
        if (filas.isEmpty()) {
            sb.append("<row r=\"5\">").append(celdaTexto(0, 5, "Sin registros para los filtros elegidos.", CUERPO)).append("</row>");
        }
        sb.append("</sheetData>");
        sb.append("<autoFilter ref=\"A4:").append(columna(n - 1)).append(ultima).append("\"/>");
        sb.append("<mergeCells count=\"2\"><mergeCell ref=\"A1:").append(columna(n - 1)).append("1\"/><mergeCell ref=\"A2:")
                .append(columna(n - 1)).append("2\"/></mergeCells>");
        sb.append("<pageMargins left=\"0.4\" right=\"0.4\" top=\"0.6\" bottom=\"0.6\" header=\"0.3\" footer=\"0.3\"/>");
        sb.append("<pageSetup paperSize=\"9\" orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/>");
        sb.append("<headerFooter><oddFooter>&amp;LSIGE&amp;RPágina &amp;P de &amp;N</oddFooter></headerFooter>");
        return sb.append("</worksheet>").toString();
    }

    private static String hojaResumen(String titulo, String subtitulo, List<Indicador> indicadores, String encabezado, List<Object[]> resumen) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr>"
                + "<sheetViews><sheetView showGridLines=\"0\" workbookViewId=\"0\"/></sheetViews>"
                + "<sheetFormatPr defaultRowHeight=\"15\"/>"
                + "<cols><col min=\"1\" max=\"1\" width=\"38\" customWidth=\"1\"/><col min=\"2\" max=\"2\" width=\"22\" customWidth=\"1\"/>"
                + "<col min=\"3\" max=\"3\" width=\"16\" customWidth=\"1\"/></cols><sheetData>");
        sb.append("<row r=\"1\" ht=\"28\" customHeight=\"1\">").append(celdaTexto(0, 1, titulo, TITULO)).append("</row>");
        sb.append("<row r=\"2\" ht=\"16\" customHeight=\"1\">").append(celdaTexto(0, 2, subtitulo, SUBTITULO)).append("</row>");
        sb.append("<row r=\"3\" ht=\"8\" customHeight=\"1\"/>");
        int fila = 4;
        for (Indicador i : indicadores) {
            sb.append("<row r=\"").append(fila).append("\" ht=\"20\" customHeight=\"1\">")
                    .append(celdaTexto(0, fila, i.etiqueta(), ETIQUETA)).append(celdaTexto(1, fila, i.valor(), VALOR)).append("</row>");
            fila++;
        }
        fila++;
        sb.append("<row r=\"").append(fila).append("\" ht=\"22\" customHeight=\"1\">")
                .append(celdaTexto(0, fila, encabezado, ENCABEZADO)).append(celdaTexto(1, fila, "Cantidad", ENCABEZADO))
                .append(celdaTexto(2, fila, "% del total", ENCABEZADO)).append("</row>");
        int primera = fila + 1;
        fila++;
        double total = 0;
        for (Object[] r : resumen) {
            total += ((Number) r[1]).doubleValue();
        }
        for (int k = 0; k < resumen.size(); k++, fila++) {
            Object[] r = resumen.get(k);
            double cantidad = ((Number) r[1]).doubleValue();
            sb.append("<row r=\"").append(fila).append("\">").append(celdaTexto(0, fila, String.valueOf(r[0]), k % 2 == 1 ? CUERPO_ALT : CUERPO))
                    .append(celdaNumero(1, fila, cantidad, ENTERO)).append(celdaNumero(2, fila, total == 0 ? 0 : cantidad / total, PORCENTAJE)).append("</row>");
        }
        if (resumen.isEmpty()) {
            sb.append("<row r=\"").append(fila).append("\">").append(celdaTexto(0, fila, "Sin datos", CUERPO)).append(celdaVacia(1, fila, CUERPO))
                    .append(celdaVacia(2, fila, CUERPO)).append("</row>");
        }
        sb.append("</sheetData><mergeCells count=\"2\"><mergeCell ref=\"A1:C1\"/><mergeCell ref=\"A2:C2\"/></mergeCells>");
        sb.append("<pageMargins left=\"0.5\" right=\"0.5\" top=\"0.6\" bottom=\"0.6\" header=\"0.3\" footer=\"0.3\"/>");
        sb.append("<pageSetup paperSize=\"9\" orientation=\"portrait\" fitToWidth=\"1\" fitToHeight=\"0\"/>");
        return sb.append("</worksheet>").toString();
    }

    /** Para pruebas: lista de partes esperadas del paquete. */
    static List<String> partes() {
        return new ArrayList<>(List.of("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/_rels/workbook.xml.rels",
                "xl/styles.xml", "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml"));
    }
}
