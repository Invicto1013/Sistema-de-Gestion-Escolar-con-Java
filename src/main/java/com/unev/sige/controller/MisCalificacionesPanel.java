package com.unev.sige.controller;

import com.unev.sige.data.InscripcionDAO;
import com.unev.sige.model.Calificacion;
import com.unev.sige.model.EscalaCalificaciones;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;

/**
 * Pantalla del estudiante: sus notas por materia (solo lectura).
 * Las materias en curso (sin examen final) muestran la nota acumulada hasta el momento.
 */
public class MisCalificacionesPanel extends VBox {

    public MisCalificacionesPanel(int idEstudiante) {
        setSpacing(12);

        TableView<FilaMateria> tabla = new TableView<>();

        TableColumn<FilaMateria, String> colPeriodo = new TableColumn<>("Periodo");
        colPeriodo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().inscripcion().getPeriodoLectivo()));
        colPeriodo.setMaxWidth(90);

        TableColumn<FilaMateria, String> colAsignatura = new TableColumn<>("Asignatura");
        colAsignatura.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().detalle().getSeccion().getAsignatura().getCodigo() + " - "
                        + c.getValue().detalle().getSeccion().getAsignatura().getNombre()));

        tabla.getColumns().addAll(colPeriodo, colAsignatura,
                columnaNota("Parcial 1", 80, Calificacion::getNotaParcial1),
                columnaNota("Parcial 2", 80, Calificacion::getNotaParcial2),
                columnaNota("Examen final", 100, Calificacion::getNotaExamenFinal),
                columnaNota("Nota final", 90, Calificacion::getNotaFinal));

        TableColumn<FilaMateria, String> colLetra = new TableColumn<>("Letra");
        colLetra.setCellValueFactory(c -> {
            Calificacion cal = c.getValue().detalle().getCalificacion();
            return new SimpleStringProperty(cal == null || cal.getCalificacionLiteral() == null
                    ? "-" : cal.getCalificacionLiteral());
        });
        colLetra.setMaxWidth(60);

        TableColumn<FilaMateria, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().detalle().getEstadoMateria()));
        colEstado.setMaxWidth(90);

        tabla.getColumns().addAll(colLetra, colEstado);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Label lblEstado = new Label();

        try {
            List<FilaMateria> filas = FilaMateria.aplanar(new InscripcionDAO().listarPorEstudiante(idEstudiante));
            tabla.getItems().setAll(filas);

            List<FilaMateria> evaluadas = filas.stream()
                    .filter(f -> "Aprobada".equals(f.detalle().getEstadoMateria())
                            || "Reprobada".equals(f.detalle().getEstadoMateria()))
                    .filter(f -> f.detalle().getCalificacion() != null)
                    .toList();

            if (filas.isEmpty()) {
                Dialogos.estado(lblEstado, "Aún no estás inscrito en ninguna materia.", false);
            } else if (evaluadas.isEmpty()) {
                Dialogos.estado(lblEstado, "Aún no tienes materias evaluadas.", false);
            } else {
                double promedio = evaluadas.stream()
                        .mapToDouble(f -> f.detalle().getCalificacion().getNotaFinal())
                        .average().orElse(0);
                long aprobadas = evaluadas.stream()
                        .filter(f -> "Aprobada".equals(f.detalle().getEstadoMateria()))
                        .count();
                Dialogos.estado(lblEstado, String.format("Materias evaluadas: %d (aprobadas: %d). Promedio: %.2f",
                        evaluadas.size(), aprobadas, promedio), false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Dialogos.estado(lblEstado, "No se pudieron leer tus calificaciones. Revisa la conexión con MySQL.", true);
        }

        Label lblFormula = new Label(EscalaCalificaciones.descripcionFormula());
        lblFormula.setWrapText(true);
        lblFormula.setTextFill(Color.GRAY);

        getChildren().addAll(Dialogos.titulo("Mis calificaciones"), tabla, lblEstado, lblFormula);
    }

    /**
     * Columna de una nota; muestra "-" si la materia no tiene calificacion.
     */
    private TableColumn<FilaMateria, String> columnaNota(String titulo, double ancho,
                                                         Function<Calificacion, Double> nota) {
        TableColumn<FilaMateria, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> {
            Calificacion cal = c.getValue().detalle().getCalificacion();
            return new SimpleStringProperty(cal == null ? "-" : Dialogos.nota(nota.apply(cal)));
        });
        columna.setMaxWidth(ancho);
        return columna;
    }
}
