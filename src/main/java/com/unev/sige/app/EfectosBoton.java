package com.unev.sige.app;

import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 * Animación fluida de botones sin bibliotecas externas: crecen un poco al pasar el cursor
 * y se hunden al pulsar. Se instala una sola vez por escena.
 */
public final class EfectosBoton {

    private static final String MARCA = "sige.efectos.boton";
    private static final String ANIMACION = "sige.efectos.animacion";

    private EfectosBoton() {
    }

    public static void instalar(Scene scene) {
        if (scene.getProperties().containsKey(MARCA)) return;
        scene.getProperties().put(MARCA, Boolean.TRUE);
        scene.addEventFilter(MouseEvent.MOUSE_ENTERED_TARGET, e -> actualizar(e.getTarget()));
        scene.addEventFilter(MouseEvent.MOUSE_EXITED_TARGET, e -> actualizar(e.getTarget()));
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> actualizar(e.getTarget()));
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> actualizar(e.getTarget()));
    }

    private static void actualizar(EventTarget objetivo) {
        if (!(objetivo instanceof Node)) return;
        Node nodo = (Node) objetivo;
        while (nodo != null && !(nodo instanceof ButtonBase)) nodo = nodo.getParent();
        if (nodo == null) return;
        final ButtonBase boton = (ButtonBase) nodo;
        // El estado hover/pressed se lee un instante después para que ya esté actualizado.
        Platform.runLater(() -> animar(boton));
    }

    private static void animar(ButtonBase boton) {
        double escala = boton.isDisabled() ? 1.0 : boton.isPressed() ? 0.96 : boton.isHover() ? 1.04 : 1.0;
        Object previa = boton.getProperties().get(ANIMACION);
        if (previa instanceof ScaleTransition) ((ScaleTransition) previa).stop();
        ScaleTransition t = new ScaleTransition(Duration.millis(120), boton);
        t.setToX(escala);
        t.setToY(escala);
        boton.getProperties().put(ANIMACION, t);
        t.play();
    }
}
