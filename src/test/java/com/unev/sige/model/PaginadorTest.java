package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PaginadorTest {

    @Test
    void cuentaPaginas() {
        assertEquals(1, Paginador.paginas(0, 10));
        assertEquals(1, Paginador.paginas(10, 10));
        assertEquals(2, Paginador.paginas(11, 10));
        assertEquals(3, Paginador.paginas(25, 10));
        assertEquals(5, Paginador.paginas(5, 0)); // tamano invalido = 1 fila por pagina
    }

    @Test
    void limitaLaPagina() {
        assertEquals(0, Paginador.limitar(-3, 25, 10));
        assertEquals(2, Paginador.limitar(9, 25, 10));
        assertEquals(0, Paginador.limitar(4, 0, 10));
    }

    @Test
    void calculaElRangoDeCadaPagina() {
        assertEquals(0, Paginador.desde(0, 25, 10));
        assertEquals(10, Paginador.hasta(0, 25, 10));
        assertEquals(20, Paginador.desde(2, 25, 10));
        assertEquals(25, Paginador.hasta(2, 25, 10));
        assertEquals(20, Paginador.desde(7, 25, 10));
        assertEquals(0, Paginador.desde(0, 0, 10));
        assertEquals(0, Paginador.hasta(0, 0, 10));
    }

    @Test
    void buscaSinAcentosNiMayusculas() {
        assertTrue(Paginador.coincide("Matemáticas · 6.º de Secundaria", "matematicas"));
        assertTrue(Paginador.coincide("Juan Vladimir Manzanillo", "MANZANILLO juan"));
        assertTrue(Paginador.coincide("cualquier cosa", "   "));
        assertTrue(Paginador.coincide("cualquier cosa", null));
        assertFalse(Paginador.coincide("Carlos Peguero", "carlos lopez"));
    }

    @Test
    void normalizaEspaciosYNulos() {
        assertEquals("a b", Paginador.normalizar("  Á   B "));
        assertEquals("", Paginador.normalizar(null));
    }
}
