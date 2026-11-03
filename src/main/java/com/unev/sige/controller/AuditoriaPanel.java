package com.unev.sige.controller;

import com.unev.sige.data.AuditoriaDAO;
import com.unev.sige.data.AuditoriaDAO.Registro;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.util.function.Function;

/** Últimos 1000 movimientos auditados (solo lectura). Los filtros y el CSV están en la versión web. */
public final class AuditoriaPanel extends BorderPane {

    private final AuditoriaDAO dao = new AuditoriaDAO();
    private final TableView<Registro> tabla = new TableView<>();
    private final Label estado = new Label();
    private final TablaPaginada<Registro> vista = new TablaPaginada<>(tabla, r -> r.fecha() + " "
            + (r.usuario() == null ? "" : r.usuario()) + " " + r.rol() + " " + r.accion() + " "
            + r.detalle() + " " + (r.motivo() == null ? "" : r.motivo()) + " " + r.resultado());

    public AuditoriaPanel() {
        setPadding(new Insets(8));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        columna("Fecha", Registro::fecha);
        columna("Usuario", r -> r.usuario() == null ? "—" : r.usuario());
        columna("Acción", Registro::accion);
        columna("Detalle", Registro::detalle);
        columna("Motivo", r -> r.motivo() == null ? "" : r.motivo());
        columna("Resultado", Registro::resultado);
        Button actualizar = new Button("Actualizar");
        actualizar.setOnAction(e -> cargar());
        HBox barra = new HBox(12, actualizar, estado);
        barra.setPadding(new Insets(0, 0, 10, 0));
        setTop(barra);
        vista.setTituloPdf("Auditoría");
        setCenter(vista);
        cargar();
    }

    private void columna(String nombre, Function<Registro, String> valor) {
        TableColumn<Registro, String> c = new TableColumn<>(nombre);
        c.setCellValueFactory(f -> new SimpleStringProperty(valor.apply(f.getValue())));
        tabla.getColumns().add(c);
    }

    private void cargar() {
        try {
            vista.setDatos(dao.listar(AuditoriaDAO.Filtro.vacio(), 1000, 0));
            Dialogos.estado(estado, "Se cargaron los últimos " + vista.totalDatos() + " movimientos (máximo 1000). Para más, usa la auditoría web.", false);
        } catch (SQLException e) {
            e.printStackTrace();
            vista.setDatos(java.util.List.of());
            Dialogos.estado(estado, "No se pudo cargar la auditoría. Revisa la conexión.", true);
        }
    }
}
