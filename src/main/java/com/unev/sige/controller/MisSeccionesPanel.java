package com.unev.sige.controller;

import com.unev.sige.data.SeccionDAO;
import com.unev.sige.model.Seccion;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

/**
 * Pantalla del profesor: las secciones que tiene asignadas (solo lectura).
 */
public class MisSeccionesPanel extends VBox {

    public MisSeccionesPanel(int idProfesor) {
        setSpacing(12);

        TableView<Seccion> tabla = new TableView<>();

        TableColumn<Seccion, String> colAsignatura = new TableColumn<>("Asignatura");
        colAsignatura.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getAsignatura().getCodigo() + " - " + c.getValue().getAsignatura().getNombre()));

        TableColumn<Seccion, Number> colCreditos = new TableColumn<>("Créditos");
        colCreditos.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getAsignatura().getCreditos()));
        colCreditos.setMaxWidth(90);

        TableColumn<Seccion, String> colAula = new TableColumn<>("Aula");
        colAula.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAula()));
        colAula.setMaxWidth(100);

        TableColumn<Seccion, String> colHorario = new TableColumn<>("Horario");
        colHorario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHorario()));

        TableColumn<Seccion, String> colCupo = new TableColumn<>("Inscritos / Cupo");
        colCupo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getInscritos() + " / " + c.getValue().getCupoMaximo()));
        colCupo.setMaxWidth(120);

        tabla.getColumns().addAll(colAsignatura, colCreditos, colAula, colHorario, colCupo);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Label lblEstado = new Label();

        try {
            List<Seccion> secciones = new SeccionDAO().listarPorProfesor(idProfesor);
            tabla.getItems().setAll(secciones);
            Dialogos.estado(lblEstado, secciones.isEmpty()
                    ? "Aún no tienes secciones asignadas."
                    : "Total: " + secciones.size() + " sección(es)", false);
        } catch (SQLException e) {
            e.printStackTrace();
            Dialogos.estado(lblEstado, "No se pudo leer tus secciones. Revisa la conexión con MySQL.", true);
        }

        getChildren().addAll(Dialogos.titulo("Mis secciones"), tabla, lblEstado);
    }
}
