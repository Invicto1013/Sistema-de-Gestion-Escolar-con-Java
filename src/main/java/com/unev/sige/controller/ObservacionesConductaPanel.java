package com.unev.sige.controller;

import com.unev.sige.data.ObservacionConductaDAO;
import com.unev.sige.data.UsuarioDAO;
import com.unev.sige.model.Estudiante;
import com.unev.sige.model.ObservacionConducta;
import com.unev.sige.model.Permisos;
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
 * Panel JavaFX para registrar y consultar méritos, incidencias y conducta escolar.
 */
public final class ObservacionesConductaPanel extends BorderPane {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Usuario usuarioActual;
    private final ObservacionConductaDAO dao = new ObservacionConductaDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final TableView<ObservacionConducta> tabla = new TableView<>();
    private final Label lblEstado = new Label();
    private final TablaPaginada<ObservacionConducta> vista = new TablaPaginada<>(tabla, o ->
            o.getTipo() + " " + o.getEstudianteNombre() + " " + o.getMatricula() + " " + o.getTitulo() + " " + o.getProfesorNombre());

    public ObservacionesConductaPanel(Usuario usuario) {
        this.usuarioActual = usuario;
        setPadding(new Insets(10));

        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        columna("Fecha", o -> o.getFecha() != null ? o.getFecha().format(FORMATO_FECHA) : "—");
        columna("Tipo", ObservacionConducta::getTipo);
        columna("Estudiante", ObservacionConducta::getEstudianteNombre);
        columna("Matrícula", ObservacionConducta::getMatricula);
        columna("Título", ObservacionConducta::getTitulo);
        columna("Profesor", ObservacionConducta::getProfesorNombre);
        columna("Detalle", o -> o.getDescripcion() != null && o.getDescripcion().length() > 50
                ? o.getDescripcion().substring(0, 47) + "..." : o.getDescripcion());

        HBox barra = new HBox(10);
        barra.setPadding(new Insets(0, 0, 10, 0));

        Button btnDetalle = new Button("Ver detalle");
        btnDetalle.setOnAction(e -> onVerDetalle());
        barra.getChildren().add(btnDetalle);

        if (Permisos.registraConducta(usuario.getRol())) {
            Button btnNuevo = new Button("Registrar observación");
            btnNuevo.getStyleClass().add("primary");
            btnNuevo.setOnAction(e -> onNuevaObservacion());

            Button btnEliminar = new Button("Eliminar");
            btnEliminar.setOnAction(e -> onEliminarObservacion());

            barra.getChildren().addAll(btnNuevo, btnEliminar);
        }

        barra.getChildren().add(lblEstado);
        setTop(barra);

        vista.setTituloPdf("Registro de Observaciones y Conducta");
        setCenter(vista);

        recargar();
    }

    private void columna(String nombre, java.util.function.Function<ObservacionConducta, String> valor) {
        TableColumn<ObservacionConducta, String> c = new TableColumn<>(nombre);
        c.setCellValueFactory(f -> new SimpleStringProperty(valor.apply(f.getValue())));
        tabla.getColumns().add(c);
    }

    private void recargar() {
        try {
            List<ObservacionConducta> lista;
            if (Permisos.veConductaCompleta(usuarioActual.getRol())) {
                lista = dao.listarTodas();
            } else if (usuarioActual.getRol() == Rol.PROFESOR) {
                lista = dao.listarPorProfesor(usuarioActual.getId());
            } else if (usuarioActual.getRol() == Rol.ESTUDIANTE) {
                lista = dao.listarPorEstudiante(usuarioActual.getId());
            } else {
                lista = List.of();
            }
            vista.setDatos(lista);
            lblEstado.setText(lista.size() + " registros de conducta");
        } catch (SQLException e) {
            lblEstado.setText("No se pudieron cargar las observaciones.");
        }
    }

