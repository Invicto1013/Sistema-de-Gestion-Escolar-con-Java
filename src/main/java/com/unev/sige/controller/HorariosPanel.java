package com.unev.sige.controller;

import com.unev.sige.data.HorarioDAO;
import com.unev.sige.data.SeccionDAO;
import com.unev.sige.model.HorarioSeccion;
import com.unev.sige.model.Rol;
import com.unev.sige.model.Seccion;
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
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Panel JavaFX para planificar y consultar los horarios semanales de clases.
 */
public final class HorariosPanel extends BorderPane {

    private final Usuario usuarioActual;
    private final HorarioDAO horarioDAO = new HorarioDAO();
    private final SeccionDAO seccionDAO = new SeccionDAO();
    private final TableView<HorarioSeccion> tabla = new TableView<>();
    private final Label lblEstado = new Label();
    private final ComboBox<Seccion> cmbSecciones = new ComboBox<>();
    private final TablaPaginada<HorarioSeccion> vista = new TablaPaginada<>(tabla, h ->
            h.getDiaSemana() + " " + h.getAsignaturaNombre() + " " + h.getProfesorNombre() + " " + h.getAula());

    public HorariosPanel(Usuario usuario) {
        this.usuarioActual = usuario;
        setPadding(new Insets(10));

        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        columna("Día", HorarioSeccion::getDiaSemana);
        columna("Horario", HorarioSeccion::getRangoHora);
        columna("Asignatura", HorarioSeccion::getAsignaturaNombre);
        columna("Profesor", HorarioSeccion::getProfesorNombre);
        columna("Aula", HorarioSeccion::getAula);

        HBox barra = new HBox(10);
        barra.setPadding(new Insets(0, 0, 10, 0));

        if (usuario.getRol() == Rol.ADMIN) {
            cmbSecciones.setPromptText("Filtrar por sección...");
            cmbSecciones.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
                @Override protected void updateItem(Seccion s, boolean empty) {
                    super.updateItem(s, empty);
                    setText(empty || s == null ? null : (s.getAsignatura() != null ? s.getAsignatura().getNombre() : "Asignatura") + " · Sección #" + s.getId() + " (" + (s.getAula() != null ? s.getAula() : "Aula") + ")");
                }
            });
            cmbSecciones.setButtonCell(new javafx.scene.control.ListCell<>() {
                @Override protected void updateItem(Seccion s, boolean empty) {
                    super.updateItem(s, empty);
                    setText(empty || s == null ? null : (s.getAsignatura() != null ? s.getAsignatura().getNombre() : "Asignatura") + " · Sección #" + s.getId() + " (" + (s.getAula() != null ? s.getAula() : "Aula") + ")");
                }
            });
            cargarSecciones();
            cmbSecciones.setOnAction(e -> {
                Seccion s = cmbSecciones.getValue();
                if (s != null) cargarPorSeccion(s.getId());
            });

            Button btnNuevo = new Button("Asignar bloque");
            btnNuevo.getStyleClass().add("primary");
            btnNuevo.setOnAction(e -> onNuevoHorario());

            Button btnEliminar = new Button("Eliminar bloque");
            btnEliminar.setOnAction(e -> onEliminarHorario());

