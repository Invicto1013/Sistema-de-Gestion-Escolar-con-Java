package com.unev.sige.controller;

import com.unev.sige.data.VinculacionDAO;
import com.unev.sige.model.Estudiante;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

/** Estudiantes vinculados a la cuenta autenticada del padre o tutor. */
public class MisHijosPanel extends VBox {
    public MisHijosPanel(int idPadreUsuario) {
        setSpacing(12);
        TableView<Estudiante> tabla = new TableView<>();
        TableColumn<Estudiante, String> nombre = col("Estudiante", Estudiante::getNombreCompleto);
        TableColumn<Estudiante, String> matricula = col("Matrícula", Estudiante::getMatricula);
        TableColumn<Estudiante, String> grado = col("Grado", Estudiante::getGrado);
        TableColumn<Estudiante, String> promedio = col("Promedio académico",
                e -> String.format("%.2f", e.getPromedioAcademico()));
        tabla.getColumns().addAll(nombre, matricula, grado, promedio);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        Label estado = new Label();
        try {
            List<Estudiante> hijos = new VinculacionDAO().listarHijos(idPadreUsuario);
            tabla.getItems().setAll(hijos);
            Dialogos.estado(estado, hijos.isEmpty()
                    ? "Aún no hay estudiantes vinculados a tu cuenta. Contacta al administrador."
                    : "Estudiantes vinculados: " + hijos.size() + ".", false);
        } catch (SQLException e) {
            Dialogos.estado(estado, "No se pudo leer la lista de estudiantes vinculados.", true);
        }
        getChildren().addAll(Dialogos.titulo("Mis hijos"), tabla, estado);
    }

    private TableColumn<Estudiante, String> col(String titulo,
            java.util.function.Function<Estudiante, String> valor) {
        TableColumn<Estudiante, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new SimpleStringProperty(valor.apply(c.getValue())));
        return columna;
    }
}
