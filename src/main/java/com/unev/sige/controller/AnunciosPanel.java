package com.unev.sige.controller;

import com.unev.sige.data.AnuncioDAO;
import com.unev.sige.model.Anuncio;
import com.unev.sige.model.Rol;
import com.unev.sige.model.Usuario;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Panel JavaFX para consultar y publicar anuncios y circulares escolares.
 */
public final class AnunciosPanel extends BorderPane {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Usuario usuarioActual;
    private final AnuncioDAO dao = new AnuncioDAO();
    private final TableView<Anuncio> tabla = new TableView<>();
    private final Label lblEstado = new Label();
    private final TablaPaginada<Anuncio> vista = new TablaPaginada<>(tabla, a ->
            a.getTitulo() + " " + a.getContenido() + " " + a.getAutorNombre() + " " + a.getPrioridad());

    public AnunciosPanel(Usuario usuario) {
        this.usuarioActual = usuario;
        setPadding(new Insets(10));

        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        columna("Prioridad", Anuncio::getPrioridad);
        columna("Fecha", a -> a.getFechaPublicacion() != null ? a.getFechaPublicacion().format(FORMATO_FECHA) : "—");
        columna("Título", Anuncio::getTitulo);
        columna("Destinatarios", Anuncio::getDestinatarios);
        columna("Autor", Anuncio::getAutorNombre);
        columna("Contenido", a -> a.getContenido() != null && a.getContenido().length() > 60
                ? a.getContenido().substring(0, 57) + "..." : a.getContenido());

        HBox barra = new HBox(10);
        barra.setPadding(new Insets(0, 0, 10, 0));

        Button btnVer = new Button("Leer anuncio");
        btnVer.setOnAction(e -> onLeerAnuncio());
        barra.getChildren().add(btnVer);

        if (usuario.getRol() == Rol.ADMIN) {
            Button btnNuevo = new Button("Publicar anuncio");
            btnNuevo.getStyleClass().add("primary");
            btnNuevo.setOnAction(e -> onNuevoAnuncio());

            Button btnEliminar = new Button("Eliminar");
            btnEliminar.setOnAction(e -> onEliminarAnuncio());

            barra.getChildren().addAll(btnNuevo, btnEliminar);
        }

        barra.getChildren().add(lblEstado);
        setTop(barra);

        vista.setTituloPdf("Anuncios y Circulares Escolares");
        setCenter(vista);

        recargar();
    }

    private void columna(String nombre, java.util.function.Function<Anuncio, String> valor) {
        TableColumn<Anuncio, String> c = new TableColumn<>(nombre);
        c.setCellValueFactory(f -> new SimpleStringProperty(valor.apply(f.getValue())));
        tabla.getColumns().add(c);
    }

    private void recargar() {
        try {
            List<Anuncio> lista = usuarioActual.getRol() == Rol.ADMIN
                    ? dao.listarTodos()
                    : dao.listarParaUsuario(usuarioActual.getRol());
            vista.setDatos(lista);
            lblEstado.setText(lista.size() + " anuncios disponibles");
        } catch (SQLException e) {
            lblEstado.setText("No se pudieron cargar los anuncios.");
        }
    }

    private void onLeerAnuncio() {
        Anuncio a = tabla.getSelectionModel().getSelectedItem();
        if (a == null) {
            mostrarAviso("Selecciona un anuncio para leer su contenido completo.");
            return;
        }
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Circular escolar");
        dialog.setHeaderText("[" + a.getPrioridad() + "] " + a.getTitulo());
        dialog.setContentText("Publicado por: " + a.getAutorNombre() + " ("
                + (a.getFechaPublicacion() != null ? a.getFechaPublicacion().format(FORMATO_FECHA) : "") + ")\n"
                + "Dirigido a: " + a.getDestinatarios() + "\n\n" + a.getContenido());
        dialog.showAndWait();
    }

    private void onNuevoAnuncio() {
        Dialog<Anuncio> dialog = new Dialog<>();
        dialog.setTitle("Publicar anuncio");
        dialog.setHeaderText("Nueva circular o comunicado institucional");

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(16));

        TextField txtTitulo = new TextField();
        txtTitulo.setPromptText("Ej. Reunión de padres y entrega de notas");
        ComboBox<String> cmbDest = new ComboBox<>();
        cmbDest.getItems().addAll("TODOS", "PROFESORES", "ESTUDIANTES", "PADRES");
        cmbDest.setValue("TODOS");

        ComboBox<String> cmbPrioridad = new ComboBox<>();
        cmbPrioridad.getItems().addAll("NORMAL", "IMPORTANTE", "URGENTE");
        cmbPrioridad.setValue("NORMAL");

        TextArea txtContenido = new TextArea();
        txtContenido.setPromptText("Escribe el comunicado...");
        txtContenido.setPrefRowCount(4);

        g.add(new Label("Título:"), 0, 0);
        g.add(txtTitulo, 1, 0);
        g.add(new Label("Destinatarios:"), 0, 1);
        g.add(cmbDest, 1, 1);
        g.add(new Label("Prioridad:"), 0, 2);
        g.add(cmbPrioridad, 1, 2);
        g.add(new Label("Contenido:"), 0, 3);
        g.add(txtContenido, 1, 3);

        dialog.getDialogPane().setContent(g);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String tit = txtTitulo.getText().trim();
                String cont = txtContenido.getText().trim();
                if (tit.isEmpty() || cont.isEmpty()) return null;
                Anuncio a = new Anuncio();
                a.setTitulo(tit);
                a.setContenido(cont);
                a.setIdAutor(usuarioActual.getId());
                a.setDestinatarios(cmbDest.getValue());
                a.setPrioridad(cmbPrioridad.getValue());
                a.setActivo(true);
                return a;
            }
            return null;
        });

        Optional<Anuncio> res = dialog.showAndWait();
        res.ifPresent(anuncio -> {
            try {
                dao.crear(anuncio);
                recargar();
            } catch (SQLException e) {
                mostrarAviso("Error al guardar el anuncio: " + e.getMessage());
            }
        });
    }

    private void onEliminarAnuncio() {
        Anuncio a = tabla.getSelectionModel().getSelectedItem();
        if (a == null) {
            mostrarAviso("Selecciona el anuncio que deseas eliminar.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Deseas eliminar el anuncio '" + a.getTitulo() + "'?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        Optional<ButtonType> opt = confirm.showAndWait();
        if (opt.isPresent() && opt.get() == ButtonType.YES) {
            try {
                dao.eliminar(a.getId());
                recargar();
            } catch (SQLException e) {
                mostrarAviso("No se pudo eliminar el anuncio: " + e.getMessage());
            }
        }
    }

    private void mostrarAviso(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
