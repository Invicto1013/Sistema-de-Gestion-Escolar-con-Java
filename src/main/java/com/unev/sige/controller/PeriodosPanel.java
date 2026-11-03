package com.unev.sige.controller;

import com.unev.sige.data.PeriodoDAO;
import com.unev.sige.data.PermisoDenegadoException;
import com.unev.sige.model.Periodo;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Function;

/** Períodos lectivos: crear, cerrar (bloquea notas y matrícula) y reabrir con motivo. Solo administrador. */
public final class PeriodosPanel extends BorderPane {

    private final int idAdmin;
    private final PeriodoDAO dao = new PeriodoDAO();
    private final TableView<Periodo> tabla = new TableView<>();
    private final Label estado = new Label();
    private final TablaPaginada<Periodo> vista = new TablaPaginada<>(tabla, p -> p.nombre() + " " + p.estado().name()
            + " " + (p.cerradoPor() == null ? "" : p.cerradoPor()));

    public PeriodosPanel(int idAdmin) {
        this.idAdmin = idAdmin;
        setPadding(new Insets(8));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        columna("Período", Periodo::nombre);
        columna("Inicio", p -> p.inicio() == null ? "—" : p.inicio().toString());
        columna("Fin", p -> p.fin() == null ? "—" : p.fin().toString());
        columna("Estado", p -> p.estado().name());
        columna("Cerrado el", p -> p.fechaCierre() == null ? "—" : p.fechaCierre().toString());

        Button nuevo = new Button("Nuevo período");
        nuevo.setOnAction(e -> onNuevo());
        Button cerrar = new Button("Cerrar período");
        cerrar.setOnAction(e -> onCerrar());
        Button reabrir = new Button("Reabrir…");
        reabrir.setOnAction(e -> onReabrir());
        HBox barra = new HBox(10, nuevo, cerrar, reabrir, estado);
        barra.setPadding(new Insets(0, 0, 10, 0));
        setTop(barra);
        vista.setTituloPdf("Períodos lectivos");
        setCenter(vista);
        recargar();
    }

    private void columna(String nombre, Function<Periodo, String> valor) {
        TableColumn<Periodo, String> c = new TableColumn<>(nombre);
        c.setCellValueFactory(f -> new SimpleStringProperty(valor.apply(f.getValue())));
        tabla.getColumns().add(c);
    }

    private void recargar() {
        try {
            vista.setDatos(dao.listar());
            Dialogos.estado(estado, "Períodos: " + vista.totalDatos(), false);
        } catch (SQLException e) {
            e.printStackTrace();
            Dialogos.estado(estado, "No se pudieron cargar los períodos. Revisa la conexión y ejecuta sql/fase_periodos_auditoria.sql.", true);
        }
    }

    private void onNuevo() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Nuevo período");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField nombre = new TextField();
        nombre.setPromptText("Ej. 2026-1");
        DatePicker ini = new DatePicker();
        DatePicker fin = new DatePicker();
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(16));
        g.addRow(0, new Label("Nombre:"), nombre);
        g.addRow(1, new Label("Inicio:"), ini);
        g.addRow(2, new Label("Fin:"), fin);
        d.getDialogPane().setContent(g);
        Dialogos.aplicarEstilos(d);
        Optional<ButtonType> r = d.showAndWait();
        if (r.isEmpty() || r.get() != ButtonType.OK) {
            return;
        }
        try {
            dao.crear(nombre.getText(), ini.getValue(), fin.getValue(), idAdmin);
        } catch (PermisoDenegadoException | IllegalArgumentException e) {
            Dialogos.error(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            Dialogos.error("No se pudo crear el período. Revisa la conexión con MySQL.");
        }
        recargar();
    }

    private void onCerrar() {
        Periodo p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            Dialogos.error("Selecciona un período.");
            return;
        }
        try {
            if (p.cerrado()) {
                Dialogos.error("El período " + p.nombre() + " ya está cerrado.");
                return;
            }
            String resumen = dao.vistaPrevia(p.id()).texto();
            if (!Dialogos.confirmar("Cerrar período", "Se cerrará " + p.nombre() + ": " + resumen
                    + ".\n\nLas notas y las matrículas quedarán bloqueadas y se guardará el historial académico.")) {
                return;
            }
            Dialogos.informacion("Período cerrado: " + dao.cerrar(p.id(), idAdmin).texto() + ".");
        } catch (PermisoDenegadoException e) {
            Dialogos.error(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            Dialogos.error("No se pudo cerrar el período. Revisa la conexión con MySQL.");
        }
        recargar();
    }

    private void onReabrir() {
        Periodo p = tabla.getSelectionModel().getSelectedItem();
        if (p == null || !p.cerrado()) {
            Dialogos.error("Selecciona un período cerrado.");
            return;
        }
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Reabrir período");
        d.setHeaderText("Reabrir " + p.nombre() + " (queda auditado)");
        d.setContentText("Motivo (10 a 300 caracteres):");
        Dialogos.aplicarEstilos(d);
        Optional<String> motivo = d.showAndWait();
        if (motivo.isEmpty()) {
            return;
        }
        try {
            dao.reabrir(p.id(), idAdmin, motivo.get());
        } catch (PermisoDenegadoException | IllegalArgumentException e) {
            Dialogos.error(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            Dialogos.error("No se pudo reabrir el período. Revisa la conexión con MySQL.");
        }
        recargar();
    }
}
