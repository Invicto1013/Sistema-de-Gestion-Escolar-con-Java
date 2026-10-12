package com.unev.sige.controller;

import com.unev.sige.data.CalificacionDAO;
import com.unev.sige.data.PermisoDenegadoException;
import com.unev.sige.data.SeccionDAO;
import com.unev.sige.model.Calificacion;
import com.unev.sige.model.EscalaCalificaciones;
import com.unev.sige.model.NotaEstudiante;
import com.unev.sige.model.Seccion;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Pantalla para capturar o corregir notas de una sección.
 * Los profesores ven sus secciones; el administrador puede elegir cualquiera.
 * La nota final y la letra se calculan solas con EscalaCalificaciones.
 */
public class CalificacionesPanel extends VBox {

    /** Opcion del combo de secciones del profesor. */
    private record OpcionSeccion(Seccion seccion) {
        @Override
        public String toString() {
            return seccion.getAsignatura().getCodigo() + " - " + seccion.getAsignatura().getNombre()
                    + " | " + seccion.getHorario() + " | Aula " + seccion.getAula()
                    + " | " + seccion.getInscritos() + " inscrito(s)";
        }
    }

    private final CalificacionDAO calificacionDAO = new CalificacionDAO();
    private final int idUsuario;
    private final boolean modoAdministrador;

    private final ComboBox<OpcionSeccion> cmbSeccion = new ComboBox<>();
    private final TableView<NotaEstudiante> tabla = new TableView<>();
    private final Label lblEstado = new Label();

    public CalificacionesPanel(int idProfesor) {
        this(idProfesor, false);
    }

    public CalificacionesPanel(int idUsuario, boolean modoAdministrador) {
        this.idUsuario = idUsuario;
        this.modoAdministrador = modoAdministrador;
        setSpacing(12);

        construirTabla();

        cmbSeccion.setPromptText("Selecciona una sección");
        cmbSeccion.setPrefWidth(520);
        cmbSeccion.setOnAction(e -> recargar());

        HBox selector = new HBox(10, new Label("Sección:"), cmbSeccion);
        selector.setAlignment(Pos.CENTER_LEFT);

        Button btnEditar = new Button(modoAdministrador ? "Corregir notas" : "Editar notas");
        btnEditar.setOnAction(e -> onEditar());
        btnEditar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());

        Label lblFormula = new Label(EscalaCalificaciones.descripcionFormula());
        lblFormula.setWrapText(true);
        lblFormula.getStyleClass().add("muted-text");

        VBox.setVgrow(tabla, Priority.ALWAYS);
        getChildren().addAll(Dialogos.titulo(modoAdministrador ? "Administración de calificaciones" : "Calificaciones"),
                selector, btnEditar, tabla, lblEstado, lblFormula);

