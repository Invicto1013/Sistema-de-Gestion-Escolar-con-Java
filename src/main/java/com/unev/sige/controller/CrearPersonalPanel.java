package com.unev.sige.controller;

import com.unev.sige.data.CorreoNotificaciones;
import com.unev.sige.data.PermisoDenegadoException;
import com.unev.sige.data.UsuarioDAO;
import com.unev.sige.model.Permisos;
import com.unev.sige.model.PoliticaContrasena;
import com.unev.sige.model.Rol;
import com.unev.sige.model.Usuario;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.concurrent.CompletableFuture;

/** Formulario administrativo para crear cuentas de director/a y secretario/a. */
public class CrearPersonalPanel extends VBox {
    private final int idAdmin;
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final TextField nombre = new TextField();
    private final TextField apellido = new TextField();
    private final TextField correo = new TextField();
    private final ComboBox<Rol> cargo = new ComboBox<>();
    private final PasswordField contrasena = new PasswordField();
    private final PasswordField confirmar = new PasswordField();
    private final Label estado = new Label();
    private final HBox accionManual = new HBox();

    public CrearPersonalPanel(int idAdmin) {
        this.idAdmin = idAdmin;
        setSpacing(12);
        cargo.getItems().setAll(Rol.DIRECTOR, Rol.SECRETARIA);
        cargo.setValue(Rol.DIRECTOR);
        cargo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Rol r) { return r == null ? "" : r.etiqueta(); }
            @Override public Rol fromString(String s) { return null; }
        });
        GridPane formulario = new GridPane();
        formulario.setHgap(12);
        formulario.setVgap(10);
        formulario.setPadding(new Insets(12, 0, 12, 0));
        nombre.setPromptText("Nombre");
        apellido.setPromptText("Apellido");
        correo.setPromptText("correo personal (Gmail, Outlook, Hotmail…)");
        contrasena.setPromptText(PoliticaContrasena.REQUISITOS);
        confirmar.setPromptText("Repite la contraseña");
        agregarFila(formulario, 0, "Cargo:", cargo);
        agregarFila(formulario, 1, "Nombre:", nombre);
        agregarFila(formulario, 2, "Apellido:", apellido);
        agregarFila(formulario, 3, "Correo:", correo);
        agregarFila(formulario, 4, "Contraseña inicial:", contrasena);
        agregarFila(formulario, 5, "Confirmar contraseña:", confirmar);
        Button guardar = new Button("Crear cuenta");
        guardar.setOnAction(e -> guardar());
        estado.setWrapText(true);
        getChildren().addAll(Dialogos.titulo("Crear director/a o secretario/a"),
                new Label("Solo un administrador puede crear estas cuentas. La contraseña no se envía por correo: entrégala por otro medio."),
                formulario, new HBox(guardar), estado, accionManual);
    }

    private void agregarFila(GridPane formulario, int fila, String etiqueta, javafx.scene.Node campo) {
        formulario.add(new Label(etiqueta), 0, fila);
        formulario.add(campo, 1, fila);
        GridPane.setHgrow(campo, Priority.ALWAYS);
        if (campo instanceof javafx.scene.layout.Region r) r.setPrefWidth(360);
    }

    private void guardar() {
        accionManual.getChildren().clear();
        Rol rol = cargo.getValue();
        String n = limpiar(nombre.getText());
        String a = limpiar(apellido.getText());
        String c = limpiar(correo.getText()).toLowerCase();
        String pass = contrasena.getText();
        String pass2 = confirmar.getText();
        if (rol == null || !Permisos.esPersonalCreable(rol)) { Dialogos.estado(estado, "Selecciona un cargo válido.", true); return; }
        if (n.isEmpty() || a.isEmpty() || c.isEmpty() || pass == null || pass.isBlank() || pass2 == null || pass2.isBlank()) {
            Dialogos.estado(estado, "Completa todos los campos.", true);
            return;
        }
        if (n.length() > 50 || a.length() > 50 || c.length() > 100) {
            Dialogos.estado(estado, "Revisa la longitud: nombre y apellido 50, correo 100 caracteres máximo.", true);
            return;
        }
        if (!c.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            Dialogos.estado(estado, "Ingresa un correo electrónico válido.", true);
            return;
        }
        String incumplimiento = PoliticaContrasena.validar(pass);
        if (incumplimiento != null) { Dialogos.estado(estado, incumplimiento, true); return; }
        if (!pass.equals(pass2)) { Dialogos.estado(estado, "Las contraseñas no coinciden.", true); return; }
        try {
            if (usuarioDAO.existeCorreo(c)) { Dialogos.estado(estado, "Ese correo ya está registrado.", true); return; }
            Usuario personal = new Usuario(0, n, a, c, null, rol);
            usuarioDAO.registrarPersonalComoAdmin(personal, pass, idAdmin);
            limpiarFormulario();
            avisar(personal);
        } catch (PermisoDenegadoException e) {
            Dialogos.estado(estado, e.getMessage(), true);
        } catch (SQLIntegrityConstraintViolationException e) {
            Dialogos.estado(estado, "Ese correo ya está en uso.", true);
        } catch (SQLException e) {
            Dialogos.estado(estado, "No se pudo crear la cuenta. Revisa la conexión con MySQL.", true);
        }
    }

    private void avisar(Usuario personal) {
        String correoPersonal = personal.getCorreo();
        if (!CorreoNotificaciones.aceptaDestinatario(correoPersonal)) {
            Dialogos.estado(estado, "Perfil creado. Se omitió el correo: solo se notifican cuentas personales Gmail, Outlook, Hotmail, Live o MSN.", false);
            return;
        }
        if (!CorreoNotificaciones.envioAutomaticoConfigurado()) {
            mostrarManual(personal, "Perfil creado. No hay un servicio de correo automático configurado: envía el aviso desde tu propio correo.");
            return;
        }
        Dialogos.estado(estado, "Perfil creado. Enviando aviso a " + correoPersonal + "…", false);
        CompletableFuture.runAsync(() -> {
            try {
                CorreoNotificaciones.enviarPerfilPersonal(personal);
                Platform.runLater(() -> Dialogos.estado(estado, "Perfil creado y aviso enviado a " + correoPersonal + ".", false));
            } catch (Exception envio) {
                System.err.println("No se pudo enviar el aviso de creación del perfil.");
                Platform.runLater(() -> mostrarManual(personal, "Perfil creado, pero el correo automático falló. Puedes enviar el aviso desde tu propio correo."));
            }
        });
    }

    private void mostrarManual(Usuario personal, String texto) {
        Dialogos.estado(estado, texto, false);
        accionManual.getChildren().setAll(AvisoCorreo.botonManual(personal.getCorreo(), CorreoNotificaciones.mensajePerfilPersonal(personal), estado));
    }

    private String limpiar(String texto) { return texto == null ? "" : texto.trim(); }

    private void limpiarFormulario() {
        nombre.clear(); apellido.clear(); correo.clear(); contrasena.clear(); confirmar.clear();
    }
}
