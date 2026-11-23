package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ControlServicioSocialTest {

    @Test
    @DisplayName("Sin horas acumuladas el estado es INICIAL")
    void evaluar_ceroHoras() {
        ControlServicioSocial.ExpedienteServicio exp = ControlServicioSocial.evaluar(0);

        assertEquals(0, exp.horasAcumuladas());
        assertEquals(60, exp.horasMeta());
        assertEquals(60, exp.horasRestantes());
        assertEquals(0.0, exp.porcentajeCumplido(), 0.001);
        assertEquals(ControlServicioSocial.EstadoServicio.INICIAL, exp.estado());
        assertFalse(exp.acreditado());
    }

    @Test
    @DisplayName("Horas parciales sitúan al estudiante en estado EN_PROCESO")
    void evaluar_horasParciales() {
        ControlServicioSocial.ExpedienteServicio exp = ControlServicioSocial.evaluar(30);

        assertEquals(30, exp.horasAcumuladas());
        assertEquals(30, exp.horasRestantes());
        assertEquals(50.0, exp.porcentajeCumplido(), 0.001);
        assertEquals(ControlServicioSocial.EstadoServicio.EN_PROCESO, exp.estado());
        assertFalse(exp.acreditado());
    }

    @Test
    @DisplayName("Permite configurar metas horarias personalizadas por el centro")
    void evaluarConMeta_personalizada() {
        // Meta de 80 horas comunitarias
        ControlServicioSocial.ExpedienteServicio exp = ControlServicioSocial.evaluarConMeta(80, 80);

        assertEquals(80, exp.horasAcumuladas());
        assertEquals(80, exp.horasMeta());
        assertEquals(0, exp.horasRestantes());
        assertEquals(100.0, exp.porcentajeCumplido(), 0.001);
        assertEquals(ControlServicioSocial.EstadoServicio.COMPLETADO, exp.estado());
        assertTrue(exp.acreditado());
    }
}
