package com.unev.sige.controller;

import com.unev.sige.data.DashboardDAO;
import com.unev.sige.model.Rol;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.Chart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import com.unev.sige.model.Usuario;

/** Inicio con indicadores y gráficos reales, consultados fuera del hilo JavaFX. */
final class DashboardInicioPanel extends VBox {
    private final DashboardDAO dao=new DashboardDAO();
    private final Usuario usuario;
    private final VBox cuerpo=new VBox(14);
    private GridPane tarjetasActuales;
    private GridPane graficosActuales;
    private TableView<DashboardDAO.Movimiento> tablaActual;
    private TableColumn<DashboardDAO.Movimiento,String> columnaEstudiante;
    private TableColumn<DashboardDAO.Movimiento,String> columnaPeriodo;
    private ScrollPane desplazable;
    private int ultimaDistribucionTarjetas=-1;
    private int ultimaDistribucionGraficos=-1;
    private Boolean ultimaVistaTelefono;

    DashboardInicioPanel(Usuario usuario) {
        this.usuario=usuario;
        setSpacing(12);setPadding(new Insets(4));setMinWidth(0);getStyleClass().add("dashboard-home");
        Label titulo=new Label("Resumen académico");titulo.getStyleClass().add("section-title");
        Label detalle=new Label("Indicadores y actividad para tu cuenta de "+rolTexto(usuario.getRol())+".");
        ProgressIndicator carga=new ProgressIndicator();carga.setMaxSize(34,34);
        HBox estado=new HBox(10,carga,new Label("Cargando datos actualizados desde MySQL…"));estado.setAlignment(Pos.CENTER_LEFT);
        cuerpo.setFillWidth(true);
        desplazable=new ScrollPane(cuerpo);desplazable.setFitToWidth(true);desplazable.setMinWidth(0);
        desplazable.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(desplazable,Priority.ALWAYS);
        getChildren().addAll(titulo,detalle,estado,desplazable);
        sceneProperty().addListener((obs,anterior,scene)->{if(scene!=null){scene.widthProperty().addListener((o,a,n)->
                Platform.runLater(()->adaptarAncho(scene.getWidth())));
            Platform.runLater(()->adaptarAncho(scene.getWidth()));}});
        cargar(estado);
    }

    private void cargar(HBox estado) {
        Task<DashboardDAO.Datos> tarea=new Task<>() {
            @Override protected DashboardDAO.Datos call() throws Exception {return dao.cargar(usuario.getRol(),usuario.getId());}
        };
        tarea.setOnSucceeded(e->{getChildren().remove(estado);construir(tarea.getValue());});
        tarea.setOnFailed(e->{getChildren().remove(estado);Label error=new Label("No fue posible cargar el dashboard. Revisa la conexión y las tablas académicas.");
            error.getStyleClass().add("error-message");error.setWrapText(true);cuerpo.getChildren().setAll(error);
            if(tarea.getException()!=null)tarea.getException().printStackTrace();});
        Thread hilo=new Thread(tarea,"sige-dashboard");hilo.setDaemon(true);hilo.start();
    }

    private void construir(DashboardDAO.Datos datos) {
        DashboardDAO.Indicadores i=datos.indicadores();
        GridPane tarjetas=new GridPane();tarjetas.setHgap(10);tarjetas.setVgap(10);tarjetasActuales=tarjetas;
        String[] nombres;String[] valores;
        switch(usuario.getRol()) {
            case ADMIN -> {nombres=new String[]{"Usuarios activos","Inscripciones","Secciones","Evaluaciones registradas"};
                valores=new String[]{Long.toString(i.usuariosActivos()),Long.toString(i.inscripciones()),Long.toString(i.secciones()),Long.toString(i.evaluaciones())};}
            case PROFESOR -> {nombres=new String[]{"Estudiantes atendidos","Inscripciones en mis secciones","Secciones asignadas","Evaluaciones registradas"};
                valores=new String[]{Long.toString(i.estudiantes()),Long.toString(i.inscripciones()),Long.toString(i.secciones()),Long.toString(i.evaluaciones())};}
            case ESTUDIANTE -> {nombres=new String[]{"Materias inscritas","Secciones inscritas","Materias evaluadas","Asistencias registradas"};
                valores=new String[]{Long.toString(i.inscripciones()),Long.toString(i.secciones()),Long.toString(i.evaluaciones()),Long.toString(i.asistencias())};}
            case PADRE -> {nombres=new String[]{"Hijos vinculados","Materias de mis hijos","Secciones académicas","Materias evaluadas"};
                valores=new String[]{Long.toString(i.hijos()),Long.toString(i.inscripciones()),Long.toString(i.secciones()),Long.toString(i.evaluaciones())};}
            default -> throw new IllegalStateException();
        }
        for(int x=0;x<nombres.length;x++) {VBox tarjeta=new VBox(7);tarjeta.getStyleClass().add("dashboard-card");
            Label nombre=new Label(nombres[x]);nombre.getStyleClass().add("dashboard-card-label");
            Label valor=new Label(valores[x]);valor.getStyleClass().add("dashboard-card-value");tarjeta.getChildren().addAll(nombre,valor);
            GridPane.setHgrow(tarjeta,Priority.ALWAYS);GridPane.setFillWidth(tarjeta,true);
            tarjetas.add(tarjeta,x%4,x/4);}
        Chart barras=crearBarras(datos),pie=crearPie(datos);
        GridPane graficos=new GridPane();graficos.setHgap(14);graficos.setVgap(14);graficosActuales=graficos;
        GridPane.setHgrow(barras,Priority.ALWAYS);GridPane.setHgrow(pie,Priority.ALWAYS);GridPane.setFillWidth(barras,true);GridPane.setFillWidth(pie,true);
        graficos.add(barras,0,0);graficos.add(pie,1,0);
        TableView<DashboardDAO.Movimiento> tabla=crearTabla(datos);
        Label actividad=new Label("Actividad académica reciente");actividad.getStyleClass().add("section-title");
        Label promedio=new Label(String.format(java.util.Locale.forLanguageTag("es-DO"),"Promedio de notas disponibles: %.2f",i.promedio()));
        tablaActual=tabla;
        cuerpo.getChildren().setAll(tarjetas,graficos,actividad,tabla,promedio);VBox.setVgrow(tabla,Priority.ALWAYS);
        if(getScene()!=null)adaptarAncho(getScene().getWidth());
    }

