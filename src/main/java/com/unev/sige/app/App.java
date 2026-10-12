package com.unev.sige.app;

import com.unev.sige.data.SesionDAO;
import com.unev.sige.model.ControladorSesion;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.SQLException;

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
        Navegacion.aplicarEstilos(scene);

        stage.setTitle("SIGE - Inicio de sesion");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Al cerrar la ventana con la sesion abierta, la marca como cerrada en la base de datos.
     */
    @Override
    public void stop() {
        ControladorSesion sesion = SesionActual.getSesion();
        if (sesion != null) {
            try {
                new SesionDAO().cerrar(sesion.getIdSesion());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
