package com.unev.sige.controller;

import com.unev.sige.app.SesionActual;
import com.unev.sige.data.HerramientaRegistroDAO;
import com.unev.sige.model.CatalogoHerramientas;
import com.unev.sige.model.HerramientaEscolar;
import com.unev.sige.model.IconosHerramientas;
import com.unev.sige.model.Rol;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Herramientas escolares en el escritorio: las mismas calculadoras y validaciones que la web,
 * filtradas por el rol de quien inició sesión. «Calcular» solo consulta; «Calcular y guardar» guarda el registro.
 */
public final class HerramientasPanel extends VBox {

    private final TextField buscador = new TextField();
    private final Accordion menu = new Accordion();
    private final Label cabecera = new Label();
    private final VBox detalle = new VBox(8);
    private final List<ListView<HerramientaEscolar>> listas = new java.util.ArrayList<>();
    private boolean sincronizando;
    private final Label descripcion = new Label();
    private final GridPane formulario = new GridPane();
    private final TextArea resultado = new TextArea();
    private final Map<String, Supplier<String>> lectores = new LinkedHashMap<>();
    private HerramientaEscolar actual;

    public HerramientasPanel(Rol rol) {
        setSpacing(12);
        List<HerramientaEscolar> disponibles = CatalogoHerramientas.paraRol(rol);

        buscador.setPromptText("🔎 Buscar herramienta…");
        buscador.getStyleClass().add("herr-buscador");
        buscador.textProperty().addListener((obs, a, n) -> construirMenu(rol, n));
        menu.getStyleClass().add("herr-menu");
        cabecera.getStyleClass().add("herr-cabecera");
        detalle.getStyleClass().add("herr-detalle");

        descripcion.setWrapText(true);
        formulario.setHgap(10);
        formulario.setVgap(8);
        formulario.setPadding(new Insets(4, 0, 4, 0));

        Button calcular = new Button("Calcular");
        calcular.getStyleClass().add("primary");
        calcular.setOnAction(e -> calcular(false));
        Button guardar = new Button("Calcular y guardar");
        guardar.getStyleClass().add("herr-secundario");
        guardar.setOnAction(e -> calcular(true));

        resultado.setEditable(false);
        resultado.setWrapText(true);
        resultado.setPrefRowCount(8);
        resultado.setPromptText("El resultado aparecerá aquí. Es solo una consulta: no se guarda nada.");

        detalle.getChildren().addAll(cabecera, descripcion, formulario, new HBox(8, calcular, guardar), resultado);
        ScrollPane desplazable = new ScrollPane(detalle);
        desplazable.setFitToWidth(true);
        HBox.setHgrow(desplazable, Priority.ALWAYS);

        VBox lateral = new VBox(8, buscador, menu);
        lateral.setPrefWidth(300);
        lateral.setMinWidth(260);
        VBox.setVgrow(menu, Priority.ALWAYS);
        HBox cuerpo = new HBox(12, lateral, desplazable);
        VBox.setVgrow(cuerpo, Priority.ALWAYS);

        getChildren().addAll(Dialogos.titulo("🧰 Herramientas escolares"),
                new Label("Calculadoras y validaciones de las reglas del centro para tu rol (" + disponibles.size() + " disponibles)."),
                cuerpo);
        construirMenu(rol, "");
        if (!disponibles.isEmpty()) {
            menu.getPanes().stream().findFirst().ifPresent(p -> menu.setExpandedPane(p));
            listas.stream().filter(l -> !l.getItems().isEmpty()).findFirst().ifPresent(l -> l.getSelectionModel().selectFirst());
        }
    }

    /** Un panel plegable por grupo, con icono, contador y color de acento; filtra por el texto buscado. */
    private void construirMenu(Rol rol, String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase(java.util.Locale.ROOT);
        menu.getPanes().clear();
        listas.clear();
        for (Map.Entry<String, List<HerramientaEscolar>> e : CatalogoHerramientas.porGrupo(rol).entrySet()) {
            List<HerramientaEscolar> items = e.getValue().stream()
                    .filter(h -> f.isEmpty() || h.titulo().toLowerCase(java.util.Locale.ROOT).contains(f)
                            || h.descripcion().toLowerCase(java.util.Locale.ROOT).contains(f))
                    .toList();
            if (items.isEmpty()) continue;
            ListView<HerramientaEscolar> lista = new ListView<>();
            lista.getItems().setAll(items);
            lista.getStyleClass().add("herr-lista-fx");
            lista.setPrefHeight(items.size() * 34 + 4);
            lista.setCellFactory(v -> new ListCell<>() {
                @Override
                protected void updateItem(HerramientaEscolar h, boolean vacio) {
                    super.updateItem(h, vacio);
                    setText(vacio || h == null ? null : IconosHerramientas.icono(h.id()) + "  " + h.titulo());
                }
            });
            lista.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nueva) -> {
                if (nueva == null || sincronizando) return;
                sincronizando = true;
                listas.stream().filter(l -> l != lista).forEach(l -> l.getSelectionModel().clearSelection());
                sincronizando = false;
                mostrar(nueva);
            });
            TitledPane panel = new TitledPane(IconosHerramientas.iconoGrupo(e.getKey()) + "  " + e.getKey() + "  (" + items.size() + ")", lista);
            panel.setAnimated(true);
            panel.getStyleClass().add("herr-grupo-fx");
            panel.setStyle("-fx-accent: " + IconosHerramientas.acento(e.getKey()) + ";");
            listas.add(lista);
            menu.getPanes().add(panel);
        }
        if (!f.isEmpty() && !menu.getPanes().isEmpty()) menu.setExpandedPane(menu.getPanes().get(0));
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
        cabecera.setText(IconosHerramientas.icono(h.id()) + "  " + h.titulo());
        detalle.setOpacity(0);
        detalle.setTranslateY(8);
        FadeTransition fade = new FadeTransition(Duration.millis(260), detalle);
        fade.setToValue(1);
        TranslateTransition sube = new TranslateTransition(Duration.millis(260), detalle);
        sube.setToY(0);
        fade.play();
        sube.play();
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

    private void calcular(boolean guardar) {
        if (actual == null) return;
        Map<String, String> valores = new LinkedHashMap<>();
        lectores.forEach((nombre, lector) -> valores.put(nombre, lector.get()));
        HerramientaEscolar.Resultado r = actual.ejecutar(valores);
        resultado.setText(r.texto());
        if (!guardar) return;
        if (!r.exito()) {
            Dialogos.error("No se guardó: revisa los datos del formulario.");
            return;
        }
        if (SesionActual.getUsuario() == null) {
            Dialogos.error("Inicia sesión para guardar registros.");
            return;
        }
        try {
            int id = new HerramientaRegistroDAO().guardar(SesionActual.getUsuario().getId(), actual, valores, r.texto());
            Dialogos.informacion("Registro N.º " + id + " guardado. Puedes verlo y descargarlo en PDF desde «Mis registros».");
        } catch (IllegalStateException ex) {
            Dialogos.error(ex.getMessage());
        } catch (SQLException ex) {
            boolean falta = "42S02".equals(ex.getSQLState()) || ex.getErrorCode() == 1146;
            Dialogos.error(falta ? "El guardado aún no está activado: ejecuta sql/fase_herramientas_escolares.sql en la base de datos."
                    : "No se pudo guardar el registro: " + ex.getMessage());
        }
    }
}
