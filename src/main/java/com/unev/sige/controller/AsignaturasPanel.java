package com.unev.sige.controller;

import com.unev.sige.data.AsignaturaDAO;
import com.unev.sige.model.Asignatura;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Pantalla del administrador para gestionar las asignaturas (crear, editar y eliminar).
 */
public class AsignaturasPanel extends VBox {

    private final AsignaturaDAO dao = new AsignaturaDAO();
    private final TableView<Asignatura> tabla = new TableView<>();
    private final Label lblEstado = new Label();

    public AsignaturasPanel() {
        setSpacing(12);

        construirTabla();

        Button btnNueva = new Button("Nueva asignatura");
        btnNueva.setOnAction(e -> onNueva());

        Button btnEditar = new Button("Editar");
        btnEditar.setOnAction(e -> onEditar());
        btnEditar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());

        Button btnEliminar = new Button("Eliminar");
        btnEliminar.setOnAction(e -> onEliminar());
        btnEliminar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());

        HBox barra = new HBox(8, btnNueva, btnEditar, btnEliminar);

        VBox.setVgrow(tabla, Priority.ALWAYS);
        getChildren().addAll(Dialogos.titulo("Asignaturas"), barra, tabla, lblEstado);

        recargar();
    }

    private void construirTabla() {
        TableColumn<Asignatura, String> colCodigo = new TableColumn<>("Código");
        colCodigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colCodigo.setMaxWidth(120);

        TableColumn<Asignatura, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));

        TableColumn<Asignatura, Number> colCreditos = new TableColumn<>("Créditos");
        colCreditos.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getCreditos()));
        colCreditos.setMaxWidth(90);

        TableColumn<Asignatura, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDescripcion() == null ? "" : c.getValue().getDescripcion()));

        tabla.getColumns().addAll(colCodigo, colNombre, colCreditos, colDescripcion);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void recargar() {
        try {
            tabla.getItems().setAll(dao.listarTodas());
            Dialogos.estado(lblEstado, "Total: " + tabla.getItems().size() + " asignatura(s)", false);
        } catch (SQLException e) {
            e.printStackTrace();
            Dialogos.estado(lblEstado, "No se pudo leer la lista. Revisa la conexión con MySQL.", true);
        }
    }

    // ----- Acciones -----

    private void onNueva() {
        pedirDatos(null).ifPresent(asignatura -> guardar(asignatura, true));
    }

    private void onEditar() {
        Asignatura seleccionada = tabla.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            return;
        }
        pedirDatos(seleccionada).ifPresent(asignatura -> guardar(asignatura, false));
    }

    private void onEliminar() {
        Asignatura seleccionada = tabla.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            return;
        }

        try {
            if (dao.tieneSecciones(seleccionada.getId())) {
                Dialogos.error("No se puede eliminar \"" + seleccionada.getNombre()
                        + "\" porque tiene secciones. Elimina primero sus secciones.");
                return;
            }
            if (!Dialogos.confirmar("Eliminar asignatura",
                    "¿Eliminar la asignatura " + seleccionada.getCodigo() + " - " + seleccionada.getNombre() + "?")) {
                return;
            }
            dao.eliminar(seleccionada.getId());
            recargar();
        } catch (SQLException e) {
            e.printStackTrace();
            Dialogos.error("No se pudo eliminar la asignatura. Revisa la conexión con MySQL.");
        }
    }

    private void guardar(Asignatura asignatura, boolean esNueva) {
        try {
            if (esNueva) {
                dao.insertar(asignatura);
            } else {
                dao.actualizar(asignatura);
            }
            recargar();
        } catch (SQLException e) {
            e.printStackTrace();
            Dialogos.error("No se pudo guardar la asignatura. Revisa la conexión con MySQL.");
        }
    }

    // ----- Formulario -----

    /**
     * Muestra el formulario (vacio para una asignatura nueva, con datos para editar).
     * El dialogo no se cierra mientras haya errores de validacion.
     */
    private Optional<Asignatura> pedirDatos(Asignatura base) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle(base == null ? "Nueva asignatura" : "Editar asignatura");
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField txtCodigo = new TextField(base == null ? "" : base.getCodigo());
        txtCodigo.setPromptText("Ej. MAT-101");

        TextField txtNombre = new TextField(base == null ? "" : base.getNombre());
        txtNombre.setPromptText("Ej. Matemática");

        TextField txtCreditos = new TextField(base == null ? "" : String.valueOf(base.getCreditos()));
        txtCreditos.setPromptText("Ej. 3");

        TextArea txtDescripcion = new TextArea(base == null || base.getDescripcion() == null
                ? "" : base.getDescripcion());
        txtDescripcion.setPrefRowCount(3);
        txtDescripcion.setWrapText(true);

        Label lblError = new Label();
        lblError.getStyleClass().add("error-message");
        lblError.setWrapText(true);

        GridPane formulario = new GridPane();
        formulario.setHgap(10);
        formulario.setVgap(10);
        formulario.setPadding(new Insets(16));
        formulario.addRow(0, new Label("Código:"), txtCodigo);
        formulario.addRow(1, new Label("Nombre:"), txtNombre);
        formulario.addRow(2, new Label("Créditos:"), txtCreditos);
        formulario.addRow(3, new Label("Descripción:"), txtDescripcion);
        formulario.add(lblError, 0, 4, 2, 1);
        dialogo.getDialogPane().setContent(formulario);

        int idActual = base == null ? 0 : base.getId();

        Button botonAceptar = (Button) dialogo.getDialogPane().lookupButton(ButtonType.OK);
        botonAceptar.addEventFilter(ActionEvent.ACTION, evento -> {
            String error = validar(txtCodigo.getText(), txtNombre.getText(), txtCreditos.getText(), idActual);
            if (error != null) {
                lblError.setText(error);
                evento.consume(); // mantiene el dialogo abierto
            }
        });

        Dialogos.prepararModal(dialogo, getScene().getWindow());
        Optional<ButtonType> respuesta = dialogo.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            String descripcion = txtDescripcion.getText().trim();
            return Optional.of(new Asignatura(idActual, txtCodigo.getText().trim(), txtNombre.getText().trim(),
                    Integer.parseInt(txtCreditos.getText().trim()), descripcion.isEmpty() ? null : descripcion));
        }
        return Optional.empty();
    }

    /**
     * Devuelve el mensaje de error o null si los datos son validos.
     * Los limites de longitud son los de las columnas de la tabla `asignaturas`.
     */
    private String validar(String codigo, String nombre, String creditos, int idActual) {
        codigo = codigo == null ? "" : codigo.trim();
        nombre = nombre == null ? "" : nombre.trim();
        creditos = creditos == null ? "" : creditos.trim();

        if (codigo.isEmpty() || nombre.isEmpty() || creditos.isEmpty()) {
            return "Código, nombre y créditos son obligatorios.";
        }
        if (codigo.length() > 10) {
            return "El código admite máximo 10 caracteres.";
        }
        if (nombre.length() > 100) {
            return "El nombre admite máximo 100 caracteres.";
        }

        int valorCreditos;
        try {
            valorCreditos = Integer.parseInt(creditos);
        } catch (NumberFormatException e) {
            return "Los créditos deben ser un número entero.";
        }
        if (valorCreditos < 0 || valorCreditos > 99) {
            return "Los créditos deben estar entre 0 y 99.";
        }

        try {
            if (dao.existeCodigo(codigo, idActual)) {
                return "Ya existe una asignatura con ese código.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "No se pudo verificar el código. Revisa la conexión con MySQL.";
        }
        return null;
    }
}
