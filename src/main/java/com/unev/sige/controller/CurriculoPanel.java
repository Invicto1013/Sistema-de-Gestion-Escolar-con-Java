package com.unev.sige.controller;

import com.unev.sige.data.CurriculoDAO;
import com.unev.sige.model.GradoCurricular;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;

/** Consulta del catalogo de grados y areas curriculares compartido con SIGE Web. */
public final class CurriculoPanel extends VBox {
    private final TableView<GradoCurricular> tabla = new TableView<>();
    private final Label estado = new Label();

    public CurriculoPanel() {
        setSpacing(12);
        TableColumn<GradoCurricular, String> nivel = new TableColumn<>("Nivel");
        nivel.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().nivel()));
        TableColumn<GradoCurricular, String> ciclo = new TableColumn<>("Ciclo");
        ciclo.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().ciclo()));
        TableColumn<GradoCurricular, String> grado = new TableColumn<>("Grado");
        grado.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().nombre()));
        TableColumn<GradoCurricular, String> areas = new TableColumn<>("Áreas curriculares");
        areas.setCellValueFactory(row -> new SimpleStringProperty(String.join(", ", row.getValue().areas())));
        nivel.setPrefWidth(125);
        ciclo.setPrefWidth(150);
        grado.setPrefWidth(170);
        areas.setPrefWidth(650);
        tabla.getColumns().addAll(nivel, ciclo, grado, areas);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        getChildren().addAll(Dialogos.titulo("Grados y áreas curriculares"),
                new Label("Catálogo base de Primaria y Secundaria. Las horas y ofertas por modalidad se configuran según el plan del centro."), tabla, estado);
        cargar();
    }

    private void cargar() {
        try {
            tabla.getItems().setAll(new CurriculoDAO().listarGrados());
            estado.setText(tabla.getItems().isEmpty()
                    ? "El catálogo está vacío. Ejecuta sql/fase_curriculo_grados.sql en sige_db."
                    : "Grados curriculares cargados: " + tabla.getItems().size());
        } catch (SQLException e) {
            estado.setText("No se pudo cargar el catálogo. Ejecuta sql/fase_curriculo_grados.sql y verifica MySQL.");
        }
    }
}
