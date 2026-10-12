package com.unev.sige.controller;

import com.unev.sige.data.AsistenciaDAO;
import com.unev.sige.data.SeccionDAO;
import com.unev.sige.model.FilaAsistencia;
import com.unev.sige.model.Seccion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Toma de asistencia del profesor por sección y fecha. */
public class AsistenciaPanel extends VBox {
    private record OpcionSeccion(Seccion seccion) {
        @Override public String toString() {
            return seccion.getAsignatura().getCodigo() + " - " + seccion.getAsignatura().getNombre()
                    + " | " + seccion.getHorario() + " | Aula " + seccion.getAula();
        }
    }
    private static final List<String> ESTADOS = List.of("Sin registrar", "Presente", "Ausente");
    private static final class FilaUI {
        final FilaAsistencia fila;
        final SimpleStringProperty estado;
        FilaUI(FilaAsistencia fila) {
            this.fila = fila;
            this.estado = new SimpleStringProperty(fila.presente() == null ? ESTADOS.get(0)
                    : fila.presente() ? ESTADOS.get(1) : ESTADOS.get(2));
        }
        FilaAsistencia guardar(LocalDate fecha) {
            Boolean presente = estado.get().equals(ESTADOS.get(1)) ? Boolean.TRUE
                    : estado.get().equals(ESTADOS.get(2)) ? Boolean.FALSE : null;
            return new FilaAsistencia(fila.idDetalleInscripcion(), fila.matricula(), fila.estudiante(), fecha, presente);
        }
    }

    private final int idProfesor;
    private final AsistenciaDAO asistenciaDAO = new AsistenciaDAO();
    private final ComboBox<OpcionSeccion> secciones = new ComboBox<>();
    private final DatePicker fecha = new DatePicker(LocalDate.now());
    private final TableView<FilaUI> tabla = new TableView<>();
    private final Label estado = new Label();

    public AsistenciaPanel(int idProfesor) {
        this.idProfesor = idProfesor;
        setSpacing(12);
        secciones.setPromptText("Selecciona una sección");
        secciones.setPrefWidth(490);
        secciones.setOnAction(e -> recargar());
        fecha.setOnAction(e -> recargar());
        HBox controles = new HBox(10, new Label("Sección:"), secciones, new Label("Fecha:"), fecha);
        controles.setAlignment(Pos.CENTER_LEFT);

        TableColumn<FilaUI, String> matricula = new TableColumn<>("Matrícula");
        matricula.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fila.matricula()));
        matricula.setMaxWidth(130);
        TableColumn<FilaUI, String> estudiante = new TableColumn<>("Estudiante");
        estudiante.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fila.estudiante()));
        TableColumn<FilaUI, String> asistencia = new TableColumn<>("Asistencia");
        asistencia.setCellValueFactory(c -> c.getValue().estado);
        asistencia.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(ESTADOS)));
        asistencia.setEditable(true);
        tabla.getColumns().addAll(matricula, estudiante, asistencia);
        tabla.setEditable(true);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button guardar = new Button("Guardar asistencia");
        guardar.setOnAction(e -> guardar());
        getChildren().addAll(Dialogos.titulo("Asistencia"), controles, guardar, tabla, estado);
        cargarSecciones();
    }

    private void cargarSecciones() {
        try {
            List<Seccion> lista = new SeccionDAO().listarPorProfesor(idProfesor);
            lista.forEach(s -> secciones.getItems().add(new OpcionSeccion(s)));
            if (lista.isEmpty()) Dialogos.estado(estado, "Aún no tienes secciones asignadas.", false);
            else {
                secciones.setValue(secciones.getItems().get(0));
                recargar();
            }
        } catch (SQLException e) {
            Dialogos.estado(estado, "No se pudieron leer tus secciones. Revisa la conexión con MySQL.", true);
        }
    }

    private void recargar() {
        OpcionSeccion seleccion = secciones.getValue();
        LocalDate dia = fecha.getValue();
        if (seleccion == null || dia == null) return;
        try {
            List<FilaUI> filas = asistenciaDAO.listarPorSeccionYFecha(seleccion.seccion().getId(), dia)
                    .stream().map(FilaUI::new).toList();
            tabla.getItems().setAll(filas);
            Dialogos.estado(estado, filas.isEmpty() ? "No hay estudiantes inscritos en esta sección."
                    : "Elige Presente o Ausente para cada estudiante y guarda. 'Sin registrar' no cambia datos.", false);
        } catch (SQLException e) {
            Dialogos.estado(estado, "No se pudo leer la lista de asistencia.", true);
        }
    }

    private void guardar() {
        OpcionSeccion seleccion = secciones.getValue();
        LocalDate dia = fecha.getValue();
        if (seleccion == null || dia == null) {
            Dialogos.error("Selecciona una sección y una fecha.");
            return;
        }
        try {
            List<FilaAsistencia> filas = new ArrayList<>();
            for (FilaUI f : tabla.getItems()) filas.add(f.guardar(dia));
            asistenciaDAO.guardarPorSeccion(seleccion.seccion().getId(), idProfesor, dia, filas);
            Dialogos.estado(estado, "Asistencia guardada para " + dia + ".", false);
            recargar();
        } catch (SQLException e) {
            Dialogos.error("No se pudo guardar la asistencia: " + e.getMessage());
        }
    }
}
