package com.unev.sige.controller;

import com.unev.sige.data.UsuarioRepositorio;
import com.unev.sige.model.Usuario;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

/**
 * Controlador de la pantalla de Login (login.fxml).
 * Valida campos vacios y autentica al usuario contra UsuarioRepositorio,
 * usando el metodo autenticar() de la clase Usuario.
 */
@SuppressWarnings("unused")
public class LoginController {

    @FXML
    private TextField txtUsuario;

    @FXML
    private PasswordField txtContrasena;

    @FXML
    private Label lblMensaje;

    @FXML
    private Button btnIniciarSesion;

    @FXML
    private Button btnSalir;

    @FXML
    private void onIniciarSesion() {
        String usuarioIngresado = txtUsuario.getText();
        String contrasenaIngresada = txtContrasena.getText();

        if (usuarioIngresado == null || usuarioIngresado.isBlank()
                || contrasenaIngresada == null || contrasenaIngresada.isBlank()) {
            mostrarError("Debes completar usuario y contraseña.");
            return;
        }

        Optional<Usuario> usuarioEncontrado = UsuarioRepositorio.buscarPorNombreUsuario(usuarioIngresado);

        if (usuarioEncontrado.isEmpty()) {
            mostrarError("Usuario o contraseña incorrectos.");
            return;
        }

        Usuario usuario = usuarioEncontrado.get();
        boolean autenticado = usuario.autenticar(usuarioIngresado, contrasenaIngresada);

        if (autenticado) {
            mostrarExito("Acceso correcto. Bienvenido, " + usuario.getNombreCompleto() + " (" + usuario.getRol() + ")");
        } else {
            mostrarError("Usuario o contraseña incorrectos.");
        }
    }

    @FXML
    private void onSalir() {
        Platform.exit();
    }

    @FXML
private void onIrARegistro(javafx.event.ActionEvent event) {
    try {
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
            getClass().getResource("/com/unev/sige/view/registro.fxml")
        );
        javafx.scene.Parent root = loader.load();
        javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setScene(new javafx.scene.Scene(root));
        stage.setTitle("SIGE - Registro de Usuario");
        stage.show();
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    private void mostrarError(String mensaje) {
        lblMensaje.setTextFill(javafx.scene.paint.Color.RED);
        lblMensaje.setText(mensaje);
    }

    private void mostrarExito(String mensaje) {
        lblMensaje.setTextFill(javafx.scene.paint.Color.GREEN);
        lblMensaje.setText(mensaje);
    }
}
