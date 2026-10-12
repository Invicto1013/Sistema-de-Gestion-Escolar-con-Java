package com.unev.sige.controller;

import com.unev.sige.data.AsignaturaDAO;
import com.unev.sige.data.SeccionDAO;
import com.unev.sige.data.UsuarioDAO;
import com.unev.sige.model.Asignatura;
import com.unev.sige.model.Profesor;
import com.unev.sige.model.Seccion;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Pantalla del administrador para gestionar las secciones (asignatura + profesor + aula + horario + cupo).
 */
public class SeccionesPanel extends VBox {

    /** Opcion del combo de asignaturas (el combo muestra "codigo - nombre"). */
    private record OpcionAsignatura(Asignatura asignatura) {
        @Override
        public String toString() {
            return asignatura.getCodigo() + " - " + asignatura.getNombre();
        }
    }

    /** Opcion del combo de profesores; profesor == null significa "sin profesor". */
    private record OpcionProfesor(Profesor profesor) {
        @Override
        public String toString() {
            return profesor == null
                    ? "(Sin profesor)"
                    : profesor.getNombreCompleto() + " (" + profesor.getCodigoProfesor() + ")";
        }
    }

    private final SeccionDAO seccionDAO = new SeccionDAO();
    private final AsignaturaDAO asignaturaDAO = new AsignaturaDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final TableView<Seccion> tabla = new TableView<>();
    private final Label lblEstado = new Label();

    public SeccionesPanel() {
        setSpacing(12);

        construirTabla();

        Button btnNueva = new Button("Nueva sección");
        btnNueva.setOnAction(e -> onNueva());

        Button btnEditar = new Button("Editar");
        btnEditar.setOnAction(e -> onEditar());
        btnEditar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());

