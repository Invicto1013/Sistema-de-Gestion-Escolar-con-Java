package com.unev.sige.controller;

import com.unev.sige.data.HerramientaRegistroDAO;
import com.unev.sige.data.PermisoDenegadoException;
import com.unev.sige.model.RegistroHerramienta;
import com.unev.sige.model.ReportePdfHerramientas;
import com.unev.sige.model.Rol;
import com.unev.sige.model.Usuario;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Registros guardados de «Herramientas»: cada usuario ve los suyos; administración y dirección ven los de todos.
 * Permite consultar el detalle, descargar en PDF (uno o todos) y eliminar.
 */
public final class RegistrosHerramientasPanel extends VBox {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Usuario usuario;
    private final boolean verTodos;
    private final HerramientaRegistroDAO dao = new HerramientaRegistroDAO();
    private final TableView<RegistroHerramienta> tabla = new TableView<>();
    private final TextArea detalle = new TextArea();

    public RegistrosHerramientasPanel(Usuario usuario) {
        this.usuario = usuario;
        this.verTodos = usuario.getRol() == Rol.ADMIN || usuario.getRol() == Rol.DIRECTOR;
        setSpacing(10);

        TableColumn<RegistroHerramienta, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().creado() == null ? "" : c.getValue().creado().format(FECHA)));
        TableColumn<RegistroHerramienta, String> colHerramienta = new TableColumn<>("Herramienta");
        colHerramienta.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().titulo()));
        TableColumn<RegistroHerramienta, String> colAutor = new TableColumn<>("Autor");
        colAutor.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().autor()));
        TableColumn<RegistroHerramienta, String> colResultado = new TableColumn<>("Resultado");
        colResultado.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().resultado() == null ? "" : c.getValue().resultado().split("\\r?\\n", 2)[0]));
        tabla.getColumns().add(colFecha);
        tabla.getColumns().add(colHerramienta);
        if (verTodos) tabla.getColumns().add(colAutor);
        tabla.getColumns().add(colResultado);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("Todavía no hay registros. Usa «Calcular y guardar» en Herramientas."));
        VBox.setVgrow(tabla, Priority.ALWAYS);
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nuevo) -> mostrarDetalle(nuevo));

        detalle.setEditable(false);
        detalle.setWrapText(true);
        detalle.setPrefRowCount(8);

        Button pdfUno = new Button("PDF del seleccionado");
        pdfUno.setOnAction(e -> descargarSeleccionado());
        Button pdfTodos = new Button("PDF de todo el listado");
        pdfTodos.setOnAction(e -> descargarListado());
        Button eliminar = new Button("Eliminar seleccionado");
        eliminar.getStyleClass().add("danger");
        eliminar.setOnAction(e -> eliminarSeleccionado());
        Button actualizar = new Button("Actualizar");
        actualizar.setOnAction(e -> cargar());

        getChildren().addAll(Dialogos.titulo("Registros de herramientas"),
                new Label(verTodos ? "Como " + usuario.getRol().etiqueta() + " ves los registros de todos los usuarios."
                        : "Estos son los cálculos que guardaste."),
                new HBox(8, pdfUno, pdfTodos, eliminar, actualizar), tabla, detalle);
        cargar();
    }

    private void cargar() {
        try {
            tabla.setItems(FXCollections.observableArrayList(dao.listar(usuario.getId(), verTodos)));
            detalle.clear();
        } catch (SQLException ex) {
            Dialogos.error(mensajeSql(ex));
        }
    }

    private static String mensajeSql(SQLException ex) {
        boolean falta = "42S02".equals(ex.getSQLState()) || ex.getErrorCode() == 1146;
        return falta ? "El guardado aún no está activado: ejecuta sql/fase_herramientas_escolares.sql en la base de datos."
                : "No se pudo completar la operación con los registros: " + ex.getMessage();
    }

    /** El listado trae solo la cabecera; el detalle (datos ingresados) se pide por separado. */
    private RegistroHerramienta completo(RegistroHerramienta r) throws SQLException {
        return dao.obtener(r.id()).orElse(null);
    }

    private void mostrarDetalle(RegistroHerramienta r) {
        if (r == null) {
            detalle.clear();
            return;
        }
        try {
            RegistroHerramienta c = completo(r);
            if (c == null) return;
            StringBuilder b = new StringBuilder("DATOS INGRESADOS\n");
            for (RegistroHerramienta.Dato d : c.datos()) b.append(d.etiqueta()).append(": ").append(d.valor()).append('\n');
            b.append("\nRESULTADO\n").append(c.resultado());
            detalle.setText(b.toString());
        } catch (SQLException ex) {
            Dialogos.error(mensajeSql(ex));
        }
    }

    private void guardarPdf(byte[] pdf, String nombreSugerido) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar PDF");
        selector.setInitialFileName(nombreSugerido);
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File destino = selector.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (destino == null) return;
        try {
            Files.write(destino.toPath(), pdf);
            Dialogos.informacion("PDF guardado en:\n" + destino.getAbsolutePath());
        } catch (IOException ex) {
            Dialogos.error("No se pudo guardar el PDF: " + ex.getMessage());
        }
    }

    private void descargarSeleccionado() {
        RegistroHerramienta r = tabla.getSelectionModel().getSelectedItem();
        if (r == null) {
            Dialogos.error("Selecciona un registro de la lista.");
            return;
        }
        try {
            RegistroHerramienta c = completo(r);
            if (c != null) guardarPdf(ReportePdfHerramientas.registro(c), "herramienta-registro-" + c.id() + ".pdf");
        } catch (SQLException ex) {
            Dialogos.error(mensajeSql(ex));
        }
    }

    private void descargarListado() {
        List<RegistroHerramienta> lista = tabla.getItems();
        if (lista.isEmpty()) {
            Dialogos.error("No hay registros para descargar.");
            return;
        }
        guardarPdf(ReportePdfHerramientas.listado("Registros de herramientas escolares", List.copyOf(lista), verTodos),
                "herramientas-registros.pdf");
    }

    private void eliminarSeleccionado() {
        RegistroHerramienta r = tabla.getSelectionModel().getSelectedItem();
        if (r == null) {
            Dialogos.error("Selecciona un registro de la lista.");
            return;
        }
        if (!r.puedeEliminar(usuario.getId(), usuario.getRol())) {
            Dialogos.error("Solo el dueño del registro o un administrador puede eliminarlo.");
            return;
        }
        if (!Dialogos.confirmar("Eliminar registro", "¿Eliminar el registro N.º " + r.id() + " (" + r.titulo() + ")? No se puede deshacer.")) return;
        try {
            dao.eliminar(r.id(), usuario.getId(), usuario.getRol());
            cargar();
        } catch (PermisoDenegadoException ex) {
            Dialogos.error(ex.getMessage());
        } catch (SQLException ex) {
            Dialogos.error(mensajeSql(ex));
        }
    }
}
