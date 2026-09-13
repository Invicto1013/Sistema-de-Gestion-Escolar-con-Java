package com.unev.sige.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Clase principal de la aplicacion JavaFX.
 * Por ahora muestra una pantalla simple; en el siguiente paso se reemplazara
 * por la carga del Login (login.fxml).
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        Label label = new Label("SIGE - Sistema Integral de Gestion Escolar");
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 500, 300);

        stage.setTitle("SIGE");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
