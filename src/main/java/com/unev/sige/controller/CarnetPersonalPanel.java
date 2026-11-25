package com.unev.sige.controller;

import com.unev.sige.data.UsuarioDAO;
import com.unev.sige.model.Permisos;
import com.unev.sige.model.Usuario;
import com.unev.sige.service.GeneradorDocumentos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

/**
 * Carnet de identidad del personal. Profesores, dirección y secretaría generan el suyo; administración,
 * dirección y secretaría pueden además emitir el de cualquier miembro activo del personal.
 */
public final class CarnetPersonalPanel extends VBox {

    private final Usuario usuario;
    private final Label estado = new Label();

    public CarnetPersonalPanel(Usuario usuario) {
        this.usuario = usuario;
        setSpacing(12);
        getChildren().add(Dialogos.titulo("🆔 Carnet de identidad del personal"));
        getChildren().add(new Label("Se abre en el navegador listo para imprimir, con código QR."));

        if (Permisos.tieneCarnetPersonal(usuario.getRol())) {
            Button propio = new Button("Generar mi carnet");
            propio.getStyleClass().add("primary");
            propio.setOnAction(e -> abrir(usuario));
            getChildren().add(propio);
        }
        if (Permisos.emiteCarnetsDelPersonal(usuario.getRol())) {
            ComboBox<Usuario> personal = new ComboBox<>();
            personal.setPrefWidth(360);
            personal.setPromptText("Elige un miembro del personal");
            personal.setConverter(new StringConverter<>() {
                @Override
                public String toString(Usuario u) {
                    return u == null ? "" : u.getNombreCompleto() + " · " + u.getRol().etiqueta()
                            + " · " + GeneradorDocumentos.codigoPersonal(u);
                }

                @Override
                public Usuario fromString(String s) {
                    return null;
                }
            });
            try {
                for (Usuario u : new UsuarioDAO().listarTodos()) {
                    if (u.isActivo() && Permisos.tieneCarnetPersonal(u.getRol())) {
                        personal.getItems().add(u);
                    }
                }
            } catch (SQLException e) {
                Dialogos.estado(estado, "No se pudo cargar el personal. Verifica la conexión con la base de datos.", true);
            }
            Button emitir = new Button("Generar carnet seleccionado");
            emitir.setOnAction(e -> {
                Usuario elegido = personal.getValue();
                if (elegido == null) {
                    Dialogos.error("Elige un miembro del personal.");
                    return;
                }
                abrir(elegido);
            });
            getChildren().addAll(new Label("Emitir el carnet de otra persona del personal:"), new HBox(8, personal, emitir));
        }
        getChildren().add(estado);
    }

    private void abrir(Usuario persona) {
        try {
            String html = GeneradorDocumentos.generarCarnetPersonalHtml(persona);
            Path temp = Files.createTempFile("carnet-personal-", ".html");
            Files.writeString(temp, html, StandardCharsets.UTF_8);
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(temp.toUri());
            } else {
                Dialogos.estado(estado, "Carnet generado en: " + temp.toAbsolutePath(), false);
            }
        } catch (Exception e) {
            Dialogos.error("No se pudo generar el carnet: " + e.getMessage());
        }
    }
}
