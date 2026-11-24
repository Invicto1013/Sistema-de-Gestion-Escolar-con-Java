package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportePdfHerramientasTest {

    private static RegistroHerramienta registro(int id, String resultado) {
        return new RegistroHerramienta(id, 10, "Ana Gómez", "creditos", "Créditos académicos", resultado,
                LocalDateTime.of(2026, 11, 24, 9, 30),
                List.of(new RegistroHerramienta.Dato("asignaturas", "Asignaturas", "MAT-101;Matemática;4;88\nLEN-101;Lengua;4;72")));
    }

    private static String texto(byte[] pdf) {
        return new String(pdf, StandardCharsets.ISO_8859_1);
    }

    @Test
    void elPdfDeUnRegistroEsUnPdfConElTituloLosDatosYElResultado() {
        byte[] pdf = ReportePdfHerramientas.registro(registro(7, "Créditos aprobados: 8\nPromedio acumulado: 80"));
        String t = texto(pdf);
        assertTrue(t.startsWith("%PDF"));
        assertTrue(t.contains("Registro N.\\272 7") || t.contains("Registro N"));
        assertTrue(t.contains("MAT-101"));
        assertTrue(t.contains("Promedio acumulado: 80"));
        assertTrue(t.contains("RESULTADO"));
        assertTrue(t.contains("DATOS INGRESADOS"));
    }

    @Test
    void lasLineasLargasSePartenSinPerderTexto() {
        String largo = "palabra ".repeat(60).trim();
        List<String> partes = ReportePdfHerramientas.envolver(largo, 70);
        assertTrue(partes.size() > 1);
        assertTrue(partes.stream().allMatch(p -> p.length() <= 70));
        assertEquals(largo, String.join(" ", partes));
        assertEquals(List.of("a", "", "b"), ReportePdfHerramientas.envolver("a\n\nb", 70));
        assertEquals(List.of(""), ReportePdfHerramientas.envolver(null, 70));
        assertEquals(2, ReportePdfHerramientas.envolver("x".repeat(100), 70).size());
    }

    @Test
    void elListadoIncluyeTodosLosRegistrosYVariasPaginas() {
        List<RegistroHerramienta> muchos = new ArrayList<>();
        for (int i = 1; i <= 120; i++) muchos.add(registro(i, "Resultado " + i));
        byte[] pdf = ReportePdfHerramientas.listado("Registros", muchos, true);
        String t = texto(pdf);
        assertTrue(t.startsWith("%PDF"));
        assertTrue(t.contains("120 registro\\(s\\)"));
        assertTrue(t.contains("Resultado 120"));
        assertTrue(t.contains("Autor"));
        assertTrue(!texto(ReportePdfHerramientas.listado("Registros", muchos, false)).contains("Autor"));
    }

    @Test
    void unListadoVacioTambienGeneraPdf() {
        assertTrue(texto(ReportePdfHerramientas.listado("Registros", List.of(), false)).startsWith("%PDF"));
    }

    @Test
    void unResultadoConCaracteresRarosNoRompeElPdf() {
        byte[] pdf = ReportePdfHerramientas.registro(registro(1, "Sí ✓ ñandú (prueba) \\ fin"));
        assertTrue(texto(pdf).startsWith("%PDF"));
    }
}
