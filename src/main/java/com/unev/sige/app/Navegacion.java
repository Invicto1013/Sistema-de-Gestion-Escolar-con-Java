package com.unev.sige.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Utilidad para cambiar de pantalla dentro de la misma ventana.
 */
public final class Navegacion {

    private static final String RUTA_VISTAS = "/com/unev/sige/view/";

    private Navegacion() {
    }

    /**
     * Carga un FXML de la carpeta de vistas y lo muestra en la ventana del nodo de origen.
     *
     * @param origen cualquier nodo de la pantalla actual (un boton, por ejemplo)
     * @param fxml   nombre del archivo, p. ej. "login.fxml"
     * @param titulo titulo de la ventana
     */
    public static void irA(Node origen, String fxml, String titulo) throws IOException {
        FXMLLoader loader = new FXMLLoader(Navegacion.class.getResource(RUTA_VISTAS + fxml));
        Parent root = loader.load();

        Stage stage = (Stage) origen.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle(titulo);
        stage.sizeToScene();
        stage.centerOnScreen();
    }
}