        Button btnEliminar = new Button("Eliminar");
        btnEliminar.setOnAction(e -> onEliminar());
        btnEliminar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());

        HBox barra = new HBox(8, btnNueva, btnEditar, btnEliminar);

        VBox.setVgrow(tabla, Priority.ALWAYS);
        getChildren().addAll(Dialogos.titulo("Secciones"), barra, tabla, lblEstado);

        recargar();
    }

    private void construirTabla() {
        TableColumn<Seccion, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colId.setMaxWidth(60);

        TableColumn<Seccion, String> colAsignatura = new TableColumn<>("Asignatura");
        colAsignatura.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getAsignatura().getCodigo() + " - " + c.getValue().getAsignatura().getNombre()));

        TableColumn<Seccion, String> colProfesor = new TableColumn<>("Profesor");
        colProfesor.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getProfesor() == null ? "Sin asignar" : c.getValue().getProfesor().getNombreCompleto()));

        TableColumn<Seccion, String> colAula = new TableColumn<>("Aula");
        colAula.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAula()));
        colAula.setMaxWidth(100);

        TableColumn<Seccion, String> colHorario = new TableColumn<>("Horario");
        colHorario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHorario()));

        TableColumn<Seccion, String> colCupo = new TableColumn<>("Inscritos / Cupo");
        colCupo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getInscritos() + " / " + c.getValue().getCupoMaximo()));
        colCupo.setMaxWidth(120);

        tabla.getColumns().addAll(colId, colAsignatura, colProfesor, colAula, colHorario, colCupo);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void recargar() {
        try {
            tabla.getItems().setAll(seccionDAO.listarTodas());
            Dialogos.estado(lblEstado, "Total: " + tabla.getItems().size() + " sección(es)", false);
        } catch (SQLException e) {
            e.printStackTrace();
            Dialogos.estado(lblEstado, "No se pudo leer la lista. Revisa la conexión con MySQL.", true);
        }
    }

    // ----- Acciones -----

    private void onNueva() {
        abrirFormulario(null);
    }

    private void onEditar() {
        Seccion seleccionada = tabla.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            abrirFormulario(seleccionada);
        }
    }

    /**
     * Carga los datos de los combos, muestra el formulario y guarda el resultado.
     */
    private void abrirFormulario(Seccion base) {
        try {
            List<Asignatura> asignaturas = asignaturaDAO.listarTodas();
            if (asignaturas.isEmpty()) {
                Dialogos.error("Primero crea al menos una asignatura.");
                return;
            }
            List<Profesor> profesores = usuarioDAO.listarProfesores();

            Optional<Seccion> resultado = pedirDatos(base, asignaturas, profesores);
            if (resultado.isPresent()) {
                if (base == null) {
                    seccionDAO.insertar(resultado.get());
                } else {
                    seccionDAO.actualizar(resultado.get());
                }
                recargar();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Dialogos.error("No se pudo completar la operación. Revisa la conexión con MySQL.");
        }
    }

    private void onEliminar() {
        Seccion seleccionada = tabla.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            return;
        }

        try {
            int inscritos = seccionDAO.contarInscritos(seleccionada.getId());
            if (inscritos > 0) {
                Dialogos.error("No se puede eliminar la sección porque tiene " + inscritos
                        + " estudiante(s) inscrito(s).");
                return;
            }
            if (!Dialogos.confirmar("Eliminar sección", "¿Eliminar la sección " + seleccionada.getId() + " de "
                    + seleccionada.getAsignatura().getNombre() + "?")) {
                return;
            }
            seccionDAO.eliminar(seleccionada.getId());
            recargar();
        } catch (SQLException e) {
            e.printStackTrace();
            Dialogos.error("No se pudo eliminar la sección. Revisa la conexión con MySQL.");
        }
    }

    // ----- Formulario -----

    private Optional<Seccion> pedirDatos(Seccion base, List<Asignatura> asignaturas, List<Profesor> profesores) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle(base == null ? "Nueva sección" : "Editar sección");
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<OpcionAsignatura> cmbAsignatura = new ComboBox<>();
        for (Asignatura a : asignaturas) {
            OpcionAsignatura opcion = new OpcionAsignatura(a);
            cmbAsignatura.getItems().add(opcion);
            if (base != null && base.getAsignatura().getId() == a.getId()) {
                cmbAsignatura.setValue(opcion);
            }
        }
        cmbAsignatura.setPromptText("Selecciona una asignatura");
        cmbAsignatura.setMaxWidth(Double.MAX_VALUE);

        ComboBox<OpcionProfesor> cmbProfesor = new ComboBox<>();
        OpcionProfesor sinProfesor = new OpcionProfesor(null);
        cmbProfesor.getItems().add(sinProfesor);
        cmbProfesor.setValue(sinProfesor);
        for (Profesor p : profesores) {
            OpcionProfesor opcion = new OpcionProfesor(p);
            cmbProfesor.getItems().add(opcion);
            if (base != null && base.getProfesor() != null && base.getProfesor().getId() == p.getId()) {
                cmbProfesor.setValue(opcion);
            }
        }
        cmbProfesor.setMaxWidth(Double.MAX_VALUE);

        TextField txtAula = new TextField(base == null ? "" : base.getAula());
        txtAula.setPromptText("Ej. A-101");

        TextField txtHorario = new TextField(base == null ? "" : base.getHorario());
        txtHorario.setPromptText("Ej. Lun-Mié 8:00-9:30");

        TextField txtCupo = new TextField(base == null ? "" : String.valueOf(base.getCupoMaximo()));
        txtCupo.setPromptText("Ej. 30");

        Label lblError = new Label();
        lblError.getStyleClass().add("error-message");
        lblError.setWrapText(true);

        GridPane formulario = new GridPane();
        formulario.setHgap(10);
        formulario.setVgap(10);
        formulario.setPadding(new Insets(16));
        formulario.addRow(0, new Label("Asignatura:"), cmbAsignatura);
        formulario.addRow(1, new Label("Profesor:"), cmbProfesor);
        formulario.addRow(2, new Label("Aula:"), txtAula);
        formulario.addRow(3, new Label("Horario:"), txtHorario);
        formulario.addRow(4, new Label("Cupo máximo:"), txtCupo);
        formulario.add(lblError, 0, 5, 2, 1);
        dialogo.getDialogPane().setContent(formulario);

        int idActual = base == null ? 0 : base.getId();

        Button botonAceptar = (Button) dialogo.getDialogPane().lookupButton(ButtonType.OK);
        botonAceptar.addEventFilter(ActionEvent.ACTION, evento -> {
            String error = validar(cmbAsignatura.getValue(), txtAula.getText(), txtHorario.getText(),
                    txtCupo.getText(), idActual);
            if (error != null) {
                lblError.setText(error);
                evento.consume(); // mantiene el dialogo abierto
            }
        });

        Dialogos.prepararModal(dialogo, getScene().getWindow());
        Optional<ButtonType> respuesta = dialogo.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            return Optional.of(new Seccion(idActual, txtAula.getText().trim(), txtHorario.getText().trim(),
                    Integer.parseInt(txtCupo.getText().trim()), cmbAsignatura.getValue().asignatura(),
                    cmbProfesor.getValue().profesor()));
        }
        return Optional.empty();
    }

    /**
     * Devuelve el mensaje de error o null si los datos son validos.
     * Los limites de longitud son los de las columnas de la tabla `secciones`.
     */
    private String validar(OpcionAsignatura asignatura, String aula, String horario, String cupo, int idActual) {
        aula = aula == null ? "" : aula.trim();
        horario = horario == null ? "" : horario.trim();
        cupo = cupo == null ? "" : cupo.trim();

        if (asignatura == null) {
            return "Selecciona una asignatura.";
        }
        if (aula.isEmpty() || horario.isEmpty() || cupo.isEmpty()) {
            return "Aula, horario y cupo son obligatorios.";
        }
        if (aula.length() > 20) {
            return "El aula admite máximo 20 caracteres.";
        }
        if (horario.length() > 50) {
            return "El horario admite máximo 50 caracteres.";
        }

        int valorCupo;
        try {
            valorCupo = Integer.parseInt(cupo);
        } catch (NumberFormatException e) {
            return "El cupo debe ser un número entero.";
        }
        if (valorCupo < 1 || valorCupo > 500) {
            return "El cupo debe estar entre 1 y 500.";
        }

        if (idActual != 0) {
            try {
                int inscritos = seccionDAO.contarInscritos(idActual);
                if (valorCupo < inscritos) {
                    return "El cupo no puede ser menor que los " + inscritos + " estudiante(s) ya inscrito(s).";
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return "No se pudo verificar los inscritos. Revisa la conexión con MySQL.";
            }
        }
        return null;
    }
}
