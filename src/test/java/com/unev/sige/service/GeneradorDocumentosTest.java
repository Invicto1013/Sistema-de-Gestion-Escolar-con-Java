package com.unev.sige.service;

import com.unev.sige.model.Estudiante;
import com.unev.sige.model.LineaBoleta;
import com.unev.sige.model.Rol;
import com.unev.sige.model.Usuario;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private static Usuario persona(int id, String nombre, String apellido, Rol rol) {
        return new Usuario(id, nombre, apellido, nombre.toLowerCase() + "@sige.edu.do", "hash", rol);
    }

    @Test
    void carnetPersonalSeEmiteParaProfesorDirectorYSecretaria() {
        String prof = GeneradorDocumentos.generarCarnetPersonalHtml(persona(42, "Luis", "Pérez", Rol.PROFESOR));
        String dir = GeneradorDocumentos.generarCarnetPersonalHtml(persona(7, "María", "Gómez", Rol.DIRECTOR));
        String sec = GeneradorDocumentos.generarCarnetPersonalHtml(persona(8, "Rosa", "Díaz", Rol.SECRETARIA));

        assertTrue(prof.contains("Luis Pérez") && prof.contains("PROFESOR") && prof.contains("P-00042") && prof.contains("Personal docente"));
        assertTrue(dir.contains("María Gómez") && dir.contains("DIRECTOR/A") && dir.contains("P-00007") && dir.contains("Dirección del centro"));
        assertTrue(sec.contains("Rosa Díaz") && sec.contains("SECRETARIO/A") && sec.contains("P-00008") && sec.contains("Secretaría"));
        assertTrue(prof.contains("data:image/svg+xml;base64,") && prof.contains("window.print()"));
        assertTrue(dir.contains("#1e3a8a") && sec.contains("#6d28d9") && prof.contains("#0f766e"), "cada rol tiene su color");
        assertFalse(prof.contains("Matrícula"), "el carnet de personal no usa matrícula");
    }

    @Test
    void carnetPersonalNoSeEmiteParaOtrosRoles() {
        for (Rol r : new Rol[] {Rol.ESTUDIANTE, Rol.PADRE, Rol.ADMIN}) {
            assertThrows(IllegalArgumentException.class, () -> GeneradorDocumentos.generarCarnetPersonalHtml(persona(1, "A", "B", r)));
        }
    }

    @Test
    void carnetPersonalEscapaElNombre() {
        String html = GeneradorDocumentos.generarCarnetPersonalHtml(persona(5, "<script>alert(1)</script>", "X", Rol.PROFESOR));
        assertFalse(html.contains("<script>alert(1)"));
    }

    @Test
    void elCarnetEstudiantilSigueIgualConLaPlantillaCompartida() {
        Estudiante est = new Estudiante(10, "Ana", "Morales", "ana@sige.edu.do", "hash", "EST-2026-999", "4to Primaria", 95.0);
        String html = GeneradorDocumentos.generarCarnetHtml(est);
        assertTrue(html.contains("<span>ESTUDIANTE</span>") && html.contains("Matrícula: EST-2026-999") && html.contains("#0f766e") && html.contains("<title>Carnet Estudiantil - Ana Morales</title>"));
        assertEquals("P-00003", GeneradorDocumentos.codigoPersonal(persona(3, "A", "B", Rol.PROFESOR)));
    }
}
