package com.unev.sige.controller;

import com.unev.sige.data.UsuarioRepositorio;
import com.unev.sige.model.Rol;
import com.unev.sige.model.Usuario;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controlador del formulario de Registro (registro.fxml).
 * Valida los datos ingresados y crea un nuevo Usuario en UsuarioRepositorio.
 */
public class RegistroController {

    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtApellido;

    @FXML
    private TextField txtUsuario;

    @FXML
    private TextField txtCorreo;

    @FXML
    private PasswordField txtContrasena;

    @FXML
    private PasswordField txtConfirmarContrasena;

    @FXML
    private ComboBox<Rol> cmbRol;

    @FXML
    private Label lblMensaje;

    @FXML
    private Button btnGuardar;

    @FXML
    private Button btnLimpiar;

    @FXML
    private Button btnCancelar;

    private int siguienteId = 100;

    @FXML
private void initialize() {
    // Filtrar la lista de roles para excluir ADMINISTRADOR
    for (Rol r : Rol.values()) {
        if (r != Rol.ADMIN) { // Cambia Rol.ADMINISTRADOR según como esté nombrado en tu Enum (ej. Rol.ADMIN)
            cmbRol.getItems().add(r);
        }
    }
}


    /**
     * 
     */
    @FXML
    private void onGuardar() {
        String nombre = txtNombre.getText();
        String apellido = txtApellido.getText();
        String usuario = txtUsuario.getText();
        String correo = txtCorreo.getText();
        String contrasena = txtContrasena.getText();
        String confirmar = txtConfirmarContrasena.getText();
        Rol rol = cmbRol.getValue();

        if (rol == Rol.ADMIN) {
    mostrarError("El rol de Administrador no está permitido en el registro público.");
    return;
}
        if (esVacio(nombre) || esVacio(apellido) || esVacio(usuario) || esVacio(correo)
                || esVacio(contrasena) || esVacio(confirmar) || rol == null) {
            mostrarError("Todos los campos son obligatorios.");
            return;
        }

        if (!correo.contains("@") || !correo.contains(".")) {
            mostrarError("Ingresa un correo electrónico válido.");
            return;
        }

        if (!contrasena.equals(confirmar)) {
            mostrarError("Las contraseñas no coinciden.");
            return;
        }

        if (contrasena.length() < 4) {
            mostrarError("La contraseña debe tener al menos 4 caracteres.");
            return;
        }

        if (UsuarioRepositorio.existeNombreUsuario(usuario)) {
            mostrarError("Ese nombre de usuario ya existe. Elige otro.");
            return;
        }

        String nombreCompleto = nombre + " " + apellido;
        Usuario nuevoUsuario = new Usuario(siguienteId++, usuario, contrasena, nombreCompleto, correo, rol);
        UsuarioRepositorio.agregar(nuevoUsuario);

        mostrarExito("Usuario registrado correctamente: " + nombreCompleto + " (" + rol + ")");
        onLimpiar();
    }

    @FXML
    private void onLimpiar() {
        txtNombre.clear();
        txtApellido.clear();
        txtUsuario.clear();
        txtCorreo.clear();
        txtContrasena.clear();
        txtConfirmarContrasena.clear();
        cmbRol.getSelectionModel().clearSelection();
    }

    @FXML
    private void onCancelar(ActionEvent event) {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    private boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
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