        cargarSecciones();
    }

    private void construirTabla() {
        TableColumn<NotaEstudiante, String> colMatricula = new TableColumn<>("Matrícula");
        colMatricula.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().estudiante().getMatricula()));
        colMatricula.setMaxWidth(110);

        TableColumn<NotaEstudiante, String> colNombre = new TableColumn<>("Estudiante");
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().estudiante().getApellido() + ", " + c.getValue().estudiante().getNombre()));

        TableColumn<NotaEstudiante, String> colPeriodo = new TableColumn<>("Periodo");
        colPeriodo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().periodo()));
        colPeriodo.setMaxWidth(80);

        TableColumn<NotaEstudiante, String> colP1 = new TableColumn<>("Parcial 1");
        colP1.setCellValueFactory(c -> new SimpleStringProperty(
                Dialogos.nota(c.getValue().calificacion().getNotaParcial1())));
        colP1.setMaxWidth(80);

        TableColumn<NotaEstudiante, String> colP2 = new TableColumn<>("Parcial 2");
        colP2.setCellValueFactory(c -> new SimpleStringProperty(
                Dialogos.nota(c.getValue().calificacion().getNotaParcial2())));
        colP2.setMaxWidth(80);

        TableColumn<NotaEstudiante, String> colExamen = new TableColumn<>("Examen final");
        colExamen.setCellValueFactory(c -> new SimpleStringProperty(
                Dialogos.nota(c.getValue().calificacion().getNotaExamenFinal())));
        colExamen.setMaxWidth(100);

        TableColumn<NotaEstudiante, String> colFinal = new TableColumn<>("Nota final");
        colFinal.setCellValueFactory(c -> new SimpleStringProperty(
                Dialogos.nota(c.getValue().calificacion().getNotaFinal())));
        colFinal.setMaxWidth(90);

        TableColumn<NotaEstudiante, String> colLiteral = new TableColumn<>("Letra");
        colLiteral.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().calificacion().getCalificacionLiteral() == null
                        ? "" : c.getValue().calificacion().getCalificacionLiteral()));
        colLiteral.setMaxWidth(60);

        TableColumn<NotaEstudiante, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().estadoMateria()));
        colEstado.setMaxWidth(90);

        tabla.getColumns().addAll(colMatricula, colNombre, colPeriodo, colP1, colP2, colExamen, colFinal,
                colLiteral, colEstado);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Doble clic sobre un estudiante abre la edicion de sus notas.
        tabla.setRowFactory(tv -> {
            TableRow<NotaEstudiante> fila = new TableRow<>();
            fila.setOnMouseClicked(evento -> {
                if (evento.getClickCount() == 2 && !fila.isEmpty()) {
                    onEditar();
                }
            });
            return fila;
        });
    }

    private void cargarSecciones() {
        try {
            List<Seccion> secciones = modoAdministrador ? new SeccionDAO().listarTodas()
                    : new SeccionDAO().listarPorProfesor(idUsuario);
            for (Seccion s : secciones) {
                cmbSeccion.getItems().add(new OpcionSeccion(s));
            }

            if (secciones.isEmpty()) {
                Dialogos.estado(lblEstado, modoAdministrador ? "No hay secciones registradas."
                        : "Aún no tienes secciones asignadas.", false);
            } else if (secciones.size() == 1) {
                cmbSeccion.setValue(cmbSeccion.getItems().get(0));
                recargar();
            } else {
                Dialogos.estado(lblEstado, "Selecciona una sección para ver a sus estudiantes.", false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Dialogos.estado(lblEstado, "No se pudieron leer las secciones. Revisa la conexión con MySQL.", true);
        }
    }

    private void recargar() {
        OpcionSeccion elegida = cmbSeccion.getValue();
        if (elegida == null) {
            tabla.getItems().clear();
            return;
        }

        try {
            List<NotaEstudiante> notas = calificacionDAO.listarPorSeccion(elegida.seccion().getId());
            tabla.getItems().setAll(notas);
            Dialogos.estado(lblEstado, notas.isEmpty()
                    ? "Esta sección aún no tiene estudiantes inscritos."
                    : "Total: " + notas.size() + " estudiante(s). Doble clic para editar sus notas.", false);
        } catch (SQLException e) {
            e.printStackTrace();
            tabla.getItems().clear();
            Dialogos.estado(lblEstado, "No se pudo leer la lista. Revisa la conexión con MySQL.", true);
        }
    }

    // ----- Acciones -----

    private void onEditar() {
        NotaEstudiante fila = tabla.getSelectionModel().getSelectedItem();
        if (fila == null) {
            return;
        }

        Optional<double[]> notas = pedirNotas(fila);
        if (notas.isEmpty()) {
            return;
        }

        double[] n = notas.get();
        // La nota final y la letra las recalcula el DAO; aqui van en 0 / vacio.
        Calificacion nueva = new Calificacion(fila.calificacion().getId(), fila.idDetalleInscripcion(),
                n[0], n[1], n[2], 0, "");
        try {
            if (modoAdministrador) calificacionDAO.guardarComoAdministrador(nueva, idUsuario);
            else calificacionDAO.guardar(nueva, idUsuario);
        } catch (PermisoDenegadoException e) {
            Dialogos.error(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            Dialogos.error("No se pudieron guardar las notas. Revisa la conexión con MySQL.");
        }
        recargar();
    }

    // ----- Formulario -----

    /**
     * Pide parcial 1, parcial 2 y examen final. Un campo vacio cuenta como 0 (aun sin nota).
     * Muestra en vivo la nota final y la letra. Devuelve {parcial1, parcial2, examenFinal}.
     */
    private Optional<double[]> pedirNotas(NotaEstudiante fila) {
        Calificacion actual = fila.calificacion();

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Editar notas");
        dialogo.setHeaderText(fila.estudiante().getNombreCompleto() + " (" + fila.estudiante().getMatricula() + ")");
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField txtP1 = new TextField(Dialogos.nota(actual.getNotaParcial1()));
        TextField txtP2 = new TextField(Dialogos.nota(actual.getNotaParcial2()));
        TextField txtExamen = new TextField(Dialogos.nota(actual.getNotaExamenFinal()));

        Label lblVista = new Label();
        lblVista.getStyleClass().add("strong-text");

        Label lblError = new Label();
        lblError.getStyleClass().add("error-message");
        lblError.setWrapText(true);

        Label lblFormula = new Label(EscalaCalificaciones.descripcionFormula());
        lblFormula.setWrapText(true);
        lblFormula.getStyleClass().add("muted-text");
        lblFormula.setMaxWidth(380);

        GridPane formulario = new GridPane();
        formulario.setHgap(10);
        formulario.setVgap(10);
        formulario.setPadding(new Insets(16));
        formulario.addRow(0, new Label("Parcial 1 (0-100):"), txtP1);
        formulario.addRow(1, new Label("Parcial 2 (0-100):"), txtP2);
        formulario.addRow(2, new Label("Examen final (0-100):"), txtExamen);
        formulario.add(lblVista, 0, 3, 2, 1);
        formulario.add(lblFormula, 0, 4, 2, 1);
        formulario.add(lblError, 0, 5, 2, 1);
        dialogo.getDialogPane().setContent(formulario);

        Runnable actualizarVista = () -> {
            double[] n = leerNotas(txtP1.getText(), txtP2.getText(), txtExamen.getText());
            if (n == null) {
                lblVista.setText("Nota final: -");
            } else {
                double notaFinal = EscalaCalificaciones.calcularNotaFinal(n[0], n[1], n[2]);
                lblVista.setText("Nota final: " + Dialogos.nota(notaFinal) + "  (" + EscalaCalificaciones.literal(notaFinal)
                        + ")");
            }
        };
        txtP1.textProperty().addListener((o, antes, ahora) -> actualizarVista.run());
        txtP2.textProperty().addListener((o, antes, ahora) -> actualizarVista.run());
        txtExamen.textProperty().addListener((o, antes, ahora) -> actualizarVista.run());
        actualizarVista.run();

        Button botonAceptar = (Button) dialogo.getDialogPane().lookupButton(ButtonType.OK);
        botonAceptar.addEventFilter(ActionEvent.ACTION, evento -> {
            String error = errorNota("Parcial 1", txtP1.getText());
            if (error == null) {
                error = errorNota("Parcial 2", txtP2.getText());
            }
            if (error == null) {
                error = errorNota("Examen final", txtExamen.getText());
            }
            if (error != null) {
                lblError.setText(error);
                evento.consume(); // mantiene el dialogo abierto
            }
        });

        Dialogos.aplicarEstilos(dialogo);
        Optional<ButtonType> respuesta = dialogo.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            return Optional.ofNullable(leerNotas(txtP1.getText(), txtP2.getText(), txtExamen.getText()));
        }
        return Optional.empty();
    }

    /**
     * Convierte un texto en nota. Vacio es 0. Acepta coma o punto decimal.
     * Devuelve null si no es un numero valido.
     */
    private static Double parsear(String texto) {
        String limpio = texto == null ? "" : texto.trim().replace(',', '.');
        if (limpio.isEmpty()) {
            return 0.0;
        }
        try {
            double valor = Double.parseDouble(limpio);
            return Double.isNaN(valor) || Double.isInfinite(valor) ? null : valor;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Mensaje de error de una nota, o null si es valida (numero entre 0 y 100).
     */
    private static String errorNota(String nombre, String texto) {
        Double valor = parsear(texto);
        if (valor == null) {
            return nombre + " debe ser un número.";
        }
        if (valor < 0 || valor > 100) {
            return nombre + " debe estar entre 0 y 100.";
        }
        return null;
    }

    /**
     * Las tres notas, o null si alguna no es valida.
     */
    private static double[] leerNotas(String p1, String p2, String examen) {
        if (errorNota("", p1) != null || errorNota("", p2) != null || errorNota("", examen) != null) {
            return null;
        }
        return new double[]{parsear(p1), parsear(p2), parsear(examen)};
    }
}
