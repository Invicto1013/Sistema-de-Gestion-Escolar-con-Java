package com.unev.sige.controller;

import com.unev.sige.data.InscripcionDAO;
import com.unev.sige.data.InscripcionInvalidaException;
import com.unev.sige.data.SeccionDAO;
import com.unev.sige.data.UsuarioDAO;
import com.unev.sige.model.Estudiante;
import com.unev.sige.model.Seccion;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Pantalla del administrador para inscribir estudiantes en secciones y retirarlos.
 * Se elige un estudiante; la tabla muestra sus materias de todos los periodos.
 */
public class InscripcionesPanel extends VBox {

    /** Opcion del combo de estudiantes ("Apellido, Nombre (matricula)"). */
    private record OpcionEstudiante(Estudiante estudiante) {
        @Override
        public String toString() {
            return estudiante.getApellido() + ", " + estudiante.getNombre()
                    + " (" + estudiante.getMatricula() + ")";
        }
    }

    /** Opcion del combo de secciones con cupo disponible. */
    private record OpcionSeccion(Seccion seccion) {
        @Override
        public String toString() {
            String profesor = seccion.getProfesor() == null
                    ? "Sin profesor" : seccion.getProfesor().getNombreCompleto();
            return seccion.getAsignatura().getCodigo() + " - " + seccion.getAsignatura().getNombre()
                    + " | " + profesor + " | " + seccion.getHorario() + " | Aula " + seccion.getAula()
                    + " | " + seccion.getInscritos() + "/" + seccion.getCupoMaximo();
        }
    }

    /** Datos que pide el formulario de inscripcion. */
    private record DatosInscripcion(Seccion seccion, String periodo) {
    }

    private static final int MAX_PERIODO = 20; // largo de la columna periodo_lectivo

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final SeccionDAO seccionDAO = new SeccionDAO();
    private final InscripcionDAO inscripcionDAO = new InscripcionDAO();

    private final ComboBox<OpcionEstudiante> cmbEstudiante = new ComboBox<>();
    private final TableView<FilaMateria> tabla = FilaMateria.crearTabla();
    private final Label lblEstado = new Label();

    private String ultimoPeriodo = LocalDate.now().getYear() + "-1";

    public InscripcionesPanel() {
        setSpacing(12);

        cmbEstudiante.setPromptText("Selecciona un estudiante");
        cmbEstudiante.setPrefWidth(420);
        cmbEstudiante.setOnAction(e -> recargar());

        HBox selector = new HBox(10, new Label("Estudiante:"), cmbEstudiante);
        selector.setAlignment(Pos.CENTER_LEFT);

        Button btnInscribir = new Button("Inscribir en sección");
        btnInscribir.setOnAction(e -> onInscribir());
        btnInscribir.disableProperty().bind(cmbEstudiante.valueProperty().isNull());

        Button btnRetirar = new Button("Retirar materia");
        btnRetirar.setOnAction(e -> onRetirar());
        btnRetirar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());

        HBox barra = new HBox(8, btnInscribir, btnRetirar);

        VBox.setVgrow(tabla, Priority.ALWAYS);
        getChildren().addAll(Dialogos.titulo("Inscripciones"), selector, barra, tabla, lblEstado);

