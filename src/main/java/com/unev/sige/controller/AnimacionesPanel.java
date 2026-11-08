package com.unev.sige.controller;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Transition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.util.List;

/** Animaciones suaves y reutilizables del panel principal (entrada escalonada, contadores y realce al pasar el cursor). */
final class AnimacionesPanel {
    private AnimacionesPanel() {
    }

    /** Cada nodo aparece desvaneciéndose y subiendo un poco, uno tras otro. */
    static void entrada(List<? extends Node> nodos, int pasoMs) {
        int i = 0;
        for (Node nodo : nodos) {
            nodo.setOpacity(0);
            nodo.setTranslateY(16);
            FadeTransition fundido = new FadeTransition(Duration.millis(450), nodo);
            fundido.setToValue(1);
            TranslateTransition subida = new TranslateTransition(Duration.millis(450), nodo);
            subida.setToY(0);
            subida.setInterpolator(Interpolator.EASE_OUT);
            ParallelTransition conjunto = new ParallelTransition(fundido, subida);
            conjunto.setDelay(Duration.millis((long) i * pasoMs));
            conjunto.play();
            i++;
        }
    }

    /** Cada nodo entra desde la izquierda desvaneciéndose, uno tras otro (menú lateral). */
    static void entradaLateral(List<? extends Node> nodos, int pasoMs) {
        int i = 0;
        for (Node nodo : nodos) {
            nodo.setOpacity(0);
            nodo.setTranslateX(-18);
            FadeTransition fundido = new FadeTransition(Duration.millis(380), nodo);
            fundido.setToValue(1);
            TranslateTransition deslizar = new TranslateTransition(Duration.millis(380), nodo);
            deslizar.setToX(0);
            deslizar.setInterpolator(Interpolator.EASE_OUT);
            ParallelTransition conjunto = new ParallelTransition(fundido, deslizar);
            conjunto.setDelay(Duration.millis((long) i * pasoMs));
            conjunto.setOnFinished(e -> {
                nodo.setOpacity(1);
                nodo.setTranslateX(0);
            });
            conjunto.play();
            i++;
        }
    }

    /** El número sube desde cero hasta su valor final. */
    static void contar(Label etiqueta, long hasta) {
        if (hasta <= 0) return;
        Transition conteo = new Transition() {
            {
                setCycleDuration(Duration.millis(900));
                setInterpolator(Interpolator.EASE_OUT);
            }

            @Override
            protected void interpolate(double fraccion) {
                etiqueta.setText(Long.toString(Math.round(hasta * fraccion)));
            }
        };
        conteo.setOnFinished(e -> etiqueta.setText(Long.toString(hasta)));
        conteo.play();
    }

    /** La tarjeta crece levemente al pasar el cursor. */
    static void realzarAlPasar(Node nodo) {
        nodo.setOnMouseEntered(e -> escalar(nodo, 1.03));
        nodo.setOnMouseExited(e -> escalar(nodo, 1.0));
    }

    private static void escalar(Node nodo, double valor) {
        ScaleTransition t = new ScaleTransition(Duration.millis(150), nodo);
        t.setToX(valor);
        t.setToY(valor);
        t.play();
    }
}
