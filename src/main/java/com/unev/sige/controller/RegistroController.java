package com.unev.sige.controller;

import com.unev.sige.app.Navegacion;
import com.unev.sige.data.UsuarioDAO;
import com.unev.sige.data.CorreoNotificaciones;
import com.unev.sige.model.Estudiante;
import com.unev.sige.model.Padre;
import com.unev.sige.model.Rol;
import com.unev.sige.model.Usuario;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Window;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.concurrent.CompletableFuture;

/**
 * Controlador del formulario de Registro (registro.fxml).
 * Valida los datos, y guarda el usuario en MySQL con su contrasena hasheada.
 * El registro público admite ESTUDIANTE y PADRE; las cuentas PROFESOR se crean desde el panel admin.
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
        // ADMIN y PROFESOR se reservan al flujo del administrador.
        for (Rol r : Rol.values()) {
            if (r == Rol.ESTUDIANTE || r == Rol.PADRE) {
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

        if (rol == Rol.ADMIN || rol == Rol.PROFESOR) {
            mostrarError("Ese rol solo puede ser creado por un administrador.");
            return;
        }

        boolean pideDatosExtra = rol == Rol.ESTUDIANTE;
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

        String incumplimiento = com.unev.sige.model.PoliticaContrasena.validar(contrasena);
        if (incumplimiento != null) {
            mostrarError(incumplimiento);
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
            Usuario nuevoUsuario = switch (rol) {
                case ESTUDIANTE -> new Estudiante(0, nombre, apellido, correo, null, dato1, dato2, 0.0);
                case PADRE -> new Padre(0, nombre, apellido, correo, null, 0,
                        dato1.isEmpty() ? null : dato1, dato2.isEmpty() ? null : dato2);
                case PROFESOR, ADMIN -> throw new IllegalStateException(
                        "Ese rol se crea desde el panel del administrador.");
            };

            if (UsuarioDAO.registroRequiereAprobacion()) {
                usuarioDAO.registrarSolicitud(nuevoUsuario, contrasena);
                onLimpiar();
                mostrarExito("Solicitud enviada. Podrás iniciar sesión cuando la administración la apruebe.");
                return;
            }
            usuarioDAO.registrar(nuevoUsuario, contrasena);

            mostrarExito("Perfil creado. Intentando enviar la notificación por correo...");
            onLimpiar();
            CompletableFuture.runAsync(() -> {
                try {
                    if (!CorreoNotificaciones.aceptaDestinatario(nuevoUsuario.getCorreo())) {
                        Platform.runLater(() -> mostrarExito("Perfil creado. Se omitió el correo: solo se notifican cuentas personales Gmail, Outlook, Hotmail, Live o MSN."));
                        return;
                    }
                    if (nuevoUsuario instanceof Estudiante estudiante) CorreoNotificaciones.enviarPerfilEstudiante(estudiante);
                    else if (nuevoUsuario instanceof Padre padre) CorreoNotificaciones.enviarPerfilPadre(padre);
                    Platform.runLater(() -> mostrarExito("Perfil creado y notificación enviada al correo personal."));
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarError("El perfil se creó, pero no se envió el correo. La administración debe configurar SMTP."));
                }
            });
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
        lblMensaje.getStyleClass().add("error-message");
        lblMensaje.setText(mensaje);
    }

    private void mostrarExito(String mensaje) {
        lblMensaje.getStyleClass().remove("error-message");
        lblMensaje.getStyleClass().add("success-message");
        lblMensaje.setText(mensaje);
    }
}
