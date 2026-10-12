package com.unev.sige.controller;

import com.unev.sige.data.SesionDAO;
import com.unev.sige.model.SesionRegistrada;
import javafx.beans.property.SimpleIntegerProperty;
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

/** Historial de accesos recientes disponible para el administrador. */
public final class HistorialSesionesPanel extends BorderPane {

    private final int idAdministrador;
    private final SesionDAO dao = new SesionDAO();
    private final TableView<SesionRegistrada> tabla = new TableView<>();
    private final Label estado = new Label("Se muestran hasta 500 sesiones recientes.");

    public HistorialSesionesPanel(int idAdministrador) {
        this.idAdministrador = idAdministrador;
        setPadding(new Insets(8));
        crearTabla();

        Button actualizar = new Button("Actualizar");
        actualizar.setOnAction(e -> cargar());
        HBox barra = new HBox(12, actualizar, estado);
        barra.setPadding(new Insets(0, 0, 10, 0));
        setTop(barra);
        setCenter(tabla);
        cargar();
    }

    private void crearTabla() {
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<SesionRegistrada, Number> id = new TableColumn<>("ID");
        id.setCellValueFactory(f -> new SimpleIntegerProperty(f.getValue().idSesion()));
        tabla.getColumns().add(id);
        agregarColumnaTexto("Usuario", SesionRegistrada::usuario);
        agregarColumnaTexto("Correo", SesionRegistrada::correo);
        agregarColumnaTexto("Inicio", SesionRegistrada::inicio);
        agregarColumnaTexto("Cierre", SesionRegistrada::cierre);
        agregarColumnaTexto("Dirección IP", SesionRegistrada::direccionIP);
        agregarColumnaTexto("Estado", SesionRegistrada::estado);
    }

    private void agregarColumnaTexto(String nombre, Function<SesionRegistrada, String> valor) {
        TableColumn<SesionRegistrada, String> columna = new TableColumn<>(nombre);
        columna.setCellValueFactory(f -> new SimpleStringProperty(valor.apply(f.getValue())));
        tabla.getColumns().add(columna);
    }

    private void cargar() {
        try {
            tabla.getItems().setAll(dao.listarRecientes(idAdministrador));
            estado.setText("Sesiones encontradas: " + tabla.getItems().size()
                    + " (máximo 500 recientes).");
        } catch (SQLException e) {
            tabla.getItems().clear();
            estado.setText("No se pudo cargar el historial. Revisa la conexión y los permisos.");
        }
    }
}
