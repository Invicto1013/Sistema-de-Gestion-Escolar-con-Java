package com.unev.sige.web;

import static org.junit.jupiter.api.Assertions.*;

import com.unev.sige.model.Rol;
import com.unev.sige.model.SuscripcionCentro;
import org.junit.jupiter.api.Test;

class PlanesWebTest {

    private static final SuscripcionCentro ACTUAL = new SuscripcionCentro("ESTANDAR", "24/11/2026 09:00", "Elian Castro");

    @Test
    void adminVeBotonesDeCambioPeroNoEnElPlanActual() {
        String h = PlanesWeb.pagina(Rol.ADMIN, ACTUAL, 120, "tok", null);
        assertTrue(h.contains("name='plan' value='ESENCIAL'"));
        assertTrue(h.contains("name='plan' value='INSTITUCIONAL'"));
        assertFalse(h.contains("name='plan' value='ESTANDAR'"));
        assertTrue(h.contains("value='tok'"));
        assertTrue(h.contains("Plan actual"));
        assertTrue(h.contains("120</strong> usuarios activos de 600"));
        assertTrue(h.contains("role='progressbar'"));
    }

    @Test
    void direccionSoloConsulta() {
        String h = PlanesWeb.pagina(Rol.DIRECTOR, ACTUAL, 10, "tok", null);
        assertFalse(h.contains("<form"));
        assertTrue(h.contains("Solo la administración puede cambiar el plan."));
    }

    @Test
    void sinTablasAvisaYNoFalla() {
        String h = PlanesWeb.pagina(Rol.ADMIN, null, -1, "tok", null);
        assertTrue(h.contains("sql/fase_planes_servicio.sql"));
        assertTrue(h.contains("Cambiar a Estándar"));
    }

    @Test
    void escapaElAvisoYMarcaExceso() {
        String h = PlanesWeb.pagina(Rol.ADMIN, new SuscripcionCentro("ESENCIAL", "x", "y"), 400, "t", "<script>alert(1)</script>");
        assertFalse(h.contains("<script>"));
        assertTrue(h.contains("plan-exceso"));
        assertTrue(h.contains("Excede el límite del plan"));
    }
}
