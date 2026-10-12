package com.unev.sige.controller;

import com.unev.sige.data.InscripcionDAO;
import com.unev.sige.model.Inscripcion;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

/**
 * Pantalla del estudiante: las materias en las que esta inscrito (solo lectura).
 */
public class MisMateriasPanel extends VBox {

    public MisMateriasPanel(int idEstudiante) {
        setSpacing(12);

        TableView<FilaMateria> tabla = FilaMateria.crearTabla();
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Label lblEstado = new Label();

        try {
            List<Inscripcion> inscripciones = new InscripcionDAO().listarPorEstudiante(idEstudiante);
            List<FilaMateria> filas = FilaMateria.aplanar(inscripciones);
            tabla.getItems().setAll(filas);

            if (filas.isEmpty()) {
                Dialogos.estado(lblEstado, "Aún no estás inscrito en ninguna materia.", false);
            } else {
                int creditos = filas.stream()
                        .mapToInt(f -> f.detalle().getSeccion().getAsignatura().getCreditos())
                        .sum();
                Dialogos.estado(lblEstado, "Total: " + filas.size() + " materia(s), " + creditos
                        + " crédito(s)", false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Dialogos.estado(lblEstado, "No se pudieron leer tus materias. Revisa la conexión con MySQL.", true);
        }

        getChildren().addAll(Dialogos.titulo("Mis materias"), tabla, lblEstado);
    }
}
