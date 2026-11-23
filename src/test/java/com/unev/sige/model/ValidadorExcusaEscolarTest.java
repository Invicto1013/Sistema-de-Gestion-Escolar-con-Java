package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorExcusaEscolarTest {

    @Test
    @DisplayName("Excusa médica con comprobante a tiempo es válida y habilita reprogramación")
    void evaluar_medicaValida() {
        LocalDate falta = LocalDate.of(2026, 11, 20);
        LocalDate entrega = LocalDate.of(2026, 11, 22); // 2 días después (plazo <= 5)

        ValidadorExcusaEscolar.EvaluacionExcusa ev = ValidadorExcusaEscolar.evaluar(
                ValidadorExcusaEscolar.TipoExcusa.MEDICA, falta, entrega, true
        );

        assertTrue(ev.esValida());
        assertTrue(ev.habilitaReprogramacionExamen());
        assertEquals("Excusa aprobada institucionalmente.", ev.dictamen());
    }

    @Test
    @DisplayName("Excusa médica sin comprobante adjunto es rechazada")
    void evaluar_medicaSinComprobante() {
        LocalDate falta = LocalDate.of(2026, 11, 20);
        LocalDate entrega = LocalDate.of(2026, 11, 22);

        ValidadorExcusaEscolar.EvaluacionExcusa ev = ValidadorExcusaEscolar.evaluar(
                ValidadorExcusaEscolar.TipoExcusa.MEDICA, falta, entrega, false
        );

        assertFalse(ev.esValida());
        assertFalse(ev.habilitaReprogramacionExamen());
        assertTrue(ev.dictamen().contains("Falta adjuntar"));
    }

    @Test
    @DisplayName("Rechaza excusas extemporáneas que superan el plazo de 5 días")
    void evaluar_extemporanea() {
        LocalDate falta = LocalDate.of(2026, 11, 10);
        LocalDate entrega = LocalDate.of(2026, 11, 20); // 10 días después

        ValidadorExcusaEscolar.EvaluacionExcusa ev = ValidadorExcusaEscolar.evaluar(
                ValidadorExcusaEscolar.TipoExcusa.CALAMIDAD_FAMILIAR, falta, entrega, true
        );

        assertFalse(ev.esValida());
        assertTrue(ev.dictamen().contains("extemporánea"));
    }
}
