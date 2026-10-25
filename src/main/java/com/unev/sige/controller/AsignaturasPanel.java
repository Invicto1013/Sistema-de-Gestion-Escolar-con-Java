package com.unev.sige.controller;

import com.unev.sige.data.AsignaturaDAO;
import com.unev.sige.data.CurriculoDAO;
import com.unev.sige.model.Asignatura;
import com.unev.sige.model.GradoCurricular;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.Optional;
import java.util.List;

/**
 * Pantalla del administrador para gestionar las asignaturas (crear, editar y eliminar).
 */
public class AsignaturasPanel extends VBox {

    private record OpcionArea(String codigo, String nombre) { @Override public String toString() { return nombre; } }

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
        colCodigo.setMinWidth(85);
        colCodigo.setPrefWidth(100);
        colCodigo.setMaxWidth(125);

        TableColumn<Asignatura, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colNombre.setMinWidth(120);
        colNombre.setPrefWidth(155);

        TableColumn<Asignatura, Number> colCreditos = new TableColumn<>("Créditos");
        colCreditos.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getCreditos()));
        colCreditos.setMinWidth(75);
        colCreditos.setPrefWidth(90);
        colCreditos.setMaxWidth(100);

        TableColumn<Asignatura, String> colArea = new TableColumn<>("Área curricular");
        colArea.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAreaCurricular() == null ? "Pendiente de clasificar" : c.getValue().getAreaCurricular()));
        colArea.setMinWidth(150);
        colArea.setPrefWidth(190);
        TableColumn<Asignatura, String> colGrados = new TableColumn<>("Grados");
        colGrados.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGradosCurriculares() == null ? "Pendiente de clasificar" : c.getValue().getGradosCurriculares()));
        colGrados.setMinWidth(105);
        colGrados.setPrefWidth(145);
        colGrados.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) { setText(null); setTooltip(null); return; }
                setText(item.equals("Pendiente de clasificar") ? item : item.split(",").length + " grados");
                setTooltip(new Tooltip(item));
            }
        });
        TableColumn<Asignatura, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDescripcion() == null ? "" : c.getValue().getDescripcion()));

        tabla.getColumns().addAll(colCodigo, colNombre, colArea, colGrados, colCreditos, colDescripcion);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
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
            if (esNueva) dao.insertar(asignatura); else dao.actualizar(asignatura);
            List<Integer> grados = java.util.Arrays.stream(asignatura.getIdsGrados().split(","))
                    .map(Integer::parseInt).toList();
            dao.guardarCurriculo(asignatura.getId(), asignatura.getCodigoArea(), grados);
            recargar();
        } catch (SQLException e) {
            e.printStackTrace();
            Dialogos.error("No se pudo guardar la asignatura y su plan curricular. Revisa MySQL.");
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

        ComboBox<OpcionArea> cmbArea = new ComboBox<>();
        cmbArea.setPromptText("Selecciona el área curricular");
        cmbArea.setMaxWidth(Double.MAX_VALUE);
        ListView<GradoCurricular> listaGrados = new ListView<>();
        listaGrados.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listaGrados.setPrefHeight(145);
        try {
            CurriculoDAO curriculo = new CurriculoDAO();
            curriculo.listarAreas().forEach((codigo,nombre) -> {
                OpcionArea opcion = new OpcionArea(codigo,nombre);
                cmbArea.getItems().add(opcion);
                if (base != null && codigo.equals(base.getCodigoArea())) cmbArea.setValue(opcion);
            });
            listaGrados.getItems().setAll(curriculo.listarGrados());
            if (base != null && base.getIdsGrados() != null) {
                for (String rawId : base.getIdsGrados().split(",")) {
                    try {
                        int idGrado = Integer.parseInt(rawId);
                        listaGrados.getItems().stream().filter(g -> g.id() == idGrado).findFirst()
                                .ifPresent(g -> listaGrados.getSelectionModel().select(g));
                    } catch (NumberFormatException ignored) { }
                }
            }
        } catch (SQLException e) {
            Dialogos.error("No se pudo cargar el catálogo curricular. Verifica la conexión y ejecuta la migración.");
            return Optional.empty();
        }

        TextArea txtDescripcion = new TextArea(base == null || base.getDescripcion() == null ? "" : base.getDescripcion());
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
        formulario.addRow(2, new Label("Créditos / carga:"), txtCreditos);
        formulario.addRow(3, new Label("Área curricular:"), cmbArea);
        formulario.addRow(4, new Label("Grados (puedes elegir varios):"), listaGrados);
        formulario.addRow(5, new Label("Descripción:"), txtDescripcion);
        formulario.add(lblError, 0, 6, 2, 1);
        dialogo.getDialogPane().setContent(formulario);

        int idActual = base == null ? 0 : base.getId();
        Button botonAceptar = (Button) dialogo.getDialogPane().lookupButton(ButtonType.OK);
        botonAceptar.addEventFilter(ActionEvent.ACTION, evento -> {
            String error = validar(txtCodigo.getText(), txtNombre.getText(), txtCreditos.getText(), idActual);
            if (error == null && cmbArea.getValue() == null) error = "Selecciona el área curricular.";
            if (error == null && listaGrados.getSelectionModel().getSelectedItems().isEmpty()) error = "Selecciona al menos un grado.";
            if (error != null) {
                lblError.setText(error);
                evento.consume();
            }
        });

        Dialogos.prepararModal(dialogo, getScene().getWindow());
        Optional<ButtonType> respuesta = dialogo.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            String descripcion = txtDescripcion.getText().trim();
            Asignatura asignatura = new Asignatura(idActual, txtCodigo.getText().trim(), txtNombre.getText().trim(),
                    Integer.parseInt(txtCreditos.getText().trim()), descripcion.isEmpty() ? null : descripcion);
            asignatura.setCodigoArea(cmbArea.getValue().codigo());
            asignatura.setIdsGrados(listaGrados.getSelectionModel().getSelectedItems().stream()
                    .map(g -> String.valueOf(g.id())).collect(java.util.stream.Collectors.joining(",")));
            return Optional.of(asignatura);
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
