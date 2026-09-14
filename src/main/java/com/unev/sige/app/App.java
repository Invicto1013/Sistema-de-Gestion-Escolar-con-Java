package com.unev.sige.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Clase principal de la aplicacion JavaFX.
 * Carga la pantalla de Login (login.fxml) al iniciar.
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/unev/sige/view/login.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 480, 380);

        stage.setTitle("SIGE - Inicio de sesion");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