    private void onVerDetalle() {
        ObservacionConducta o = tabla.getSelectionModel().getSelectedItem();
        if (o == null) {
            mostrarAviso("Selecciona un registro para ver el detalle.");
            return;
        }
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Detalle de observación");
        dialog.setHeaderText("[" + o.getTipo() + "] " + o.getTitulo());
        dialog.setContentText("Estudiante: " + o.getEstudianteNombre() + " (" + o.getMatricula() + ")\n"
                + "Profesor: " + o.getProfesorNombre() + "\n"
                + "Fecha: " + (o.getFecha() != null ? o.getFecha().format(FORMATO_FECHA) : "") + "\n\n"
                + "Descripción:\n" + o.getDescripcion());
        dialog.showAndWait();
    }

    private void onNuevaObservacion() {
        Dialog<ObservacionConducta> dialog = new Dialog<>();
        dialog.setTitle("Nueva observación estudiantil");
        dialog.setHeaderText("Registrar mérito, tardanza o incidencia");

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(16));

        ComboBox<Estudiante> cmbEstudiantes = new ComboBox<>();
        cmbEstudiantes.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Estudiante e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? null : e.getNombreCompleto() + " (" + e.getMatricula() + ")");
            }
        });
        cmbEstudiantes.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Estudiante e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? null : e.getNombreCompleto() + " (" + e.getMatricula() + ")");
            }
        });
        try {
            cmbEstudiantes.getItems().setAll(usuarioDAO.listarEstudiantes());
        } catch (SQLException ignored) {
        }
        cmbEstudiantes.setPromptText("Seleccionar estudiante...");

        ComboBox<String> cmbTipo = new ComboBox<>();
        cmbTipo.getItems().addAll("MERITO", "CONDUCTA", "TARDANZA", "TAREA", "NOTA_INFORMATIVA");
        cmbTipo.setValue("MERITO");

        TextField txtTitulo = new TextField();
        txtTitulo.setPromptText("Ej. Excelente participación en ciencias / Tarea no entregada");

        TextArea txtDesc = new TextArea();
        txtDesc.setPromptText("Detalles adicionales...");
        txtDesc.setPrefRowCount(3);

        g.add(new Label("Estudiante:"), 0, 0);
        g.add(cmbEstudiantes, 1, 0);
        g.add(new Label("Tipo de registro:"), 0, 1);
        g.add(cmbTipo, 1, 1);
        g.add(new Label("Motivo / Título:"), 0, 2);
        g.add(txtTitulo, 1, 2);
        g.add(new Label("Descripción:"), 0, 3);
        g.add(txtDesc, 1, 3);

        dialog.getDialogPane().setContent(g);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Estudiante est = cmbEstudiantes.getValue();
                String tit = txtTitulo.getText().trim();
                String desc = txtDesc.getText().trim();
                if (est == null || tit.isEmpty()) return null;

                ObservacionConducta o = new ObservacionConducta();
                o.setIdEstudiante(est.getId());
                o.setIdProfesor(usuarioActual.getId());
                o.setTipo(cmbTipo.getValue());
                o.setTitulo(tit);
                o.setDescripcion(desc.isEmpty() ? tit : desc);
                return o;
            }
            return null;
        });

        Optional<ObservacionConducta> res = dialog.showAndWait();
        res.ifPresent(obs -> {
            try {
                dao.registrar(obs);
                recargar();
            } catch (SQLException e) {
                mostrarAviso("No se pudo guardar la observación: " + e.getMessage());
            }
        });
    }

    private void onEliminarObservacion() {
        ObservacionConducta o = tabla.getSelectionModel().getSelectedItem();
        if (o == null) {
            mostrarAviso("Selecciona la observación que deseas eliminar.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Deseas eliminar la observación de " + o.getEstudianteNombre() + "?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        Optional<ButtonType> opt = confirm.showAndWait();
        if (opt.isPresent() && opt.get() == ButtonType.YES) {
            try {
                if (Permisos.eliminaConductaAjena(usuarioActual.getRol())) dao.eliminar(o.getId());
                else dao.eliminar(o.getId(), usuarioActual.getId());
                recargar();
            } catch (SQLException e) {
                mostrarAviso("No se pudo eliminar: " + e.getMessage());
            }
        }
    }

    private void mostrarAviso(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
