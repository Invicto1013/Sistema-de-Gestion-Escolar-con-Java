package com.unev.sige.controller;

import com.unev.sige.data.BoletaDAO;
import com.unev.sige.data.UsuarioDAO;
import com.unev.sige.data.VinculacionDAO;
import com.unev.sige.model.Estudiante;
import com.unev.sige.model.LineaBoleta;
import com.unev.sige.model.Rol;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.PrinterJob;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Boleta de un estudiante por período, visible solo para el propio estudiante, su padre o admin. */
public class BoletaPanel extends VBox {
    private final Rol rol;
    private final int idUsuario;
    private final ComboBox<Estudiante> estudiante = new ComboBox<>();
    private final ComboBox<String> periodo = new ComboBox<>();
    private final TableView<LineaBoleta> tabla = new TableView<>();
    private final VBox documento = new VBox(10);
    private final Label datosEstudiante = new Label();
    private final Label datosPeriodo = new Label();
    private final Label resumen = new Label();
    private final Label estado = new Label();
    private final Button imprimir = new Button("Imprimir / Guardar como PDF");
    private List<LineaBoleta> todasLasNotas = List.of();

    public BoletaPanel(Rol rol, int idUsuario) {
        this.rol = rol;
        this.idUsuario = idUsuario;
        setSpacing(12);

        estudiante.setPromptText("Selecciona un estudiante");
        estudiante.setPrefWidth(420);
        estudiante.setCellFactory(c -> new ListCell<>() {
            @Override protected void updateItem(Estudiante item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNombreCompleto() + " (" + item.getMatricula() + ")");
            }
        });
        estudiante.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Estudiante item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNombreCompleto() + " (" + item.getMatricula() + ")");
            }
        });
        estudiante.setOnAction(e -> cargarNotas());
        periodo.setPromptText("Período");
        periodo.setOnAction(e -> mostrarPeriodo());
        imprimir.setOnAction(e -> imprimirBoleta());

        TableColumn<LineaBoleta, String> codigo = col("Código", l -> l.codigoAsignatura());
        codigo.setMaxWidth(90);
        TableColumn<LineaBoleta, String> materia = col("Asignatura", l -> l.asignatura());
        TableColumn<LineaBoleta, String> parcial1 = col("Parcial 1", l -> Dialogos.nota(l.parcial1()));
        TableColumn<LineaBoleta, String> parcial2 = col("Parcial 2", l -> Dialogos.nota(l.parcial2()));
        TableColumn<LineaBoleta, String> examen = col("Examen final", l -> Dialogos.nota(l.examenFinal()));
        TableColumn<LineaBoleta, String> nota = col("Nota final", l -> Dialogos.nota(l.notaFinal()));
        TableColumn<LineaBoleta, String> literal = col("Letra", l -> l.literal() == null ? "-" : l.literal());
        literal.setMaxWidth(65);
        TableColumn<LineaBoleta, String> estadoMateria = col("Estado", l -> l.estado() == null ? "Inscrita" : l.estado());
        estadoMateria.setMaxWidth(100);
        tabla.getColumns().addAll(codigo, materia, parcial1, parcial2, examen, nota, literal, estadoMateria);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPrefHeight(330);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Label encabezado = new Label("SIGE · SISTEMA DE GESTIÓN EDUCATIVA");
        encabezado.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label titulo = new Label("BOLETA DE CALIFICACIONES");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        documento.setPadding(new Insets(22));
        documento.setStyle("-fx-background-color: white; -fx-border-color: #cccccc;");
        documento.getChildren().addAll(encabezado, titulo, datosEstudiante, datosPeriodo, tabla, resumen);

        HBox selectores = new HBox(10, new Label("Estudiante:"), estudiante,
                new Label("Período:"), periodo, imprimir);
        selectores.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(documento, Priority.ALWAYS);
        getChildren().addAll(Dialogos.titulo("Boleta académica"), selectores, documento, estado);
        cargarEstudiantes();
    }

    private TableColumn<LineaBoleta, String> col(String titulo,
            java.util.function.Function<LineaBoleta, String> valor) {
        TableColumn<LineaBoleta, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new SimpleStringProperty(valor.apply(c.getValue())));
        return columna;
    }

    private void cargarEstudiantes() {
        try {
            List<Estudiante> disponibles = switch (rol) {
                case ADMIN -> new UsuarioDAO().listarEstudiantes();
                case PADRE -> new VinculacionDAO().listarHijos(idUsuario);
                case ESTUDIANTE -> new UsuarioDAO().listarEstudiantes().stream()
                        .filter(e -> e.getId() == idUsuario).toList();
                default -> List.of();
            };
            estudiante.getItems().setAll(disponibles);
            if (disponibles.isEmpty()) {
                Dialogos.estado(estado, rol == Rol.PADRE ? "No tienes hijos vinculados para emitir una boleta."
                        : "No hay estudiantes disponibles.", false);
                imprimir.setDisable(true);
            } else {
                estudiante.setValue(disponibles.get(0));
                if (rol == Rol.ESTUDIANTE) estudiante.setDisable(true);
                cargarNotas();
            }
        } catch (SQLException e) {
            Dialogos.estado(estado, "No se pudieron cargar los estudiantes para la boleta.", true);
        }
    }

    private void cargarNotas() {
        Estudiante elegido = estudiante.getValue();
        if (elegido == null) return;
        try {
            BoletaDAO dao = new BoletaDAO();
            todasLasNotas = switch (rol) {
                case ADMIN -> dao.listarParaAdministrador(elegido.getId());
                case PADRE -> dao.listarParaPadre(idUsuario, elegido.getId());
                case ESTUDIANTE -> dao.listarParaEstudiante(idUsuario);
                default -> List.of();
            };
            List<String> periodos = todasLasNotas.stream().map(LineaBoleta::periodo).distinct()
                    .sorted(Comparator.reverseOrder()).toList();
            periodo.getItems().setAll(periodos);
            if (periodos.isEmpty()) {
                tabla.getItems().clear();
                datosEstudiante.setText("Estudiante: " + elegido.getNombreCompleto() + "    Matrícula: "
                        + elegido.getMatricula() + "    Grado: " + elegido.getGrado());
                datosPeriodo.setText("Sin inscripciones registradas");
                resumen.setText("");
                imprimir.setDisable(true);
                Dialogos.estado(estado, "Este estudiante aún no tiene materias inscritas.", false);
            } else {
                periodo.setValue(periodos.get(0));
                mostrarPeriodo();
            }
        } catch (SQLException e) {
            tabla.getItems().clear();
            imprimir.setDisable(true);
            Dialogos.estado(estado, "No se pudieron cargar las calificaciones para la boleta.", true);
        }
    }

    private void mostrarPeriodo() {
        String elegidoPeriodo = periodo.getValue();
        Estudiante elegido = estudiante.getValue();
        if (elegidoPeriodo == null || elegido == null) return;
        List<LineaBoleta> filas = todasLasNotas.stream().filter(l -> elegidoPeriodo.equals(l.periodo())).toList();
        tabla.getItems().setAll(filas);
        datosEstudiante.setText("Estudiante: " + elegido.getNombreCompleto() + "    Matrícula: "
                + elegido.getMatricula() + "    Grado: " + elegido.getGrado());
        datosPeriodo.setText("Período: " + elegidoPeriodo + "    Fecha de emisión: " + LocalDate.now());
        List<LineaBoleta> evaluadas = filas.stream()
                .filter(l -> "Aprobada".equals(l.estado()) || "Reprobada".equals(l.estado())).toList();
        long aprobadas = evaluadas.stream().filter(l -> "Aprobada".equals(l.estado())).count();
        double media = evaluadas.stream().mapToDouble(LineaBoleta::notaFinal).average().orElse(0);
        resumen.setText(String.format("Materias: %d    Evaluadas: %d    Aprobadas: %d    Promedio: %.2f",
                filas.size(), evaluadas.size(), aprobadas, media));
        imprimir.setDisable(filas.isEmpty());
        Dialogos.estado(estado, "Boleta lista para imprimir o guardar como PDF.", false);
    }

    private void imprimirBoleta() {
        if (tabla.getItems().isEmpty()) return;
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            Dialogos.error("No se encontró una impresora disponible en el sistema.");
            return;
        }
        if (!job.showPrintDialog(getScene() == null ? null : getScene().getWindow())) return;
        PageLayout layout = job.getPrinter().createPageLayout(Paper.A4, PageOrientation.PORTRAIT,
                javafx.print.Printer.MarginType.DEFAULT);
        boolean impresa = job.printPage(layout, crearDocumentoImpresion());
        if (impresa && job.endJob()) {
            Dialogos.estado(estado, "Boleta enviada a la impresora seleccionada.", false);
        } else {
            job.cancelJob();
            Dialogos.error("No se pudo imprimir la boleta.");
        }
    }

    /** Construye una versión de impresión compacta; evita las filas vacías del TableView. */
    private Node crearDocumentoImpresion() {
        VBox pagina = new VBox(9);
        pagina.setPadding(new Insets(16));
        pagina.setPrefWidth(500);
        pagina.setMinWidth(500);
        pagina.setMaxWidth(500);
        pagina.setStyle("-fx-background-color: white; -fx-text-fill: #202020;");

        Label institucion = new Label("SIGE · SISTEMA DE GESTIÓN EDUCATIVA");
        institucion.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        Label titulo = new Label("BOLETA DE CALIFICACIONES");
        titulo.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");
        Label estudianteInfo = new Label(datosEstudiante.getText());
        estudianteInfo.setWrapText(true);
        Label periodoInfo = new Label(datosPeriodo.getText());
        periodoInfo.setWrapText(true);
        pagina.getChildren().addAll(institucion, titulo, estudianteInfo, periodoInfo);

        GridPane tablaCompacta = new GridPane();
        tablaCompacta.setPrefWidth(468);
        double[] anchos = {10, 29, 7, 7, 11, 9, 7, 20};
        for (double ancho : anchos) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(ancho);
            columna.setMinWidth(0);
            columna.setMaxWidth(Double.MAX_VALUE);
            tablaCompacta.getColumnConstraints().add(columna);
        }
        String[] encabezados = {"Código", "Asignatura", "P1", "P2", "Examen", "Final", "Letra", "Estado"};
        for (int c = 0; c < encabezados.length; c++) {
            tablaCompacta.add(celda(encabezados[c], true, Pos.CENTER), c, 0);
        }
        for (int fila = 0; fila < tabla.getItems().size(); fila++) {
            LineaBoleta nota = tabla.getItems().get(fila);
            String[] valores = {nota.codigoAsignatura(), nota.asignatura(), Dialogos.nota(nota.parcial1()),
                    Dialogos.nota(nota.parcial2()), Dialogos.nota(nota.examenFinal()),
                    Dialogos.nota(nota.notaFinal()), nota.literal() == null ? "-" : nota.literal(),
                    nota.estado() == null ? "Inscrita" : nota.estado()};
            for (int c = 0; c < valores.length; c++) {
                tablaCompacta.add(celda(valores[c], false, c == 1 ? Pos.CENTER_LEFT : Pos.CENTER), c, fila + 1);
            }
        }
        pagina.getChildren().addAll(tablaCompacta, new Label(resumen.getText()));
        return pagina;
    }

    private Label celda(String texto, boolean encabezado, Pos alineacion) {
        Label etiqueta = new Label(texto == null ? "" : texto);
        etiqueta.setMaxWidth(Double.MAX_VALUE);
        etiqueta.setWrapText(true);
        etiqueta.setAlignment(alineacion);
        etiqueta.setStyle("-fx-font-size: 8.5px; -fx-padding: 4px 3px; -fx-border-color: #c8c8c8; "
                + "-fx-border-width: 0 1 1 0;"
                + (encabezado ? " -fx-font-weight: bold; -fx-background-color: #eeeeee;" : ""));
        return etiqueta;
    }
}
