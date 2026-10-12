package com.unev.sige.controller;

import com.unev.sige.data.AsistenciaDAO;
import com.unev.sige.data.VinculacionDAO;
import com.unev.sige.model.Estudiante;
import com.unev.sige.model.ResumenAsistencia;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

/** Consulta de asistencia de hijos vinculados al padre autenticado. */
public class AsistenciaHijosPanel extends VBox {
    private final int idPadre;
    private final ComboBox<Estudiante> estudiantes = new ComboBox<>();
    private final TableView<ResumenAsistencia> tabla = new TableView<>();
    private final Label estado = new Label();

    public AsistenciaHijosPanel(int idPadre) {
        this.idPadre = idPadre;
        setSpacing(12);
        estudiantes.setPromptText("Selecciona un hijo o hija");
        estudiantes.setPrefWidth(420);
        estudiantes.setCellFactory(c -> new ListCell<>() {
            @Override protected void updateItem(Estudiante item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNombreCompleto() + " (" + item.getMatricula() + ")");
            }
        });
        estudiantes.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Estudiante item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNombreCompleto() + " (" + item.getMatricula() + ")");
            }
        });
        estudiantes.setOnAction(e -> recargar());
        TableColumn<ResumenAsistencia, String> periodo = col("Periodo", r -> r.periodo());
        periodo.setMaxWidth(90);
        TableColumn<ResumenAsistencia, String> materia = col("Materia", r -> r.codigoAsignatura() + " - " + r.asignatura());
        TableColumn<ResumenAsistencia, String> registradas = col("Clases registradas", r -> Integer.toString(r.clasesRegistradas()));
        TableColumn<ResumenAsistencia, String> presentes = col("Presentes", r -> Integer.toString(r.presentes()));
        TableColumn<ResumenAsistencia, String> ausentes = col("Ausentes", r -> Integer.toString(r.ausentes()));
        TableColumn<ResumenAsistencia, String> porcentaje = col("Asistencia", r -> r.clasesRegistradas() == 0
                ? "Sin datos" : String.format("%.1f%%", r.porcentaje()));
        tabla.getColumns().addAll(periodo, materia, registradas, presentes, ausentes, porcentaje);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        try {
            List<Estudiante> hijos = new VinculacionDAO().listarHijos(idPadre);
            estudiantes.getItems().setAll(hijos);
            if (hijos.isEmpty()) Dialogos.estado(estado, "No tienes estudiantes vinculados a tu cuenta.", false);
            else {
                estudiantes.setValue(hijos.get(0));
                recargar();
            }
        } catch (SQLException e) {
            Dialogos.estado(estado, "No se pudieron cargar los estudiantes vinculados.", true);
        }
        getChildren().addAll(Dialogos.titulo("Asistencia de mis hijos"),
                new HBox(10, new Label("Estudiante:"), estudiantes), tabla, estado);
    }

    private TableColumn<ResumenAsistencia, String> col(String titulo,
            java.util.function.Function<ResumenAsistencia, String> valor) {
        TableColumn<ResumenAsistencia, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new SimpleStringProperty(valor.apply(c.getValue())));
        return columna;
    }

    private void recargar() {
        Estudiante elegido = estudiantes.getValue();
        if (elegido == null) { tabla.getItems().clear(); return; }
        try {
            List<ResumenAsistencia> filas = new AsistenciaDAO().listarResumenParaPadre(idPadre, elegido.getId());
            tabla.getItems().setAll(filas);
            Dialogos.estado(estado, filas.isEmpty() ? "El estudiante aún no tiene materias inscritas."
                    : "El porcentaje usa las clases que tienen asistencia registrada.", false);
        } catch (SQLException e) {
            Dialogos.estado(estado, "No se pudo consultar la asistencia.", true);
        }
    }
}
