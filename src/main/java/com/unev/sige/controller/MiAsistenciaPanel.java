package com.unev.sige.controller;

import com.unev.sige.data.AsistenciaDAO;
import com.unev.sige.model.ResumenAsistencia;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

/** Consulta de asistencia del estudiante, agrupada por materia inscrita. */
public class MiAsistenciaPanel extends VBox {
    public MiAsistenciaPanel(int idEstudiante) {
        setSpacing(12);
        TableView<ResumenAsistencia> tabla = new TableView<>();
        TableColumn<ResumenAsistencia, String> periodo = new TableColumn<>("Periodo");
        periodo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().periodo()));
        periodo.setMaxWidth(90);
        TableColumn<ResumenAsistencia, String> materia = new TableColumn<>("Materia");
        materia.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().codigoAsignatura() + " - " + c.getValue().asignatura()));
        TableColumn<ResumenAsistencia, String> registradas = col("Clases registradas", r -> Integer.toString(r.clasesRegistradas()));
        TableColumn<ResumenAsistencia, String> presentes = col("Presentes", r -> Integer.toString(r.presentes()));
        TableColumn<ResumenAsistencia, String> ausentes = col("Ausentes", r -> Integer.toString(r.ausentes()));
        TableColumn<ResumenAsistencia, String> porcentaje = col("Asistencia",
                r -> r.clasesRegistradas() == 0 ? "Sin datos" : String.format("%.1f%%", r.porcentaje()));
        tabla.getColumns().addAll(periodo, materia, registradas, presentes, ausentes, porcentaje);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        Label estado = new Label();
        try {
            List<ResumenAsistencia> filas = new AsistenciaDAO().listarResumenPorEstudiante(idEstudiante);
            tabla.getItems().setAll(filas);
            Dialogos.estado(estado, filas.isEmpty() ? "Aún no tienes materias inscritas."
                    : "El porcentaje se calcula con las clases que tienen asistencia registrada.", false);
        } catch (SQLException e) {
            Dialogos.estado(estado, "No se pudo leer tu asistencia. Revisa la conexión con MySQL.", true);
        }
        getChildren().addAll(Dialogos.titulo("Mi asistencia"), tabla, estado);
    }

    private TableColumn<ResumenAsistencia, String> col(String titulo, java.util.function.Function<ResumenAsistencia, String> valor) {
        TableColumn<ResumenAsistencia, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new SimpleStringProperty(valor.apply(c.getValue())));
        columna.setMaxWidth(145);
        return columna;
    }
}
