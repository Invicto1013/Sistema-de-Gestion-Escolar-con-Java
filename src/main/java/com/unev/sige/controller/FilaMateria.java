package com.unev.sige.controller;

import com.unev.sige.model.DetalleInscripcion;
import com.unev.sige.model.Inscripcion;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.List;

/**
 * Una materia inscrita junto con la inscripcion (periodo) a la que pertenece.
 * La usan las tablas de "Inscripciones" (administrador) y "Mis materias" (estudiante).
 */
record FilaMateria(Inscripcion inscripcion, DetalleInscripcion detalle) {

    /**
     * Convierte las inscripciones (una por periodo) en una fila por materia.
     */
    static List<FilaMateria> aplanar(List<Inscripcion> inscripciones) {
        List<FilaMateria> filas = new ArrayList<>();
        for (Inscripcion inscripcion : inscripciones) {
            for (DetalleInscripcion detalle : inscripcion.getDetalles()) {
                filas.add(new FilaMateria(inscripcion, detalle));
            }
        }
        return filas;
    }

    /**
     * Crea la tabla con las columnas comunes: periodo, asignatura, creditos, profesor, aula, horario y estado.
     */
    static TableView<FilaMateria> crearTabla() {
        TableView<FilaMateria> tabla = new TableView<>();

        TableColumn<FilaMateria, String> colPeriodo = new TableColumn<>("Periodo");
        colPeriodo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().inscripcion().getPeriodoLectivo()));
        colPeriodo.setMaxWidth(90);

        TableColumn<FilaMateria, String> colAsignatura = new TableColumn<>("Asignatura");
        colAsignatura.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().detalle().getSeccion().getAsignatura().getCodigo() + " - "
                        + c.getValue().detalle().getSeccion().getAsignatura().getNombre()));

        TableColumn<FilaMateria, Number> colCreditos = new TableColumn<>("Créditos");
        colCreditos.setCellValueFactory(c -> new SimpleIntegerProperty(
                c.getValue().detalle().getSeccion().getAsignatura().getCreditos()));
        colCreditos.setMaxWidth(80);

        TableColumn<FilaMateria, String> colProfesor = new TableColumn<>("Profesor");
        colProfesor.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().detalle().getSeccion().getProfesor() == null
                        ? "Sin asignar"
                        : c.getValue().detalle().getSeccion().getProfesor().getNombreCompleto()));

        TableColumn<FilaMateria, String> colAula = new TableColumn<>("Aula");
        colAula.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().detalle().getSeccion().getAula()));
        colAula.setMaxWidth(80);

        TableColumn<FilaMateria, String> colHorario = new TableColumn<>("Horario");
        colHorario.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().detalle().getSeccion().getHorario()));

        TableColumn<FilaMateria, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().detalle().getEstadoMateria()));
        colEstado.setMaxWidth(90);

        tabla.getColumns().addAll(colPeriodo, colAsignatura, colCreditos, colProfesor, colAula, colHorario,
                colEstado);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return tabla;
    }
}
