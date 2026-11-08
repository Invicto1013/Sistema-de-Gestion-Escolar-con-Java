package com.unev.sige.model;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtensionesEscolaresModelTest {

    @Test
    void horarioSeccionFormateaRangoCorrectamente() {
        HorarioSeccion h = new HorarioSeccion(1, 5, "LUNES", LocalTime.of(8, 0), LocalTime.of(9, 30), "Aula 204");
        h.setAsignaturaNombre("Física General");
        h.setProfesorNombre("Prof. Roberto Cruz");

        assertEquals("08:00 - 09:30", h.getRangoHora());
        assertEquals("Aula 204", h.getAula());
        assertEquals("LUNES", h.getDiaSemana());
    }

    @Test
    void anuncioConservaPrioridadYDestinatarios() {
        Anuncio a = new Anuncio(1, "Suspensión de docencia", "Por alerta meteorológica", 1, "Dirección", "TODOS", "URGENTE", null, true);

        assertEquals("URGENTE", a.getPrioridad());
        assertEquals("TODOS", a.getDestinatarios());
        assertTrue(a.isActivo());
        assertNotNull(a.getTitulo());
    }

    @Test
    void observacionConductaConservaAtributos() {
        ObservacionConducta o = new ObservacionConducta(1, 10, "Estudiante Juan", "EST-01", 5, "Prof. Gómez", 2, "Matemáticas", "MERITO", "Gran esfuerzo", "Destacó en el proyecto final", null);

        assertEquals("MERITO", o.getTipo());
        assertEquals("Gran esfuerzo", o.getTitulo());
        assertEquals("Estudiante Juan", o.getEstudianteNombre());
    }
}
