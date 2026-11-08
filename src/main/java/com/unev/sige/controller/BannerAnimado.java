package com.unev.sige.controller;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Banner del panel principal: título y descripción sobre un fondo con círculos que flotan y laten.
 * Las animaciones se detienen solas cuando el banner sale de la pantalla.
 */
final class BannerAnimado extends StackPane {
    private final List<Animation> animaciones = new ArrayList<>();

    BannerAnimado(Label titulo, Label detalle) {
        getStyleClass().add("dashboard-banner");
        setAlignment(Pos.CENTER_LEFT);
        setMinHeight(104);
        titulo.getStyleClass().add("banner-title");
        detalle.getStyleClass().add("banner-sub");
        VBox texto = new VBox(4, titulo, detalle);
        texto.setAlignment(Pos.CENTER_LEFT);

        // Los círculos quedan detrás del texto y no reciben clics.
        Circle grande = circulo(62, Color.web("#FFFFFF", 0.07), Pos.CENTER_RIGHT, -46, 0);
        Circle teal = circulo(34, Color.web("#2AA89A", 0.28), Pos.TOP_RIGHT, -170, 14);
        Circle naranja = circulo(22, Color.web("#F26A2E", 0.38), Pos.BOTTOM_RIGHT, -110, -14);
        Circle punto = circulo(9, Color.web("#FFFFFF", 0.35), Pos.CENTER_RIGHT, -250, -10);
        getChildren().addAll(grande, teal, naranja, punto, texto);

        flotar(grande, 0, -12, 5.5);
        flotar(teal, 14, 10, 7.0);
        flotar(naranja, -12, -8, 4.5);
        flotar(punto, 10, 12, 6.0);
        latir(grande, 1.12, 4.0);
        latir(naranja, 1.25, 2.6);

        Rectangle recorte = new Rectangle();
        recorte.setArcWidth(24);
        recorte.setArcHeight(24);
        recorte.widthProperty().bind(widthProperty());
        recorte.heightProperty().bind(heightProperty());
        setClip(recorte);

        sceneProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null) animaciones.forEach(Animation::stop);
            else animaciones.forEach(Animation::play);
        });
    }

    private Circle circulo(double radio, Color color, Pos alineacion, double dx, double dy) {
        Circle c = new Circle(radio, color);
        c.setMouseTransparent(true);
        StackPane.setAlignment(c, alineacion);
        c.setTranslateX(dx);
        c.setTranslateY(dy);
        return c;
    }

    private void flotar(Node nodo, double dx, double dy, double segundos) {
        TranslateTransition t = new TranslateTransition(Duration.seconds(segundos), nodo);
        t.setByX(dx);
        t.setByY(dy);
        t.setAutoReverse(true);
        t.setCycleCount(Animation.INDEFINITE);
        t.setInterpolator(Interpolator.EASE_BOTH);
        animaciones.add(t);
        t.play();
    }

    private void latir(Node nodo, double escala, double segundos) {
        ScaleTransition s = new ScaleTransition(Duration.seconds(segundos), nodo);
        s.setToX(escala);
        s.setToY(escala);
        s.setAutoReverse(true);
        s.setCycleCount(Animation.INDEFINITE);
        s.setInterpolator(Interpolator.EASE_BOTH);
        animaciones.add(s);
        s.play();
    }
}
