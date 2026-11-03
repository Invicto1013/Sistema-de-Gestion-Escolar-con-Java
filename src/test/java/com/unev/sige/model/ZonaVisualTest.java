package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ZonaVisualTest {

    @Test
    void aceptaDesplazamientosValidos() {
        assertEquals("-05:00", ZonaVisual.normalizar(" -05:00 "));
        assertEquals("+01:30", ZonaVisual.normalizar("+01:30"));
    }

    @Test
    void usaLaPredeterminadaSiElValorEsInvalido() {
        for (String malo : new String[] {null, "", "UTC", "-4", "+15:00", "-04:60", "'; DROP TABLE x;--"}) {
            assertEquals(ZonaVisual.PREDETERMINADA, ZonaVisual.normalizar(malo));
        }
    }

    @Test
    void laExpresionSqlConvierteLaColumna() {
        String sql = ZonaVisual.sql("a.fecha");
        assertTrue(sql.startsWith("CONVERT_TZ(a.fecha, @@session.time_zone, '"));
    }
}
