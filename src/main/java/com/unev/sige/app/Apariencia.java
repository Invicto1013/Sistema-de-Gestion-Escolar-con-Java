package com.unev.sige.app;

import javafx.collections.ObservableList;

/** Aplica tema y modo de visión en el orden correcto: el modo de visión (daltonismo, contraste) siempre prevalece. */
public final class Apariencia {
    private static final String DALTONISMO_OSCURO = "/com/unev/sige/view/modo-daltonismo-oscuro.css";

    private Apariencia() {
    }

    public static void aplicar(ObservableList<String> hojas) {
        TemaApp.aplicar(hojas);
        EstacionApp.aplicar(hojas);
        ModoVision.aplicar(hojas);
        String extra = Apariencia.class.getResource(DALTONISMO_OSCURO).toExternalForm();
        hojas.remove(extra);
        if (TemaApp.actual() == TemaApp.OSCURO && ModoVision.actual() == ModoVision.DALTONISMO) hojas.add(extra);
    }
}
