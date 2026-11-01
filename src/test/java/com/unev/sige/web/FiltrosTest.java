package com.unev.sige.web;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FiltrosTest {
    @Test void normalizaAcentosYMayusculas() {
        assertEquals("matematica", Filtros.normalizar("  Matemática "));
        assertEquals("", Filtros.normalizar(null));
    }

    @Test void busquedaVaciaCoincideConTodo() {
        assertTrue(Filtros.coincide("", "cualquier cosa"));
        assertTrue(Filtros.coincide(null, (String) null));
        assertTrue(Filtros.coincide("   ", "x"));
    }

    @Test void ignoraAcentosYCaso() {
        assertTrue(Filtros.coincide("matematica", "MAT101", "Matemática básica"));
        assertTrue(Filtros.coincide("JOSÉ", "jose", "perez"));
    }

    @Test void todasLasPalabrasDebenAparecerEnAlgunCampo() {
        assertTrue(Filtros.coincide("ana perez", "Ana", "Pérez", "ana@correo.com"));
        assertFalse(Filtros.coincide("ana gomez", "Ana", "Pérez"));
    }

    @Test void camposNulosNoRompen() {
        assertTrue(Filtros.coincide("ana", null, "Ana"));
        assertFalse(Filtros.coincide("ana", (String) null));
    }

    @Test void textoRecortaYNuncaEsNulo() {
        assertEquals("", Filtros.texto(null, 10));
        assertEquals("abc", Filtros.texto("  abc ", 10));
        assertEquals("abcd", Filtros.texto("abcdefgh", 4));
    }

    @Test void opcionSoloAceptaValoresPermitidos() {
        List<String> ok = List.of("ESTUDIANTE", "PADRE");
        assertEquals("PADRE", Filtros.opcion("PADRE", ok));
        assertEquals("", Filtros.opcion("ADMIN", ok));
        assertEquals("", Filtros.opcion(null, ok));
    }

    @Test void enteroInvalidoONegativoEsSinFiltro() {
        assertEquals(7, Filtros.entero("7"));
        assertEquals(0, Filtros.entero("abc"));
        assertEquals(0, Filtros.entero("-3"));
        assertEquals(0, Filtros.entero(null));
    }

    @Test void barraEscapaValoresYMuestraLimpiar() {
        String campos = Filtros.campoTexto("q", "Buscar", "<script>", "Nombre");
        String html = Filtros.barra("/secciones", Map.of(), campos, true, 2, 9);
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("Limpiar filtros"));
        assertTrue(html.contains("Mostrando 2 de 9 registros."));
        assertTrue(html.contains("role='search'"));
    }

    @Test void barraSinFiltrosNoOfreceLimpiar() {
        String html = Filtros.barra("/secciones", Map.of(), "", false, 9, 9);
        assertFalse(html.contains("Limpiar filtros"));
        assertTrue(html.contains("Mostrando 9 registros."));
    }

    @Test void limpiarConservaCamposOcultos() {
        String html = Filtros.barra("/calificaciones", Map.of("seccion", "5"), "", true, 1, 3);
        assertTrue(html.contains("href='/calificaciones?seccion=5'"));
        assertTrue(html.contains("name='seccion' value='5'"));
    }

    @Test void listaMarcaLaOpcionSeleccionadaYEscapa() {
        Map<String, String> op = new LinkedHashMap<>();
        op.put("1", "Primero <A>");
        op.put("2", "Segundo");
        String html = Filtros.campoLista("grado", "Grado", "2", op);
        assertTrue(html.contains("<option value='2' selected>"));
        assertTrue(html.contains("Primero &lt;A&gt;"));
        assertTrue(html.contains("<option value=''>Todos</option>"));
    }
}
