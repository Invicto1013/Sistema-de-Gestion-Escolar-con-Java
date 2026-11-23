package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IndiceAcademicoTest {

    @Test
    @DisplayName("Lista vacía o nula calcula promedio 0.0")
    void calcularPromedio_vacioONulo_retornaCero() {
        assertEquals(0.0, IndiceAcademico.calcularPromedio(null), 0.001);
        assertEquals(0.0, IndiceAcademico.calcularPromedio(List.of()), 0.001);
    }

    @Test
    @DisplayName("Calcula promedio exacto redondeado a 2 decimales")
    void calcularPromedio_valoresValidos_calculaCorrectamente() {
        List<Double> notas = List.of(90.0, 85.5, 78.25);
        assertEquals(84.58, IndiceAcademico.calcularPromedio(notas), 0.001);
    }

    @Test
    @DisplayName("Evalúa boleta con excelencia académica (Summa Cum Laude)")
    void evaluarBoleta_excelenciaSummaCumLaude() {
        List<LineaBoleta> lineas = List.of(
                new LineaBoleta("2026-1", "MAT101", "Matemáticas", 98, 96, 95, 96.0, "A", "APROBADA"),
                new LineaBoleta("2026-1", "ESP101", "Español", 95, 95, 96, 95.4, "A", "APROBADA")
        );

        IndiceAcademico.Resumen resumen = IndiceAcademico.evaluarBoleta(lineas);

        assertEquals(2, resumen.totalMaterias());
        assertEquals(2, resumen.materiasAprobadas());
        assertEquals(0, resumen.materiasReprobadas());
        assertEquals(95.7, resumen.indice(), 0.01);
        assertEquals("A", resumen.literalEquivalente());
        assertEquals(IndiceAcademico.CondicionAcademica.EXCELENCIA, resumen.condicion());
        assertEquals(IndiceAcademico.HonorAcademico.SUMMA_CUM_LAUDE, resumen.honor());
        assertTrue(resumen.esPromovible());
    }

    @Test
    @DisplayName("Evalúa boleta regular aprobatoria")
    void evaluarBoleta_condicionRegular() {
        List<LineaBoleta> lineas = List.of(
                new LineaBoleta("2026-1", "MAT101", "Matemáticas", 75, 75, 75, 75.0, "C", "APROBADA"),
                new LineaBoleta("2026-1", "SOC101", "Sociales", 80, 80, 80, 80.0, "B", "APROBADA")
        );

        IndiceAcademico.Resumen resumen = IndiceAcademico.evaluarBoleta(lineas);

        assertEquals(2, resumen.totalMaterias());
        assertEquals(2, resumen.materiasAprobadas());
        assertEquals(0, resumen.materiasReprobadas());
        assertEquals(77.5, resumen.indice(), 0.01);
        assertEquals("C", resumen.literalEquivalente());
        assertEquals(IndiceAcademico.CondicionAcademica.REGULAR, resumen.condicion());
        assertEquals(IndiceAcademico.HonorAcademico.NINGUNO, resumen.honor());
        assertTrue(resumen.esPromovible());
    }

    @Test
    @DisplayName("Evalúa boleta en alerta o a prueba cuando reprueba materias")
    void evaluarBoleta_conMateriasReprobadas() {
        List<LineaBoleta> lineas = List.of(
                new LineaBoleta("2026-1", "MAT101", "Matemáticas", 50, 50, 50, 50.0, "F", "REPROBADA"),
                new LineaBoleta("2026-1", "FIS101", "Física", 60, 60, 60, 60.0, "D", "REPROBADA")
        );

        IndiceAcademico.Resumen resumen = IndiceAcademico.evaluarBoleta(lineas);

        assertEquals(2, resumen.totalMaterias());
        assertEquals(0, resumen.materiasAprobadas());
        assertEquals(2, resumen.materiasReprobadas());
        assertEquals(55.0, resumen.indice(), 0.01);
        assertEquals("F", resumen.literalEquivalente());
        assertEquals(IndiceAcademico.CondicionAcademica.A_PRUEBA, resumen.condicion());
        assertEquals(IndiceAcademico.HonorAcademico.NINGUNO, resumen.honor());
        assertFalse(resumen.esPromovible());
    }
}
