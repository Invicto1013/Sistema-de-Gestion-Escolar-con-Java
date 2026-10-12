package com.unev.sige.controller;

import com.unev.sige.app.Navegacion;
import com.unev.sige.app.SesionActual;
import com.unev.sige.data.SesionDAO;
import com.unev.sige.data.Autenticacion2FADAO;
import com.unev.sige.data.PermisoDenegadoException;
import com.unev.sige.data.UsuarioDAO;
import com.unev.sige.model.ControladorSesion;
import com.unev.sige.model.Usuario;
import com.unev.sige.security.Configuracion2FAException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Controlador de la pantalla de Login (login.fxml).
 * Valida campos vacios y autentica al usuario por correo y contrasena contra MySQL
 * (UsuarioDAO), usando el metodo autenticar() de la clase Usuario.
 */
@SuppressWarnings("unused")
public class LoginController {

    @FXML
    private TextField txtCorreo;

    @FXML
    private PasswordField txtContrasena;

    @FXML
    private Label lblMensaje;

    @FXML
    private Button btnIniciarSesion;

    @FXML
    private Button btnSalir;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    private void onIniciarSesion() {
        String correoIngresado = txtCorreo.getText() == null ? "" : txtCorreo.getText().trim();
        String contrasenaIngresada = txtContrasena.getText();

        if (correoIngresado.isEmpty() || contrasenaIngresada == null || contrasenaIngresada.isBlank()) {
            mostrarError("Debes completar correo y contraseña.");
            return;
        }

        try {
            Optional<Usuario> usuarioEncontrado = usuarioDAO.buscarPorCorreo(correoIngresado);

            if (usuarioEncontrado.isEmpty()) {
                registrarAcceso(null, correoIngresado, "FALLIDO", "Credenciales incorrectas");
                mostrarError("Correo o contraseña incorrectos.");
                return;
            }

            Usuario usuario = usuarioEncontrado.get();
            if (usuario.autenticar(correoIngresado, contrasenaIngresada)) {
                if (!usuario.isActivo()) {
                    registrarAcceso(usuario.getId(), correoIngresado, "FALLIDO", "Cuenta desactivada");
                    mostrarError("La cuenta está desactivada. Contacta al administrador.");
                    return;
                }
                if (!Autenticacion2FA.exigir(btnIniciarSesion.getScene().getWindow(), usuario)) return;
                if (usuario.isDebeCambiarContrasena()) {
                    Optional<CambioContrasenaDialog.Resultado> cambio = CambioContrasenaDialog.mostrar(
                            btnIniciarSesion.getScene().getWindow(), false);
                    if (cambio.isEmpty()) {
                        registrarAcceso(usuario.getId(), correoIngresado, "FALLIDO", "Cambio de contraseña cancelado");
                        mostrarError("Debes cambiar la contraseña inicial para iniciar sesión.");
                        return;
                    }
                    usuarioDAO.cambiarContrasena(usuario.getId(), contrasenaIngresada, cambio.get().nueva());
                    usuario.setDebeCambiarContrasena(false);
                }
                registrarAcceso(usuario.getId(), correoIngresado, "EXITOSO", "Inicio de sesión correcto");
                abrirPanelPrincipal(usuario);
            } else {
                registrarAcceso(usuario.getId(), correoIngresado, "FALLIDO", "Credenciales incorrectas");
                mostrarError("Correo o contraseña incorrectos.");
            }
        } catch (Configuracion2FAException e) {
            mostrarError(e.getMessage());
        } catch (PermisoDenegadoException e) {
            mostrarError(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarError("No se pudo conectar con la base de datos. Revisa db.properties y que MySQL esté activo.");
        }
    }

    private void registrarAcceso(Integer idUsuario, String correo, String resultado, String detalle) {
        try {
            new Autenticacion2FADAO().registrarAcceso(idUsuario, correo, resultado, detalle);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onSalir() {
        Platform.exit();
    }

    @FXML
    private void onIrARegistro(ActionEvent event) {
        try {
            Navegacion.irA((Node) event.getSource(), "registro.fxml", "SIGE - Registro de Usuario");
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("No se pudo abrir la pantalla de registro.");
        }
    }

    private void mostrarError(String mensaje) {
        lblMensaje.getStyleClass().add("error-message");
        lblMensaje.setText(mensaje);
    }

    /**
     * Registra la sesion en la base de datos (si eso falla, no bloquea el acceso)
     * y abre el panel principal del usuario.
     */
    private void abrirPanelPrincipal(Usuario usuario) {
        ControladorSesion sesion = null;
        try {
            sesion = new SesionDAO().iniciar(usuario.getId(), "127.0.0.1");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        SesionActual.iniciar(usuario, sesion);

        try {
            Navegacion.irA(btnIniciarSesion, "dashboard.fxml", "SIGE - Panel principal");
        } catch (IOException e) {
            e.printStackTrace();
            if (sesion != null) {
                try {
                    new SesionDAO().cerrar(sesion.getIdSesion());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            SesionActual.cerrar();
            mostrarError("No se pudo abrir el panel principal.");
        }
    }
}
