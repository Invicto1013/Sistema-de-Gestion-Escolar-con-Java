package com.unev.sige.controller;

import com.unev.sige.app.SesionActual;
import com.unev.sige.data.PermisoDenegadoException;
import com.unev.sige.data.ReporteDAO;
import com.unev.sige.model.LineaReporteInscripcion;
import com.unev.sige.model.Usuario;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Reporte de inscripciones por intervalo, limitado al alcance académico del usuario actual. */
public class ReporteInscripcionesPanel extends VBox {
    private final Usuario usuario = SesionActual.getUsuario();
    private final DatePicker desde = new DatePicker(LocalDate.now().withDayOfMonth(1));
    private final DatePicker hasta = new DatePicker(LocalDate.now());
    private final TableView<LineaReporteInscripcion> tabla = new TableView<>();
    private final Label estado = new Label();
    private final Button exportar = new Button("Imprimir / Guardar como PDF");

    public ReporteInscripcionesPanel() {
        setSpacing(12);
        construirTabla();
        desde.setPromptText("Desde");
        hasta.setPromptText("Hasta");
        Button consultar = new Button("Consultar");
        consultar.setOnAction(e -> consultar());
        exportar.setDisable(true);
        exportar.setOnAction(e -> exportar());
        HBox filtros = new HBox(8, new Label("Desde:"), desde, new Label("Hasta:"), hasta,
                consultar, exportar);
        filtros.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        getChildren().addAll(Dialogos.titulo("Reporte de inscripciones"), filtros, tabla, estado);
        consultar();
    }

    private void construirTabla() {
        TableColumn<LineaReporteInscripcion, String> fecha = columna("Fecha", f -> f.fecha().toString());
        TableColumn<LineaReporteInscripcion, String> estudiante = columna("Estudiante", LineaReporteInscripcion::estudiante);
        TableColumn<LineaReporteInscripcion, String> matricula = columna("Matrícula", LineaReporteInscripcion::matricula);
        TableColumn<LineaReporteInscripcion, String> periodo = columna("Período", LineaReporteInscripcion::periodo);
        TableColumn<LineaReporteInscripcion, String> asignatura = columna("Asignatura", LineaReporteInscripcion::asignatura);
        TableColumn<LineaReporteInscripcion, String> grado = columna("Grado", f -> f.grado() == null ? "Pendiente" : f.grado());
        TableColumn<LineaReporteInscripcion, String> area = columna("Área", f -> f.area() == null ? "Pendiente" : f.area());
        TableColumn<LineaReporteInscripcion, String> seccion = columna("Sección", f -> Integer.toString(f.idSeccion()));
        TableColumn<LineaReporteInscripcion, String> estadoMateria = columna("Estado", LineaReporteInscripcion::estado);
        tabla.getColumns().addAll(fecha, estudiante, matricula, periodo, asignatura, grado, area, seccion, estadoMateria);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private TableColumn<LineaReporteInscripcion, String> columna(String titulo,
            java.util.function.Function<LineaReporteInscripcion, String> obtener) {
        TableColumn<LineaReporteInscripcion, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new SimpleStringProperty(obtener.apply(c.getValue())));
        return columna;
    }

    private void consultar() {
        if (usuario == null) {
            Dialogos.estado(estado, "No hay una sesión iniciada.", true);
            return;
        }
        if (desde.getValue() == null || hasta.getValue() == null || desde.getValue().isAfter(hasta.getValue())) {
            Dialogos.estado(estado, "Selecciona un rango válido: la fecha inicial debe ser anterior a la final.", true);
            exportar.setDisable(true);
            tabla.getItems().clear();
            return;
        }
        try {
            List<LineaReporteInscripcion> filas = new ReporteDAO().listarInscripciones(
                    desde.getValue(), hasta.getValue(), usuario.getRol(), usuario.getId());
            tabla.getItems().setAll(filas);
            exportar.setDisable(filas.isEmpty());
            Dialogos.estado(estado, filas.size() + " inscripción(es) en el rango seleccionado.", false);
        } catch (PermisoDenegadoException e) {
            tabla.getItems().clear();
            exportar.setDisable(true);
            Dialogos.estado(estado, e.getMessage(), true);
        } catch (SQLException e) {
            e.printStackTrace();
            tabla.getItems().clear();
            exportar.setDisable(true);
            Dialogos.estado(estado, "No se pudo consultar el reporte. Revisa la conexión con MySQL.", true);
        }
    }

