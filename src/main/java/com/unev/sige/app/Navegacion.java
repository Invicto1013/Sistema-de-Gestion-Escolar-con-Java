package com.unev.sige.app;

import javafx.application.Platform;
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
    public static void aplicarEstilos(Scene scene) {
        String hoja = Navegacion.class.getResource(RUTA_VISTAS + "styles.css").toExternalForm();
        if (!scene.getStylesheets().contains(hoja)) scene.getStylesheets().add(hoja);
        aplicarModo(scene, scene.getWidth());
        scene.widthProperty().addListener((obs, anterior, ancho) ->
                Platform.runLater(() -> aplicarModo(scene, scene.getWidth())));
    }

    private static void aplicarModo(Scene scene, double ancho) {
        String modo = ancho < 600 ? "modo-telefono" : ancho < 1024 ? "modo-tableta" : "modo-escritorio";
        if (modo.equals(scene.getProperties().get("sige.modo.responsive"))) return;
        Parent root = scene.getRoot();
        root.getStyleClass().removeAll("modo-escritorio", "modo-tableta", "modo-telefono");
        root.getStyleClass().add(modo);
        scene.getProperties().put("sige.modo.responsive", modo);
    }

    public static void irA(Node origen, String fxml, String titulo) throws IOException {
        FXMLLoader loader = new FXMLLoader(Navegacion.class.getResource(RUTA_VISTAS + fxml));
        Parent root = loader.load();

        Stage stage = (Stage) origen.getScene().getWindow();
        Scene scene = new Scene(root);
        aplicarEstilos(scene);
        stage.setScene(scene);
        stage.setTitle(titulo);
        stage.sizeToScene();
        stage.centerOnScreen();
    }
}
