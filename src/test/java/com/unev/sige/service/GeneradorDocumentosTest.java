package com.unev.sige.service;

import com.unev.sige.model.Estudiante;
import com.unev.sige.model.LineaBoleta;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorDocumentosTest {

    @Test
    void generarBoletaHtmlContieneDatosDelEstudianteYCalificaciones() {
        Estudiante est = new Estudiante(10, "Carlos", "Santana", "carlos@sige.edu.do", "hash", "EST-2026-001", "3ro Secundaria", 88.5);
        List<LineaBoleta> lineas = List.of(
                new LineaBoleta("2026-1", "MAT101", "Matemáticas", 85.0, 90.0, 88.0, 87.7, "B", "APROBADA"),
                new LineaBoleta("2026-1", "LIT102", "Lengua Española", 92.0, 95.0, 94.0, 93.7, "A", "APROBADA")
        );

        String html = GeneradorDocumentos.generarBoletaHtml(est, lineas, "2026-1");

        assertNotNull(html);
        assertTrue(html.contains("Carlos Santana"));
        assertTrue(html.contains("EST-2026-001"));
        assertTrue(html.contains("3ro Secundaria"));
        assertTrue(html.contains("Matemáticas"));
        assertTrue(html.contains("Lengua Española"));
        assertTrue(html.contains("87.70"));
        assertTrue(html.contains("93.70"));
        assertTrue(html.contains("Boleta Oficial de Calificaciones"));
        assertTrue(html.contains("window.print()"));
    }

    @Test
    void generarCarnetHtmlContieneQrYMatricula() {
        Estudiante est = new Estudiante(10, "Ana", "Morales", "ana@sige.edu.do", "hash", "EST-2026-999", "4to Primaria", 95.0);

        String html = GeneradorDocumentos.generarCarnetHtml(est);

        assertNotNull(html);
        assertTrue(html.contains("Ana Morales"));
        assertTrue(html.contains("EST-2026-999"));
        assertTrue(html.contains("4to Primaria"));
        assertTrue(html.contains("data:image/svg+xml;base64,"));
        assertFalse(html.contains("<script>alert"));
    }
}