            barra.getChildren().addAll(new Label("Sección:"), cmbSecciones, btnNuevo, btnEliminar);
        }

        barra.getChildren().add(lblEstado);
        setTop(barra);

        vista.setTituloPdf("Horarios Escolares Semanales");
        setCenter(vista);

        recargarInicial();
    }

    private void columna(String nombre, java.util.function.Function<HorarioSeccion, String> valor) {
        TableColumn<HorarioSeccion, String> c = new TableColumn<>(nombre);
        c.setCellValueFactory(f -> new SimpleStringProperty(valor.apply(f.getValue())));
        tabla.getColumns().add(c);
    }

    private void cargarSecciones() {
        try {
            List<Seccion> lista = seccionDAO.listarTodas();
            cmbSecciones.getItems().setAll(lista);
        } catch (SQLException ignored) {
        }
    }

    private void recargarInicial() {
        try {
            List<HorarioSeccion> lista;
            if (usuarioActual.getRol() == Rol.PROFESOR) {
                lista = horarioDAO.listarPorProfesor(usuarioActual.getId());
            } else if (usuarioActual.getRol() == Rol.ESTUDIANTE) {
                lista = horarioDAO.listarPorEstudiante(usuarioActual.getId());
            } else {
                if (!cmbSecciones.getItems().isEmpty()) {
                    cmbSecciones.getSelectionModel().selectFirst();
                    cargarPorSeccion(cmbSecciones.getValue().getId());
                    return;
                }
                lista = List.of();
            }
            vista.setDatos(lista);
            lblEstado.setText(lista.size() + " bloques de clase registrados");
        } catch (SQLException e) {
            lblEstado.setText("No se pudieron cargar los horarios.");
        }
    }

    private void cargarPorSeccion(int idSeccion) {
        try {
            List<HorarioSeccion> lista = horarioDAO.listarPorSeccion(idSeccion);
            vista.setDatos(lista);
            lblEstado.setText(lista.size() + " bloques en esta sección");
        } catch (SQLException e) {
            lblEstado.setText("Error al cargar horarios de la sección.");
        }
    }

    private void onNuevoHorario() {
        Seccion secSeleccionada = cmbSecciones.getValue();
        if (secSeleccionada == null) {
            mostrarAviso("Selecciona primero una sección para asignarle un horario.");
            return;
        }

        Dialog<HorarioSeccion> dialog = new Dialog<>();
        dialog.setTitle("Asignar bloque de clase");
        dialog.setHeaderText("Asignar día y hora a: " + secSeleccionada);

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(16));

        ComboBox<String> cmbDia = new ComboBox<>();
        cmbDia.getItems().addAll("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO");
        cmbDia.setValue("LUNES");

        TextField txtInicio = new TextField("08:00");
        TextField txtFin = new TextField("09:30");
        TextField txtAula = new TextField(secSeleccionada.getAula() != null ? secSeleccionada.getAula() : "Aula 101");

        g.add(new Label("Día de la semana:"), 0, 0);
        g.add(cmbDia, 1, 0);
        g.add(new Label("Hora de inicio (HH:mm):"), 0, 1);
        g.add(txtInicio, 1, 1);
        g.add(new Label("Hora de fin (HH:mm):"), 0, 2);
        g.add(txtFin, 1, 2);
        g.add(new Label("Aula física:"), 0, 3);
        g.add(txtAula, 1, 3);

        dialog.getDialogPane().setContent(g);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    LocalTime ini = LocalTime.parse(txtInicio.getText().trim());
                    LocalTime fin = LocalTime.parse(txtFin.getText().trim());
                    if (!fin.isAfter(ini)) {
                        mostrarAviso("La hora de fin debe ser posterior a la de inicio.");
                        return null;
                    }
                    return new HorarioSeccion(0, secSeleccionada.getId(), cmbDia.getValue(), ini, fin, txtAula.getText().trim());
                } catch (DateTimeParseException e) {
                    mostrarAviso("Formato de hora incorrecto. Usa formato 24 horas, ej. 08:30 o 14:00.");
                    return null;
                }
            }
            return null;
        });

        Optional<HorarioSeccion> res = dialog.showAndWait();
        res.ifPresent(h -> {
            try {
                if (horarioDAO.hayConflictoProfesor(h.getIdSeccion(), h.getDiaSemana(), h.getHoraInicio(), h.getHoraFin(), 0)) {
                    mostrarAviso("¡Conflicto de horario! El profesor asignado ya tiene otra clase en ese mismo día y rango de horas.");
                    return;
                }
                horarioDAO.agregar(h);
                cargarPorSeccion(secSeleccionada.getId());
            } catch (SQLException e) {
                mostrarAviso("No se pudo guardar el bloque horario: " + e.getMessage());
            }
        });
    }

    private void onEliminarHorario() {
        HorarioSeccion h = tabla.getSelectionModel().getSelectedItem();
        if (h == null) {
            mostrarAviso("Selecciona el bloque de horario que deseas eliminar.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar la clase de " + h.getDiaSemana() + " (" + h.getRangoHora() + ")?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        Optional<ButtonType> opt = confirm.showAndWait();
        if (opt.isPresent() && opt.get() == ButtonType.YES) {
            try {
                horarioDAO.eliminar(h.getId());
                if (cmbSecciones.getValue() != null) cargarPorSeccion(cmbSecciones.getValue().getId());
                else recargarInicial();
            } catch (SQLException e) {
                mostrarAviso("No se pudo eliminar el bloque: " + e.getMessage());
            }
        }
    }

    private void mostrarAviso(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
