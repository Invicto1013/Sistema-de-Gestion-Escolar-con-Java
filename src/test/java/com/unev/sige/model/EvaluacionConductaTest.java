package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EvaluacionConductaTest {

    private static ObservacionConducta obs(String tipo) {
        ObservacionConducta o = new ObservacionConducta();
        o.setTipo(tipo);
        return o;
    }

    @Test
    @DisplayName("Sin observaciones la conducta es perfecta (100 puntos y Calificación A)")
    void evaluar_sinObservaciones_calificacionA() {
        EvaluacionConducta.ResumenConducta r = EvaluacionConducta.evaluar(null);
        assertEquals(100, r.puntajeNeto());
        assertEquals(EvaluacionConducta.CalificacionConducta.A, r.calificacion());
        assertFalse(r.requiereOrientacion());
    }

    @Test
    @DisplayName("Méritos y tardanzas moderadas mantienen buena calificación")
    void evaluar_meritosYTardanzas_calificacionB() {
        List<ObservacionConducta> lista = List.of(
                obs("CONDUCTA"),  // -10
                obs("TARDANZA"),  // -2
                obs("MERITO")     // +5 -> Total = 100 - 10 - 2 + 5 = 93 -> pero faltas = 1
        );

        EvaluacionConducta.ResumenConducta r = EvaluacionConducta.evaluar(lista);
        assertEquals(93, r.puntajeNeto());
        assertEquals(EvaluacionConducta.CalificacionConducta.A, r.calificacion());
        assertFalse(r.requiereOrientacion());
    }

    @Test
    @DisplayName("Faltas graves y reiteradas activan intervención de Orientación")
    void evaluar_multiplesFaltas_activaOrientacion() {
        List<ObservacionConducta> lista = List.of(
                obs("CONDUCTA"),
                obs("CONDUCTA"),
                obs("CONDUCTA"), // 3 faltas
                obs("TAREA")     // -3
        );

        // 100 - 30 - 3 = 67 puntos -> Calif D
        EvaluacionConducta.ResumenConducta r = EvaluacionConducta.evaluar(lista);
        assertEquals(67, r.puntajeNeto());
        assertEquals(EvaluacionConducta.CalificacionConducta.D, r.calificacion());
        assertTrue(r.requiereOrientacion());
    }
}
