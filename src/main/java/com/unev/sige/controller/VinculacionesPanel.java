package com.unev.sige.controller;

import com.unev.sige.data.VinculacionDAO;
import com.unev.sige.model.Estudiante;
import com.unev.sige.model.Padre;
import com.unev.sige.model.VinculoPadreEstudiante;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;

/** Administración de vínculos padre/tutor-estudiante. */
public class VinculacionesPanel extends VBox {
    private record OpcionPadre(Padre padre) {
        @Override public String toString() { return padre.getNombreCompleto() + " (" + padre.getCorreo() + ")"; }
    }
    private record OpcionEstudiante(Estudiante estudiante) {
        @Override public String toString() {
            return estudiante.getNombreCompleto() + " (" + estudiante.getMatricula() + ")";
        }
    }

    private final VinculacionDAO dao = new VinculacionDAO();
    private final ComboBox<OpcionPadre> padres = new ComboBox<>();
    private final ComboBox<OpcionEstudiante> estudiantes = new ComboBox<>();
    private final TextField parentesco = new TextField("PADRE/MADRE/TUTOR");
    private final TableView<VinculoPadreEstudiante> tabla = new TableView<>();
    private final Label estado = new Label();

    public VinculacionesPanel() {
        setSpacing(12);
        padres.setPromptText("Selecciona un padre o tutor");
        estudiantes.setPromptText("Selecciona un estudiante");
        padres.setPrefWidth(320);
        estudiantes.setPrefWidth(320);
        parentesco.setPromptText("Parentesco");
        parentesco.setPrefWidth(190);
        HBox controles = new HBox(8, padres, estudiantes, parentesco);
        controles.setAlignment(Pos.CENTER_LEFT);

        Button vincular = new Button("Vincular");
        vincular.setOnAction(e -> vincular());
        Button quitar = new Button("Quitar vínculo");
        quitar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        quitar.setOnAction(e -> quitar());
        HBox acciones = new HBox(8, vincular, quitar);

        TableColumn<VinculoPadreEstudiante, String> padre = col("Padre / tutor", VinculoPadreEstudiante::padre);
        TableColumn<VinculoPadreEstudiante, String> hijo = col("Estudiante", VinculoPadreEstudiante::estudiante);
        TableColumn<VinculoPadreEstudiante, String> matricula = col("Matrícula", VinculoPadreEstudiante::matricula);
        TableColumn<VinculoPadreEstudiante, String> parentescoCol = col("Parentesco", VinculoPadreEstudiante::parentesco);
        tabla.getColumns().addAll(padre, hijo, matricula, parentescoCol);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        getChildren().addAll(Dialogos.titulo("Vinculación de padres e hijos"),
                new Label("Selecciona las cuentas existentes para asociarlas."), controles, acciones, tabla, estado);
        cargarOpciones();
        recargar();
    }

    private TableColumn<VinculoPadreEstudiante, String> col(String titulo,
            java.util.function.Function<VinculoPadreEstudiante, String> valor) {
        TableColumn<VinculoPadreEstudiante, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new SimpleStringProperty(valor.apply(c.getValue())));
        return columna;
    }

    private void cargarOpciones() {
        try {
            dao.listarPadres().forEach(p -> padres.getItems().add(new OpcionPadre(p)));
            dao.listarEstudiantes().forEach(e -> estudiantes.getItems().add(new OpcionEstudiante(e)));
            if (padres.getItems().isEmpty()) Dialogos.estado(estado,
                    "No hay cuentas de padre/tutor registradas. Registra primero una cuenta con rol PADRE.", true);
        } catch (SQLException e) {
            Dialogos.estado(estado, "No se pudieron cargar padres y estudiantes. Revisa la base de datos.", true);
        }
    }

    private void recargar() {
        try {
            tabla.getItems().setAll(dao.listarVinculos());
            if (!tabla.getItems().isEmpty()) Dialogos.estado(estado,
                    "Vínculos registrados: " + tabla.getItems().size() + ".", false);
        } catch (SQLException e) {
            Dialogos.estado(estado, "No se pudieron leer los vínculos. Verifica que estudiante_padre exista.", true);
        }
    }

    private void vincular() {
        if (padres.getValue() == null || estudiantes.getValue() == null) {
            Dialogos.error("Selecciona un padre/tutor y un estudiante.");
            return;
        }
        try {
            boolean nuevo = dao.vincular(padres.getValue().padre().getIdPadre(),
                    estudiantes.getValue().estudiante().getId(), parentesco.getText());
            Dialogos.estado(estado, nuevo ? "Vínculo creado." : "Ese padre/tutor ya está vinculado con el estudiante.", !nuevo);
            recargar();
        } catch (SQLException e) {
            Dialogos.error("No se pudo crear el vínculo. Comprueba que ejecutaste sql/vinculacion_padres.sql.\n" + e.getMessage());
        }
    }

    private void quitar() {
        VinculoPadreEstudiante seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;
        if (!Dialogos.confirmar("Quitar vínculo", "¿Quitar a " + seleccionado.padre()
                + " de los contactos de " + seleccionado.estudiante() + "?")) return;
        try {
            dao.desvincular(seleccionado.idPadre(), seleccionado.idEstudiante());
            Dialogos.estado(estado, "Vínculo eliminado.", false);
            recargar();
        } catch (SQLException e) {
            Dialogos.error("No se pudo quitar el vínculo. Revisa la conexión con MySQL.");
        }
    }
}
