package com.unev.sige.controller;

import com.unev.sige.data.CalificacionDAO;
import com.unev.sige.data.VinculacionDAO;
import com.unev.sige.model.CalificacionHijo;
import com.unev.sige.model.Estudiante;
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

/** Consulta de calificaciones de hijos vinculados al padre autenticado. */
public class CalificacionesHijosPanel extends VBox {
    private final int idPadre;
    private final ComboBox<Estudiante> estudiantes = new ComboBox<>();
    private final TableView<CalificacionHijo> tabla = new TableView<>();
    private final Label estado = new Label();
    private final Label promedio = new Label();

    public CalificacionesHijosPanel(int idPadre) {
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
        TableColumn<CalificacionHijo, String> periodo = col("Periodo", r -> r.periodo());
        periodo.setMaxWidth(90);
        TableColumn<CalificacionHijo, String> materia = col("Asignatura", r -> r.codigoAsignatura() + " - " + r.asignatura());
        TableColumn<CalificacionHijo, String> p1 = col("Parcial 1", r -> Dialogos.nota(r.parcial1()));
        TableColumn<CalificacionHijo, String> p2 = col("Parcial 2", r -> Dialogos.nota(r.parcial2()));
        TableColumn<CalificacionHijo, String> examen = col("Examen final", r -> Dialogos.nota(r.examenFinal()));
        TableColumn<CalificacionHijo, String> notaFinal = col("Nota final", r -> Dialogos.nota(r.notaFinal()));
        TableColumn<CalificacionHijo, String> letra = col("Letra", r -> r.literal() == null ? "-" : r.literal());
        letra.setMaxWidth(65);
        TableColumn<CalificacionHijo, String> estadoMateria = col("Estado", CalificacionHijo::estado);
        estadoMateria.setMaxWidth(100);
        tabla.getColumns().addAll(periodo, materia, p1, p2, examen, notaFinal, letra, estadoMateria);
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
        getChildren().addAll(Dialogos.titulo("Calificaciones de mis hijos"),
                new HBox(10, new Label("Estudiante:"), estudiantes), tabla, promedio, estado);
    }

    private TableColumn<CalificacionHijo, String> col(String titulo,
            java.util.function.Function<CalificacionHijo, String> valor) {
        TableColumn<CalificacionHijo, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new SimpleStringProperty(valor.apply(c.getValue())));
        return columna;
    }

    private void recargar() {
        Estudiante elegido = estudiantes.getValue();
        if (elegido == null) { tabla.getItems().clear(); promedio.setText(""); return; }
        try {
            List<CalificacionHijo> filas = new CalificacionDAO().listarParaPadre(idPadre, elegido.getId());
            tabla.getItems().setAll(filas);
            List<CalificacionHijo> evaluadas = filas.stream()
                    .filter(f -> "Aprobada".equals(f.estado()) || "Reprobada".equals(f.estado())).toList();
            promedio.setText(evaluadas.isEmpty() ? "Promedio de materias evaluadas: sin datos"
                    : String.format("Promedio de materias evaluadas: %.2f",
                    evaluadas.stream().mapToDouble(CalificacionHijo::notaFinal).average().orElse(0)));
            Dialogos.estado(estado, filas.isEmpty() ? "El estudiante aún no tiene materias inscritas."
                    : "Calificaciones de " + elegido.getNombreCompleto() + ".", false);
        } catch (SQLException e) {
            Dialogos.estado(estado, "No se pudieron consultar las calificaciones.", true);
        }
    }
}
