package com.unev.sige.controller;

import com.unev.sige.model.AcercaDe;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/** Ventana «Acerca de SIGE» que se abre con el icono de interrogación de la barra superior. */
final class AcercaDeDialogo {
    private AcercaDeDialogo() { }

    static void mostrar() {
        VBox contenido = new VBox(10);
        contenido.setPadding(new Insets(4, 8, 4, 4));
        Label resumen = new Label(AcercaDe.resumen());
        resumen.setWrapText(true);
        Label version = new Label("Versión " + AcercaDe.VERSION);
        version.setStyle("-fx-font-weight:bold;");
        contenido.getChildren().addAll(resumen, version);
        for (AcercaDe.Seccion s : AcercaDe.secciones()) {
            Label titulo = new Label(s.titulo());
            titulo.setStyle("-fx-font-weight:bold;-fx-font-size:14px;");
            contenido.getChildren().add(titulo);
            for (String linea : s.lineas()) {
                Label l = new Label("• " + linea);
                l.setWrapText(true);
                contenido.getChildren().add(l);
            }
        }
        Hyperlink repo = new Hyperlink(AcercaDe.REPOSITORIO);
        repo.setOnAction(e -> {
            try { java.awt.Desktop.getDesktop().browse(java.net.URI.create(AcercaDe.REPOSITORIO)); }
            catch (Throwable ex) { repo.setText(AcercaDe.REPOSITORIO + " (cópialo en tu navegador)"); }
        });
        contenido.getChildren().addAll(new Label("Código fuente"), repo);

        ScrollPane desplazable = new ScrollPane(contenido);
        desplazable.setFitToWidth(true);
        desplazable.setPrefSize(560, 420);
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.CLOSE);
        alerta.setTitle("Acerca de " + AcercaDe.NOMBRE);
        alerta.setHeaderText(AcercaDe.NOMBRE + " · " + AcercaDe.NOMBRE_COMPLETO);
        alerta.getDialogPane().setContent(desplazable);
        Dialogos.aplicarEstilos(alerta);
        alerta.showAndWait();
    }
}
