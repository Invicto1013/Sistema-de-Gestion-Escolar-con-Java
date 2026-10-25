package com.unev.sige.controller;

import com.unev.sige.data.PermisoDenegadoException;
import com.unev.sige.data.CorreoNotificaciones;
import com.unev.sige.data.UsuarioDAO;
import com.unev.sige.model.Profesor;
import javafx.geometry.Insets;
import javafx.application.Platform;
import javafx.scene.control.Button;
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

/** Formulario administrativo para crear cuentas de profesor. */
public class CrearProfesorPanel extends VBox {
    private final int idAdmin;
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final TextField nombre = new TextField();
    private final TextField apellido = new TextField();
    private final TextField correo = new TextField();
    private final TextField codigo = new TextField();
    private final TextField especialidad = new TextField();
    private final PasswordField contrasena = new PasswordField();
    private final PasswordField confirmar = new PasswordField();
    private final Label estado = new Label();

    public CrearProfesorPanel(int idAdmin) {
        this.idAdmin = idAdmin;
        setSpacing(12);

        GridPane formulario = new GridPane();
        formulario.setHgap(12);
        formulario.setVgap(10);
        formulario.setPadding(new Insets(12, 0, 12, 0));
        nombre.setPromptText("Nombre");
        apellido.setPromptText("Apellido");
        correo.setPromptText("docente@institucion.edu.do");
        codigo.setPromptText("Ej. P-001");
        especialidad.setPromptText("Ej. Matemática");
        contrasena.setPromptText("Mínimo 4 caracteres");
        confirmar.setPromptText("Repite la contraseña");
        agregarFila(formulario, 0, "Nombre:", nombre);
        agregarFila(formulario, 1, "Apellido:", apellido);
        agregarFila(formulario, 2, "Correo:", correo);
        agregarFila(formulario, 3, "Código de profesor:", codigo);
        agregarFila(formulario, 4, "Especialidad:", especialidad);
        agregarFila(formulario, 5, "Contraseña inicial:", contrasena);
        agregarFila(formulario, 6, "Confirmar contraseña:", confirmar);

        Button guardar = new Button("Crear profesor");
        guardar.setOnAction(e -> guardar());
        VBox.setVgrow(formulario, Priority.NEVER);
        getChildren().addAll(Dialogos.titulo("Crear cuenta de profesor"),
                new Label("Esta cuenta solo se puede crear desde una sesión de administrador."),
                formulario, new HBox(guardar), estado);
    }

    private void agregarFila(GridPane formulario, int fila, String etiqueta, TextField campo) {
        formulario.add(new Label(etiqueta), 0, fila);
        formulario.add(campo, 1, fila);
        GridPane.setHgrow(campo, Priority.ALWAYS);
        campo.setPrefWidth(360);
    }

    private void guardar() {
        String n = limpiar(nombre.getText());
        String a = limpiar(apellido.getText());
        String c = limpiar(correo.getText()).toLowerCase();
        String cod = limpiar(codigo.getText());
        String esp = limpiar(especialidad.getText());
        String pass = contrasena.getText();
        String pass2 = confirmar.getText();
        if (n.isEmpty() || a.isEmpty() || c.isEmpty() || cod.isEmpty() || esp.isEmpty()
                || pass == null || pass.isBlank() || pass2 == null || pass2.isBlank()) {
            Dialogos.estado(estado, "Completa todos los campos.", true);
            return;
        }
        if (n.length() > 50 || a.length() > 50 || c.length() > 100 || cod.length() > 20 || esp.length() > 100) {
            Dialogos.estado(estado, "Revisa la longitud: nombre/apellido 50, correo 100, "
                    + "código 20 y especialidad 100 caracteres máximo.", true);
            return;
        }
        if (!c.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            Dialogos.estado(estado, "Ingresa un correo electrónico válido.", true);
            return;
        }
        if (pass.length() < 4) {
            Dialogos.estado(estado, "La contraseña inicial debe tener al menos 4 caracteres.", true);
            return;
        }
        if (!pass.equals(pass2)) {
            Dialogos.estado(estado, "Las contraseñas no coinciden.", true);
            return;
        }
        try {
            if (usuarioDAO.existeCorreo(c)) {
                Dialogos.estado(estado, "Ese correo ya está registrado.", true);
                return;
            }
            if (usuarioDAO.existeCodigoProfesor(cod)) {
                Dialogos.estado(estado, "Ese código de profesor ya está registrado.", true);
                return;
            }
            Profesor profesor = new Profesor(0, n, a, c, null, cod, esp);
            usuarioDAO.registrarProfesorComoAdmin(profesor, pass, idAdmin);
            Dialogos.estado(estado, "Perfil creado. Enviando notificación a " + profesor.getCorreo() + "…", false);
            limpiarFormulario();
            CompletableFuture.runAsync(() -> {
                try {
                    if (!CorreoNotificaciones.aceptaDestinatario(profesor.getCorreo())) {
                        Platform.runLater(() -> Dialogos.estado(estado, "Perfil creado. Se omitió el correo: solo se notifican cuentas personales Gmail, Outlook, Hotmail, Live o MSN.", false));
                        return;
                    }
                    CorreoNotificaciones.enviarPerfilProfesor(profesor);
                    Platform.runLater(() -> Dialogos.estado(estado,
                            "Perfil creado y notificación enviada a " + profesor.getCorreo() + ".", false));
                } catch (Exception envio) {
                    System.err.println("No se pudo enviar la notificación de creación del perfil docente.");
                    Platform.runLater(() -> Dialogos.estado(estado,
                            "Perfil creado, pero no se pudo enviar el correo. Revisa la configuración SMTP.", true));
                }
            });
        } catch (PermisoDenegadoException e) {
            Dialogos.estado(estado, e.getMessage(), true);
        } catch (SQLIntegrityConstraintViolationException e) {
            Dialogos.estado(estado, "El correo o código ya está en uso.", true);
        } catch (SQLException e) {
            Dialogos.estado(estado, "No se pudo crear la cuenta. Revisa la conexión con MySQL.", true);
        }
    }

    private String limpiar(String texto) { return texto == null ? "" : texto.trim(); }

    private void limpiarFormulario() {
        nombre.clear(); apellido.clear(); correo.clear(); codigo.clear(); especialidad.clear();
        contrasena.clear(); confirmar.clear();
    }
}
