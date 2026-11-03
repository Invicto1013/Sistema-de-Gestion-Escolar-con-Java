package com.unev.sige.controller;

import com.unev.sige.model.Paginador;
import com.unev.sige.model.PdfTabla;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Envuelve una TableView con busqueda dinamica (mientras se escribe, sin acentos ni mayusculas),
 * orden por columna sobre TODOS los datos y paginacion (10, 25 o 50 filas por pagina).
 * La tabla original sigue siendo la misma: su seleccion y sus columnas funcionan como antes;
 * solo cambia que sus filas son la pagina actual. Los datos se entregan con setDatos(...).
 */
final class TablaPaginada<T> extends VBox {

    private final TableView<T> tabla;
    private final Function<T, String> textoBusqueda;
    private final TextField txtBuscar = new TextField();
    private final ComboBox<Integer> cmbTamano = new ComboBox<>();
    private final Button btnPrimera = new Button("«");
    private final Button btnAnterior = new Button("‹");
    private final Button btnSiguiente = new Button("›");
    private final Button btnUltima = new Button("»");
    private final Label lblPagina = new Label();
    private final Label lblConteo = new Label();

    private String tituloPdf = "Listado";
    private List<T> datos = new ArrayList<>();
    private List<T> filtrados = new ArrayList<>();
    private int pagina;

    TablaPaginada(TableView<T> tabla, Function<T, String> textoBusqueda) {
        super(8);
        this.tabla = tabla;
        this.textoBusqueda = textoBusqueda;

        txtBuscar.setPromptText("Buscar…");
        txtBuscar.setPrefWidth(280);
        txtBuscar.textProperty().addListener((obs, antes, ahora) -> {
            pagina = 0;
            filtrar();
        });
        Button limpiar = new Button("Limpiar");
        limpiar.setOnAction(e -> txtBuscar.clear());

        cmbTamano.getItems().setAll(10, 25, 50);
        cmbTamano.setValue(10);
        cmbTamano.valueProperty().addListener((obs, antes, ahora) -> {
            pagina = 0;
            mostrar();
        });
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Button btnPdf = new Button("Exportar PDF");
        btnPdf.setOnAction(e -> exportarPdf());
        HBox superior = new HBox(8, txtBuscar, limpiar, espacio, btnPdf, new Label("Filas:"), cmbTamano);
        superior.setAlignment(Pos.CENTER_LEFT);

        btnPrimera.setOnAction(e -> irA(0));
        btnAnterior.setOnAction(e -> irA(pagina - 1));
        btnSiguiente.setOnAction(e -> irA(pagina + 1));
        btnUltima.setOnAction(e -> irA(Integer.MAX_VALUE));
        Region espacioInferior = new Region();
        HBox.setHgrow(espacioInferior, Priority.ALWAYS);
        HBox inferior = new HBox(8, lblConteo, espacioInferior, btnPrimera, btnAnterior, lblPagina, btnSiguiente, btnUltima);
        inferior.setAlignment(Pos.CENTER_LEFT);
        inferior.setPadding(new Insets(2, 0, 0, 0));

        // El orden por columna se aplica a todos los datos, no solo a la pagina visible.
        tabla.setSortPolicy(t -> true);
        tabla.comparatorProperty().addListener((obs, antes, ahora) -> reordenar());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        getChildren().addAll(superior, tabla, inferior);
        mostrar();
    }

    /** Reemplaza todos los datos; conserva la busqueda escrita y vuelve a la primera pagina. */
    void setDatos(List<T> nuevos) {
        datos = new ArrayList<>(nuevos);
        pagina = 0;
        filtrar();
    }

    /** Titulo que lleva el PDF exportado. */
    void setTituloPdf(String titulo) {
        this.tituloPdf = titulo;
    }

    /** Exporta a PDF todas las filas que cumplen la busqueda actual (no solo la pagina visible), en el orden elegido. */
    private void exportarPdf() {
        if (filtrados.isEmpty()) {
            Dialogos.error("No hay filas para exportar.");
            return;
        }
        List<String> encabezados = new ArrayList<>();
        for (javafx.scene.control.TableColumn<T, ?> c : tabla.getColumns()) {
            encabezados.add(c.getText());
        }
        List<String[]> filas = new ArrayList<>();
        for (T fila : filtrados) {
            String[] valores = new String[tabla.getColumns().size()];
            for (int i = 0; i < valores.length; i++) {
                Object v = tabla.getColumns().get(i).getCellData(fila);
                valores[i] = v == null ? "" : String.valueOf(v);
            }
            filas.add(valores);
        }
        String filtro = txtBuscar.getText() == null || txtBuscar.getText().isBlank() ? "" : " · Búsqueda: " + txtBuscar.getText().trim();
        String subtitulo = "Generado el " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + filtro;
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar PDF");
        selector.setInitialFileName(tituloPdf.toLowerCase().replaceAll("[^a-z0-9]+", "-") + ".pdf");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        java.io.File destino = selector.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (destino == null) {
            return;
        }
        try {
            Files.write(destino.toPath(), PdfTabla.generar(tituloPdf, subtitulo, encabezados, filas));
            Dialogos.informacion("PDF guardado en:\n" + destino.getAbsolutePath());
        } catch (IOException ex) {
            Dialogos.error("No se pudo guardar el PDF: " + ex.getMessage());
        }
    }

    /** Cantidad de filas que cumplen la busqueda actual. */
    int totalFiltrado() {
        return filtrados.size();
    }

    int totalDatos() {
        return datos.size();
    }

    private void filtrar() {
        String q = txtBuscar.getText();
        filtrados = new ArrayList<>();
        for (T fila : datos) {
            if (Paginador.coincide(textoBusqueda.apply(fila), q)) {
                filtrados.add(fila);
            }
        }
        reordenar();
    }

    private void reordenar() {
        Comparator<T> orden = tabla.getComparator();
        if (orden != null) {
            filtrados.sort(orden);
        }
        mostrar();
    }

    private void irA(int nueva) {
        pagina = nueva;
        mostrar();
    }

    private int tamano() {
        Integer valor = cmbTamano.getValue();
        return valor == null ? 10 : valor;
    }

    private void mostrar() {
        int total = filtrados.size();
        int tam = tamano();
        pagina = Paginador.limitar(pagina, total, tam);
        int desde = Paginador.desde(pagina, total, tam);
        int hasta = Paginador.hasta(pagina, total, tam);
        tabla.setItems(FXCollections.observableArrayList(filtrados.subList(desde, hasta)));
        int paginas = Paginador.paginas(total, tam);
        lblPagina.setText("Página " + (pagina + 1) + " de " + paginas);
        lblConteo.setText(total == 0 ? "Sin resultados"
                : "Mostrando " + (desde + 1) + "–" + hasta + " de " + total
                        + (total != datos.size() ? " (filtrado de " + datos.size() + ")" : ""));
        btnPrimera.setDisable(pagina == 0);
        btnAnterior.setDisable(pagina == 0);
        btnSiguiente.setDisable(pagina >= paginas - 1);
        btnUltima.setDisable(pagina >= paginas - 1);
    }
}