        cargarEstudiantes();
    }

    private void cargarEstudiantes() {
        try {
            List<Estudiante> estudiantes = usuarioDAO.listarEstudiantes();
            for (Estudiante e : estudiantes) {
                cmbEstudiante.getItems().add(new OpcionEstudiante(e));
            }
            if (estudiantes.isEmpty()) {
                Dialogos.estado(lblEstado, "Aún no hay estudiantes registrados.", false);
            } else {
                Dialogos.estado(lblEstado, "Selecciona un estudiante para ver sus materias.", false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Dialogos.estado(lblEstado, "No se pudo leer la lista de estudiantes. Revisa la conexión con MySQL.",
                    true);
        }
    }

    private void recargar() {
        OpcionEstudiante elegido = cmbEstudiante.getValue();
        if (elegido == null) {
            tabla.getItems().clear();
            return;
        }

        try {
            List<FilaMateria> filas = FilaMateria.aplanar(
                    inscripcionDAO.listarPorEstudiante(elegido.estudiante().getId()));
            tabla.getItems().setAll(filas);
            Dialogos.estado(lblEstado, filas.isEmpty()
                    ? "Este estudiante no tiene materias inscritas."
                    : "Total: " + filas.size() + " materia(s)", false);
        } catch (SQLException e) {
            e.printStackTrace();
            tabla.getItems().clear();
            Dialogos.estado(lblEstado, "No se pudieron leer las materias. Revisa la conexión con MySQL.", true);
        }
    }

    // ----- Acciones -----

    private void onInscribir() {
        OpcionEstudiante elegido = cmbEstudiante.getValue();
        if (elegido == null) {
            return;
        }

        try {
            List<Seccion> disponibles = seccionDAO.listarTodas().stream()
                    .filter(s -> s.getInscritos() < s.getCupoMaximo())
                    .toList();
            if (disponibles.isEmpty()) {
                Dialogos.error("No hay secciones con cupo disponible. Crea una sección o amplía el cupo "
                        + "desde \"Secciones\".");
                return;
            }

            Optional<DatosInscripcion> datos = pedirDatos(elegido, disponibles);
            if (datos.isPresent()) {
                inscripcionDAO.inscribir(elegido.estudiante().getId(), datos.get().seccion().getId(),
                        datos.get().periodo());
                ultimoPeriodo = datos.get().periodo();
            }
        } catch (InscripcionInvalidaException e) {
            Dialogos.error(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            Dialogos.error("No se pudo completar la inscripción. Revisa la conexión con MySQL.");
        }
        recargar();
    }

    private void onRetirar() {
        FilaMateria fila = tabla.getSelectionModel().getSelectedItem();
        OpcionEstudiante elegido = cmbEstudiante.getValue();
        if (fila == null || elegido == null) {
            return;
        }

        String materia = fila.detalle().getSeccion().getAsignatura().getNombre();
        if (!Dialogos.confirmar("Retirar materia", "¿Retirar a " + elegido.estudiante().getNombreCompleto()
                + " de " + materia + " (" + fila.inscripcion().getPeriodoLectivo() + ")?\n\n"
                + "Se borrará también su calificación en esa materia.")) {
            return;
        }

        try {
            inscripcionDAO.retirar(fila.detalle().getId());
        } catch (SQLException e) {
            e.printStackTrace();
            Dialogos.error("No se pudo retirar la materia. Revisa la conexión con MySQL.");
        }
        recargar();
    }

    // ----- Formulario -----

    private Optional<DatosInscripcion> pedirDatos(OpcionEstudiante estudiante, List<Seccion> disponibles) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Inscribir en sección");
        dialogo.setHeaderText("Estudiante: " + estudiante.estudiante().getNombreCompleto());
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<OpcionSeccion> cmbSeccion = new ComboBox<>();
        for (Seccion s : disponibles) {
            cmbSeccion.getItems().add(new OpcionSeccion(s));
        }
        cmbSeccion.setPromptText("Selecciona una sección");
        cmbSeccion.setPrefWidth(560);

        TextField txtPeriodo = new TextField(ultimoPeriodo);
        txtPeriodo.setPromptText("Ej. " + LocalDate.now().getYear() + "-1");

        Label lblError = new Label();
        lblError.setTextFill(Color.RED);
        lblError.setWrapText(true);

        GridPane formulario = new GridPane();
        formulario.setHgap(10);
        formulario.setVgap(10);
        formulario.setPadding(new Insets(16));
        formulario.addRow(0, new Label("Sección:"), cmbSeccion);
        formulario.addRow(1, new Label("Periodo lectivo:"), txtPeriodo);
        formulario.add(lblError, 0, 2, 2, 1);
        dialogo.getDialogPane().setContent(formulario);

        Button botonAceptar = (Button) dialogo.getDialogPane().lookupButton(ButtonType.OK);
        botonAceptar.addEventFilter(ActionEvent.ACTION, evento -> {
            String periodo = txtPeriodo.getText() == null ? "" : txtPeriodo.getText().trim();
            String error = null;
            if (cmbSeccion.getValue() == null) {
                error = "Selecciona una sección.";
            } else if (periodo.isEmpty()) {
                error = "El periodo lectivo es obligatorio.";
            } else if (periodo.length() > MAX_PERIODO) {
                error = "El periodo admite máximo " + MAX_PERIODO + " caracteres.";
            }
            if (error != null) {
                lblError.setText(error);
                evento.consume(); // mantiene el dialogo abierto
            }
        });

        Optional<ButtonType> respuesta = dialogo.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            return Optional.of(new DatosInscripcion(cmbSeccion.getValue().seccion(), txtPeriodo.getText().trim()));
        }
        return Optional.empty();
    }
}
