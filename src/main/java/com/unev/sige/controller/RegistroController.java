package com.unev.sige.controller;

import com.unev.sige.app.Navegacion;
import com.unev.sige.data.UsuarioDAO;
import com.unev.sige.model.Estudiante;
import com.unev.sige.model.Padre;
import com.unev.sige.model.Profesor;
import com.unev.sige.model.Rol;
import com.unev.sige.model.Usuario;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Window;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * Controlador del formulario de Registro (registro.fxml).
 * Valida los datos, y guarda el usuario en MySQL con su contrasena hasheada.
 * Segun el rol elegido pide los datos propios de esa tabla:
 * ESTUDIANTE (matricula y grado), PROFESOR (codigo y especialidad), PADRE (telefono y ocupacion).
 */
@SuppressWarnings("unused")
public class RegistroController {

    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtApellido;

    @FXML
    private TextField txtCorreo;

    @FXML
    private PasswordField txtContrasena;

    @FXML
    private PasswordField txtConfirmarContrasena;

    @FXML
    private ComboBox<Rol> cmbRol;

    @FXML
    private Label lblDato1;

    @FXML
    private TextField txtDato1;

    @FXML
    private Label lblDato2;

    @FXML
    private TextField txtDato2;

    @FXML
    private Label lblMensaje;

    @FXML
    private Button btnGuardar;

    @FXML
    private Button btnLimpiar;

    @FXML
    private Button btnCancelar;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    private void initialize() {
        // El rol ADMIN no se ofrece en el registro publico.
        for (Rol r : Rol.values()) {
            if (r != Rol.ADMIN) {
                cmbRol.getItems().add(r);
            }
        }
        mostrarCamposExtra(false);
    }

    /**
     * Cambia las etiquetas de los dos campos extra segun el rol elegido.
     */
    @FXML
    private void onRolSeleccionado() {
        Rol rol = cmbRol.getValue();
        if (rol == null || rol == Rol.ADMIN) {
            mostrarCamposExtra(false);
            return;
        }

        switch (rol) {
            case ESTUDIANTE -> configurarCamposExtra("Matrícula:", "Ej. M-2026-001", "Grado:", "Ej. 8vo");
            case PROFESOR -> configurarCamposExtra("Código de profesor:", "Ej. P-001",
                    "Especialidad:", "Ej. Matemática");
            case PADRE -> configurarCamposExtra("Teléfono (opcional):", "809-000-0000",
                    "Ocupación (opcional):", "Ocupación");
            default -> {
            }
        }
        mostrarCamposExtra(true);
    }

    @FXML
    private void onGuardar() {
        String nombre = limpiar(txtNombre.getText());
        String apellido = limpiar(txtApellido.getText());
        String correo = limpiar(txtCorreo.getText()).toLowerCase();
        String contrasena = txtContrasena.getText();
        String confirmar = txtConfirmarContrasena.getText();
        Rol rol = cmbRol.getValue();
        String dato1 = limpiar(txtDato1.getText());
        String dato2 = limpiar(txtDato2.getText());

        if (rol == Rol.ADMIN) {
            mostrarError("El rol de Administrador no está permitido en el registro público.");
            return;
        }

        boolean pideDatosExtra = rol == Rol.ESTUDIANTE || rol == Rol.PROFESOR;
        if (nombre.isEmpty() || apellido.isEmpty() || correo.isEmpty()
                || esVacio(contrasena) || esVacio(confirmar) || rol == null
                || (pideDatosExtra && (dato1.isEmpty() || dato2.isEmpty()))) {
            mostrarError("Todos los campos son obligatorios.");
            return;
        }

        if (!correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
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

        try {
            if (usuarioDAO.existeCorreo(correo)) {
                mostrarError("Ese correo ya está registrado. Usa otro.");
                return;
            }
            if (rol == Rol.ESTUDIANTE && usuarioDAO.existeMatricula(dato1)) {
                mostrarError("Esa matrícula ya existe.");
                return;
            }
            if (rol == Rol.PROFESOR && usuarioDAO.existeCodigoProfesor(dato1)) {
                mostrarError("Ese código de profesor ya existe.");
                return;
            }

            Usuario nuevoUsuario = switch (rol) {
                case ESTUDIANTE -> new Estudiante(0, nombre, apellido, correo, null, dato1, dato2, 0.0);
                case PROFESOR -> new Profesor(0, nombre, apellido, correo, null, dato1, dato2);
                case PADRE -> new Padre(0, nombre, apellido, correo, null, 0,
                        dato1.isEmpty() ? null : dato1, dato2.isEmpty() ? null : dato2);
                case ADMIN -> throw new IllegalStateException("ADMIN no se registra desde el formulario.");
            };

            usuarioDAO.registrar(nuevoUsuario, contrasena);

            mostrarExito("Usuario registrado correctamente: " + nuevoUsuario.getNombreCompleto() + " (" + rol + ")");
            onLimpiar();
        } catch (SQLIntegrityConstraintViolationException e) {
            mostrarError("Ya existe un registro con esos datos (correo, matrícula o código).");
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarError("No se pudo guardar en la base de datos. Revisa db.properties y que MySQL esté activo.");
        }
    }

    @FXML
    private void onLimpiar() {
        txtNombre.clear();
        txtApellido.clear();
        txtCorreo.clear();
        txtContrasena.clear();
        txtConfirmarContrasena.clear();
        txtDato1.clear();
        txtDato2.clear();
        cmbRol.getSelectionModel().clearSelection();
        mostrarCamposExtra(false);
    }

    /**
     * Cancelar vuelve a la pantalla de login (no cierra la aplicacion).
     */
    @FXML
    private void onCancelar() {
        try {
            Navegacion.irA(btnCancelar, "login.fxml", "SIGE - Inicio de sesion");
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("No se pudo volver a la pantalla de inicio de sesión.");
        }
    }

    // ----- Auxiliares -----

    private void configurarCamposExtra(String etiqueta1, String ayuda1, String etiqueta2, String ayuda2) {
        lblDato1.setText(etiqueta1);
        txtDato1.setPromptText(ayuda1);
        txtDato1.clear();
        lblDato2.setText(etiqueta2);
        txtDato2.setPromptText(ayuda2);
        txtDato2.clear();
    }

    private void mostrarCamposExtra(boolean visible) {
        for (javafx.scene.Node nodo : new javafx.scene.Node[]{lblDato1, txtDato1, lblDato2, txtDato2}) {
            nodo.setVisible(visible);
            nodo.setManaged(visible);
        }
        if (btnGuardar != null && btnGuardar.getScene() != null) {
            Window ventana = btnGuardar.getScene().getWindow();
            if (ventana != null) {
                ventana.sizeToScene();
            }
        }
    }

    private String limpiar(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setTextFill(Color.RED);
        lblMensaje.setText(mensaje);
    }

    private void mostrarExito(String mensaje) {
        lblMensaje.setTextFill(Color.GREEN);
        lblMensaje.setText(mensaje);
    }
}
