package com.unev.sige.controller;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Window;
import com.unev.sige.app.Navegacion;

import java.math.BigDecimal;

/**
 * Pequenas utilidades de interfaz compartidas por las pantallas del panel.
 */
final class Dialogos {

    private Dialogos() {
    }

    static Label titulo(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("section-title");
        etiqueta.setPadding(new Insets(0, 0, 8, 0));
        return etiqueta;
    }

    /**
     * Formatea una nota sin ceros sobrantes: 85.0 se muestra "85" y 78.5 se muestra "78.5".
     */
    static String nota(double valor) {
        return new BigDecimal(Double.toString(valor)).stripTrailingZeros().toPlainString();
    }

    static void aplicarEstilos(javafx.scene.control.Dialog<?> dialogo) {
        String hoja = Navegacion.class.getResource("/com/unev/sige/view/styles.css").toExternalForm();
        if (!dialogo.getDialogPane().getStylesheets().contains(hoja)) {
            dialogo.getDialogPane().getStylesheets().add(hoja);
        }
    }

    static void prepararModal(javafx.scene.control.Dialog<?> dialogo, Window propietario) {
        dialogo.initOwner(propietario);
        dialogo.initModality(Modality.APPLICATION_MODAL);
        aplicarEstilos(dialogo);
    }

    static void error(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensaje, ButtonType.OK);
        alerta.setTitle("SIGE");
        alerta.setHeaderText(null);
        aplicarEstilos(alerta);
        alerta.showAndWait();
    }

    static void informacion(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, mensaje, ButtonType.OK);
        alerta.setTitle("SIGE");
        alerta.setHeaderText(null);
        aplicarEstilos(alerta);
        alerta.showAndWait();
    }

    /**
     * Pregunta Si/No y devuelve true solo si el usuario elige Si.
     */
    static boolean confirmar(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.YES, ButtonType.NO);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        aplicarEstilos(alerta);
        return alerta.showAndWait().filter(boton -> boton == ButtonType.YES).isPresent();
    }

    /**
     * Muestra un texto de estado (normal o en rojo si es un error) en la etiqueta indicada.
     */
    static void estado(Label etiqueta, String texto, boolean esError) {
        etiqueta.getStyleClass().removeAll("error-message", "success-message");
        etiqueta.getStyleClass().add(esError ? "error-message" : "success-message");
        etiqueta.setText(texto);
    }
}