    private void exportar() {
        if (tabla.getItems().isEmpty()) return;
        PrinterJob trabajo = PrinterJob.createPrinterJob();
        if (trabajo == null) {
            Dialogos.error("No se encontró una impresora disponible para exportar el reporte.");
            return;
        }
        if (!trabajo.showPrintDialog(getScene() == null ? null : getScene().getWindow())) return;
        var layout = trabajo.getPrinter().createPageLayout(Paper.A4, PageOrientation.LANDSCAPE,
                javafx.print.Printer.MarginType.DEFAULT);
        boolean impreso = trabajo.printPage(layout, crearDocumento());
        if (impreso && trabajo.endJob()) {
            Dialogos.estado(estado, "Reporte enviado a la impresora seleccionada.", false);
        } else {
            trabajo.cancelJob();
            Dialogos.error("No se pudo generar el reporte.");
        }
    }

    private Node crearDocumento() {
        VBox pagina = new VBox(8);
        pagina.getStylesheets().add(ReporteInscripcionesPanel.class
                .getResource("/com/unev/sige/view/styles.css").toExternalForm());
        pagina.getStyleClass().add("report-paper");
        pagina.setPadding(new Insets(18));
        pagina.setPrefWidth(760);
        pagina.setMinWidth(760);
        pagina.setMaxWidth(760);
        Label marca = new Label("SIGE · SISTEMA DE GESTIÓN EDUCATIVA");
        marca.getStyleClass().add("strong-text");
        Label titulo = new Label("REPORTE DE INSCRIPCIONES");
        titulo.getStyleClass().add("report-title");
        Label metadatos = new Label("Generado: " + LocalDate.now() + "    Usuario: "
                + usuario.getNombreCompleto() + "    Rol: " + usuario.getRol()
                + "    Rango: " + desde.getValue() + " — " + hasta.getValue());
        metadatos.getStyleClass().add("report-meta");
        pagina.getChildren().addAll(marca, titulo, metadatos);

        GridPane filas = new GridPane();
        filas.setPrefWidth(724);
        double[] anchos = {10, 17, 10, 9, 20, 10, 10, 6, 8};
        for (double ancho : anchos) {
            javafx.scene.layout.ColumnConstraints c = new javafx.scene.layout.ColumnConstraints();
            c.setPercentWidth(ancho);
            c.setMinWidth(0);
            filas.getColumnConstraints().add(c);
        }
        String[] titulos = {"Fecha", "Estudiante", "Matrícula", "Período", "Asignatura", "Grado", "Área", "Sección", "Estado"};
        for (int i = 0; i < titulos.length; i++) filas.add(celda(titulos[i], true), i, 0);
        int fila = 1;
        for (LineaReporteInscripcion registro : tabla.getItems()) {
            String[] valores = {registro.fecha().toString(), registro.estudiante(), registro.matricula(),
                    registro.periodo(), registro.asignatura(), registro.grado() == null ? "Pendiente" : registro.grado(),
                    registro.area() == null ? "Pendiente" : registro.area(), Integer.toString(registro.idSeccion()), registro.estado()};
            for (int columna = 0; columna < valores.length; columna++) {
                filas.add(celda(valores[columna], false), columna, fila);
            }
            fila++;
        }
        pagina.getChildren().add(filas);
        return pagina;
    }

    private Label celda(String texto, boolean encabezado) {
        Label etiqueta = new Label(texto == null ? "" : texto);
        etiqueta.setWrapText(true);
        etiqueta.setMaxWidth(Double.MAX_VALUE);
        etiqueta.getStyleClass().add("report-table-cell");
        if (encabezado) etiqueta.getStyleClass().add("report-table-header");
        return etiqueta;
    }
}