    private Chart crearBarras(DashboardDAO.Datos datos) {
        CategoryAxis x=new CategoryAxis();x.setLabel("Asignatura");NumberAxis y=new NumberAxis();y.setLabel("Inscripciones");
        BarChart<String,Number> chart=new BarChart<>(x,y);chart.setTitle("Inscripciones por asignatura");chart.setLegendVisible(false);chart.setAnimated(false);
        XYChart.Series<String,Number> serie=new XYChart.Series<>();for(var d:datos.porAsignatura())serie.getData().add(new XYChart.Data<>(d.etiqueta(),d.valor()));
        chart.getData().add(serie);chart.setMinHeight(260);chart.setPrefHeight(300);chart.getStyleClass().add("dashboard-chart");return chart;
    }
    private Chart crearPie(DashboardDAO.Datos datos) {
        PieChart chart=new PieChart();chart.setTitle("Estado de las materias");chart.setAnimated(false);
        for(var d:datos.porEstado())chart.getData().add(new PieChart.Data(d.etiqueta(),d.valor()));
        chart.setMinHeight(260);chart.setPrefHeight(300);chart.getStyleClass().add("dashboard-chart");return chart;
    }
    private TableView<DashboardDAO.Movimiento> crearTabla(DashboardDAO.Datos datos) {
        TableView<DashboardDAO.Movimiento> t=new TableView<>();t.setPlaceholder(new Label("No hay actividad académica para mostrar."));
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<DashboardDAO.Movimiento,String> fecha=new TableColumn<>("Fecha");fecha.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().fecha()));
        TableColumn<DashboardDAO.Movimiento,String> alumno=new TableColumn<>("Estudiante");alumno.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().estudiante()));columnaEstudiante=alumno;
        TableColumn<DashboardDAO.Movimiento,String> materia=new TableColumn<>("Asignatura");materia.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().asignatura()));
        TableColumn<DashboardDAO.Movimiento,String> periodo=new TableColumn<>("Período");periodo.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().periodo()));columnaPeriodo=periodo;
        t.getColumns().addAll(fecha,alumno,materia,periodo);t.getItems().setAll(datos.recientes());t.setPrefHeight(260);return t;
    }
    private void adaptarAncho(double ancho) {
        int columnas=ancho<600?1:ancho<1100?2:4;
        if(tarjetasActuales!=null&&columnas!=ultimaDistribucionTarjetas) {
            ultimaDistribucionTarjetas=columnas;
            var nodos=java.util.List.copyOf(tarjetasActuales.getChildren());tarjetasActuales.getChildren().clear();
            for(int i=0;i<nodos.size();i++)tarjetasActuales.add(nodos.get(i),i%columnas,i/columnas);
        }
        int columnasGrafico=ancho<900?1:2;
        if(graficosActuales!=null&&graficosActuales.getChildren().size()>=2&&columnasGrafico!=ultimaDistribucionGraficos) {
            ultimaDistribucionGraficos=columnasGrafico;
            var nodos=java.util.List.copyOf(graficosActuales.getChildren());graficosActuales.getChildren().clear();
            for(int i=0;i<nodos.size();i++)graficosActuales.add(nodos.get(i),i%columnasGrafico,i/columnasGrafico);
        }
        boolean telefono=ancho<600;
        if(columnaEstudiante!=null&&columnaPeriodo!=null&&!java.util.Objects.equals(ultimaVistaTelefono,telefono)) {
            ultimaVistaTelefono=telefono;
            columnaEstudiante.setVisible(!telefono||usuario.getRol()!=Rol.ESTUDIANTE);
            columnaPeriodo.setVisible(!telefono);
        }
    }
    private String rolTexto(Rol r){return switch(r){case ADMIN->"administrador";case PROFESOR->"profesor";case ESTUDIANTE->"estudiante";case PADRE->"padre/tutor";};}
}
