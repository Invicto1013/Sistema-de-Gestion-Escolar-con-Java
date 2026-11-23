package com.unev.sige.controller;

import com.unev.sige.model.CatalogoHerramientas;
import com.unev.sige.model.HerramientaEscolar;
import com.unev.sige.model.Rol;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Herramientas escolares en el escritorio: las mismas calculadoras y validaciones que la web,
 * filtradas por el rol de quien inició sesión. Solo consulta; no guarda datos.
 */
public final class HerramientasPanel extends VBox {

    private final ComboBox<HerramientaEscolar> selector = new ComboBox<>();
    private final Label descripcion = new Label();
    private final GridPane formulario = new GridPane();
    private final TextArea resultado = new TextArea();
    private final Map<String, Supplier<String>> lectores = new LinkedHashMap<>();
    private HerramientaEscolar actual;

    public HerramientasPanel(Rol rol) {
        setSpacing(12);
        List<HerramientaEscolar> disponibles = CatalogoHerramientas.paraRol(rol);

        selector.getItems().setAll(disponibles);
        selector.setMaxWidth(Double.MAX_VALUE);
        selector.setPromptText("Elige una herramienta");
        selector.setConverter(new StringConverter<>() {
            @Override
            public String toString(HerramientaEscolar h) {
                return h == null ? "" : h.grupo() + " — " + h.titulo();
            }

            @Override
            public HerramientaEscolar fromString(String texto) {
                return null;
            }
        });
        selector.valueProperty().addListener((obs, anterior, nueva) -> mostrar(nueva));

        descripcion.setWrapText(true);
        formulario.setHgap(10);
        formulario.setVgap(8);
        formulario.setPadding(new Insets(4, 0, 4, 0));

        Button calcular = new Button("Calcular");
        calcular.getStyleClass().add("primary");
        calcular.setOnAction(e -> calcular());

        resultado.setEditable(false);
        resultado.setWrapText(true);
        resultado.setPrefRowCount(8);
        resultado.setPromptText("El resultado aparecerá aquí. Es solo una consulta: no se guarda nada.");

        ScrollPane desplazable = new ScrollPane(new VBox(8, descripcion, formulario, calcular, resultado));
        desplazable.setFitToWidth(true);
        VBox.setVgrow(desplazable, Priority.ALWAYS);

        getChildren().addAll(Dialogos.titulo("Herramientas escolares"),
                new Label("Calculadoras y validaciones de las reglas del centro para tu rol (" + disponibles.size() + " disponibles)."),
                selector, desplazable);
        if (!disponibles.isEmpty()) selector.getSelectionModel().selectFirst();
    }

    private void mostrar(HerramientaEscolar h) {
        actual = h;
        formulario.getChildren().clear();
        lectores.clear();
        resultado.clear();
        if (h == null) {
            descripcion.setText("");
            return;
        }
        descripcion.setText(h.descripcion());
        int fila = 0;
        for (HerramientaEscolar.Campo c : h.campos()) {
            Label etiqueta = new Label(c.etiqueta());
            etiqueta.setWrapText(true);
            formulario.add(etiqueta, 0, fila);
            formulario.add(crearControl(c), 1, fila);
            fila++;
        }
    }

    private javafx.scene.Node crearControl(HerramientaEscolar.Campo c) {
        switch (c.tipo()) {
            case LISTA -> {
                TextArea area = new TextArea(c.valorInicial());
                area.setPrefRowCount(4);
                area.setPrefWidth(420);
                lectores.put(c.nombre(), area::getText);
                return area;
            }
            case SI_NO -> {
                CheckBox caja = new CheckBox("Sí");
                caja.setSelected("si".equalsIgnoreCase(c.valorInicial()));
                lectores.put(c.nombre(), () -> caja.isSelected() ? "si" : "no");
                return caja;
            }
            case OPCION -> {
                ComboBox<String> combo = new ComboBox<>();
                combo.getItems().setAll(c.opciones());
                combo.setValue(c.valorInicial());
                lectores.put(c.nombre(), () -> combo.getValue() == null ? "" : combo.getValue());
                return combo;
            }
            case FECHA -> {
                DatePicker fecha = new DatePicker();
                try {
                    fecha.setValue(LocalDate.parse(c.valorInicial()));
                } catch (DateTimeParseException e) {
                    fecha.setValue(null);
                }
                lectores.put(c.nombre(), () -> fecha.getValue() == null ? "" : fecha.getValue().toString());
                return fecha;
            }
            default -> {
                TextField campo = new TextField(c.valorInicial());
                campo.setPrefWidth(420);
                if (c.tipo() == HerramientaEscolar.Tipo.HORA) campo.setPromptText("HH:MM");
                lectores.put(c.nombre(), campo::getText);
                return campo;
            }
        }
    }

    private void calcular() {
        if (actual == null) return;
        Map<String, String> valores = new LinkedHashMap<>();
        lectores.forEach((nombre, lector) -> valores.put(nombre, lector.get()));
        resultado.setText(actual.ejecutar(valores).texto());
    }
}
