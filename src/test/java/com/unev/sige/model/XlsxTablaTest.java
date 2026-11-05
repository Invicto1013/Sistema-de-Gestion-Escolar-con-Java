package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

class XlsxTablaTest {

    private static byte[] libro(List<String[]> filas) {
        return XlsxTabla.generar("Registros", "Título", "Subtítulo", List.of("ID", "Texto", "Resultado"), filas, 2,
                "Resumen", List.of(new XlsxTabla.Indicador("Total", String.valueOf(filas.size()))), "Acción",
                List.<Object[]>of(new Object[] {"A", 2}, new Object[] {"B", 2}));
    }

    private static String parte(byte[] zip, String nombre) throws Exception {
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                if (e.getName().equals(nombre)) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }

    @Test
    void contieneTodasLasPartesDelPaquete() throws Exception {
        byte[] zip = libro(List.<String[]>of(new String[] {"1", "a", "Correcto"}));
        for (String p : XlsxTabla.partes()) {
            assertTrue(parte(zip, p) != null && !parte(zip, p).isEmpty(), "falta " + p);
        }
    }

    @Test
    void escapaXmlYNoGeneraFormulas() throws Exception {
        String hoja = parte(libro(List.<String[]>of(new String[] {"1", "<b>&\"x\" =SUM(A1)", "Denegado"})), "xl/worksheets/sheet1.xml");
        assertTrue(hoja.contains("&lt;b&gt;&amp;&quot;x&quot; =SUM(A1)"));
        assertFalse(hoja.contains("<f>"), "no debe haber formulas");
        assertTrue(hoja.contains("t=\"inlineStr\""));
    }

    @Test
    void eliminaCaracteresDeControl() {
        assertEquals("ab", XlsxTabla.xml("a\u0000\u0007b"));
        assertEquals("ñandú", XlsxTabla.xml("ñandú"));
    }

    @Test
    void letrasDeColumna() {
        assertEquals("A", XlsxTabla.columna(0));
        assertEquals("J", XlsxTabla.columna(9));
        assertEquals("Z", XlsxTabla.columna(25));
        assertEquals("AA", XlsxTabla.columna(26));
    }

    @Test
    void detectaEnterosParaGuardarlosComoNumero() {
        assertTrue(XlsxTabla.esEntero("123"));
        assertTrue(XlsxTabla.esEntero("0"));
        assertFalse(XlsxTabla.esEntero("007"));
        assertFalse(XlsxTabla.esEntero("12a"));
        assertFalse(XlsxTabla.esEntero(""));
        assertFalse(XlsxTabla.esEntero(null));
    }

    @Test
    void sinFilasMuestraUnMensaje() throws Exception {
        String hoja = parte(libro(new ArrayList<>()), "xl/worksheets/sheet1.xml");
        assertTrue(hoja.contains("Sin registros"));
    }

    @Test
    void coloreaDenegadosYCorrectos() throws Exception {
        String hoja = parte(libro(List.<String[]>of(new String[] {"1", "a", "Denegado"}, new String[] {"2", "b", "Correcto"})), "xl/worksheets/sheet1.xml");
        assertTrue(hoja.contains("s=\"7\" t=\"inlineStr\"><is><t xml:space=\"preserve\">Denegado"));
        assertTrue(hoja.contains("s=\"6\" t=\"inlineStr\"><is><t xml:space=\"preserve\">Correcto"));
    }
}
