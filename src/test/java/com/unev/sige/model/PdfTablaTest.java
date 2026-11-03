package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PdfTablaTest {

    private static String texto(byte[] pdf) {
        return new String(pdf, StandardCharsets.ISO_8859_1);
    }

    @Test
    void generaUnPdfValido() {
        byte[] pdf = PdfTabla.generar("Auditoría", "Prueba", List.of("A", "B"),
                List.<String[]>of(new String[] {"uno", "dos"}));
        String t = texto(pdf);
        assertTrue(t.startsWith("%PDF-1.4"));
        assertTrue(t.trim().endsWith("%%EOF"));
        assertTrue(t.contains("/Count 1"));
    }

    @Test
    void escapaParentesisYBarras() {
        assertEquals("(a\\(b\\)\\\\c)", PdfTabla.cadena("a(b)\\c"));
        assertEquals("(Per\\355odo)", PdfTabla.cadena("Período"));
    }

    @Test
    void recortaTextoLargo() {
        String ajustado = PdfTabla.ajustar("x".repeat(500), 60);
        assertTrue(ajustado.length() < 20 && ajustado.endsWith("…"));
        assertEquals("", PdfTabla.ajustar(null, 60));
    }

    @Test
    void paginaCuandoHayMuchasFilas() {
        List<String[]> filas = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            filas.add(new String[] {"f" + i, "x"});
        }
        String t = texto(PdfTabla.generar("T", "S", List.of("A", "B"), filas));
        int paginas = PdfTabla.filasPorPagina(true) + 0 >= 100 ? 1 : 1 + (int) Math.ceil((100.0 - PdfTabla.filasPorPagina(true)) / PdfTabla.filasPorPagina(false));
        assertTrue(t.contains("/Count " + paginas));
        assertTrue(paginas > 1);
    }

    @Test
    void sinFilasGeneraUnaPagina() {
        assertTrue(texto(PdfTabla.generar("T", "S", List.of("A"), List.of())).contains("/Count 1"));
    }
}
