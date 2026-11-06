package com.unev.sige.app;

import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneOffset;

/**
 * Estación del año del escritorio: cambia solo el color de acento (encabezado y menú) al acercarse cada estación,
 * 7 días antes del inicio astronómico (hemisferio norte). Se puede fijar con la variable SIGE_ESTACION
 * (primavera, verano, otono, invierno) o desactivar con SIGE_ESTACION=ninguna.
 */
public final class EstacionApp {
    private static final String[] TODAS = {"primavera", "verano", "otono", "invierno"};

    private EstacionApp() {
    }

    public static String deFecha(LocalDate fecha) {
        MonthDay d = MonthDay.from(fecha);
        if (!d.isBefore(MonthDay.of(12, 14)) || d.isBefore(MonthDay.of(3, 13))) return "invierno";
        if (d.isBefore(MonthDay.of(6, 14))) return "primavera";
        if (d.isBefore(MonthDay.of(9, 15))) return "verano";
        return "otono";
    }

    /** Estación a mostrar ("" si está desactivada). */
    public static String actual() {
        String forzada = System.getenv("SIGE_ESTACION");
        if (forzada != null) {
            forzada = forzada.trim().toLowerCase();
            if (forzada.equals("ninguna")) return "";
            for (String e : TODAS) if (e.equals(forzada)) return e;
        }
        return deFecha(LocalDate.now(ZoneOffset.of(com.unev.sige.model.ZonaVisual.desplazamiento())));
    }

    /** Quita las hojas de estación y añade la actual. */
    public static void aplicar(ObservableList<String> hojas) {
        for (String e : TODAS) {
            var recurso = EstacionApp.class.getResource("/com/unev/sige/view/estacion-" + e + ".css");
            if (recurso != null) hojas.remove(recurso.toExternalForm());
        }
        String actual = actual();
        if (actual.isEmpty()) return;
        var recurso = EstacionApp.class.getResource("/com/unev/sige/view/estacion-" + actual + ".css");
        if (recurso != null) hojas.add(recurso.toExternalForm());
    }
}
