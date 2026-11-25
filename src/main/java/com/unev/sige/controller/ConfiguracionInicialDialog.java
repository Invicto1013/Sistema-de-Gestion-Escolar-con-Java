package com.unev.sige.controller;

import com.unev.sige.data.ConexionBD;
import com.unev.sige.data.ConfiguracionUsuario;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Asistente de primer arranque: pide los datos de conexión de este equipo y los guarda fuera del ejecutable
 * (ConfiguracionUsuario). Las claves se escriben aquí, nunca se muestran ni se registran.
 */
public final class ConfiguracionInicialDialog {

    private ConfiguracionInicialDialog() {
    }

    /** Muestra el asistente. Devuelve true si quedó una conexión que funciona; false si el usuario canceló. */
    public static boolean mostrar() {
        Dialog<Boolean> dialogo = new Dialog<>();
        dialogo.setTitle("SIGE - Configuración inicial");
        dialogo.setHeaderText("Primera vez en este equipo: conecta SIGE a la base de datos");

        TextField url = new TextField();
        url.setPromptText("jdbc:mysql://servidor:puerto/sige_db?sslMode=REQUIRED&serverTimezone=America/Santo_Domingo");
        url.setPrefWidth(460);
        TextField usuario = new TextField();
        PasswordField clave = new PasswordField();
        PasswordField clave2fa = new PasswordField();
        clave2fa.setPromptText("Opcional: solo si usas verificación en dos pasos");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("Dirección (db.url)"), url);
        form.addRow(1, new Label("Usuario"), usuario);
        form.addRow(2, new Label("Contraseña"), clave);
        form.addRow(3, new Label("Clave 2FA"), clave2fa);

        Label estado = new Label();
        estado.setWrapText(true);
        estado.setMaxWidth(560);
        Button importar = new Button("Importar config.properties…");
        Label nota = new Label("Los datos se guardan solo en este equipo (" + ConfiguracionUsuario.carpeta()
                + "). El programa no los incluye ni los sube a ningún sitio.");
        nota.setWrapText(true);
        nota.setMaxWidth(560);

        VBox contenido = new VBox(10, nota, form, new HBox(8, importar), estado);
        contenido.setPadding(new Insets(10));
        dialogo.getDialogPane().setContent(contenido);

        ButtonType guardar = new ButtonType("Probar y guardar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);
        Button botonGuardar = (Button) dialogo.getDialogPane().lookupButton(guardar);

        // Evita que el diálogo se cierre antes de comprobar la conexión.
        botonGuardar.addEventFilter(javafx.event.ActionEvent.ACTION, evento -> {
            evento.consume();
            List<String> errores = ConfiguracionUsuario.validar(url.getText(), usuario.getText(), clave2fa.getText());
            if (!errores.isEmpty()) {
                estado.setText(String.join("\n", errores));
                return;
            }
            Map<String, String> valores = new LinkedHashMap<>();
            valores.put(ConfiguracionUsuario.CLAVE_URL, url.getText());
            valores.put(ConfiguracionUsuario.CLAVE_USUARIO, usuario.getText());
            valores.put(ConfiguracionUsuario.CLAVE_PASSWORD, clave.getText());
            valores.put(ConfiguracionUsuario.CLAVE_2FA, clave2fa.getText());
            probarYGuardar(valores, estado, botonGuardar, dialogo);
        });

        importar.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Elige un config.properties");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Propiedades", "*.properties"));
            File f = fc.showOpenDialog(dialogo.getDialogPane().getScene().getWindow());
            if (f == null) {
                return;
            }
            try {
                ConfiguracionUsuario.importar(f.toPath());
                ConexionBD.recargar();
                estado.setText("Archivo importado. Probando la conexión…");
                comprobar(estado, botonGuardar, dialogo);
            } catch (IllegalArgumentException ex) {
                estado.setText("No se importó: " + ex.getMessage());
            } catch (IOException ex) {
                estado.setText("No se pudo leer o guardar el archivo.");
            }
        });

        dialogo.setResultConverter(b -> b == guardar);
        dialogo.showAndWait();
        return ConexionBD.estaConfigurada() && probarSilencioso();
    }

    private static void probarYGuardar(Map<String, String> valores, Label estado, Button boton, Dialog<Boolean> dialogo) {
        try {
            ConfiguracionUsuario.guardar(valores);
            ConexionBD.recargar();
        } catch (IOException ex) {
            estado.setText("No se pudo guardar la configuración en este equipo.");
            return;
        }
        comprobar(estado, boton, dialogo);
    }

    /** Intenta conectar en segundo plano; cierra el diálogo si funciona, o explica el fallo sin mostrar claves. */
    private static void comprobar(Label estado, Button boton, Dialog<Boolean> dialogo) {
        boton.setDisable(true);
        estado.setText("Probando la conexión…");
        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (Connection c = ConexionBD.obtener()) {
                    c.isValid(5);
                }
                return null;
            }
        };
        tarea.setOnSucceeded(e -> {
            dialogo.setResult(true);
            dialogo.close();
        });
        tarea.setOnFailed(e -> {
            boton.setDisable(false);
            estado.setText("No se pudo conectar. Revisa la dirección, el usuario y la contraseña, y que la base esté encendida. "
                    + "Puedes corregir los datos y volver a intentarlo.");
        });
        Thread hilo = new Thread(tarea, "SIGE-Prueba-Conexion");
        hilo.setDaemon(true);
        hilo.start();
    }

    private static boolean probarSilencioso() {
        try (Connection c = ConexionBD.obtener()) {
            return c.isValid(5);
        } catch (Exception e) {
            return false;
        }
    }
}
