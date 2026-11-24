package com.unev.sige.controller;

import com.unev.sige.app.SesionActual;
import com.unev.sige.data.SuscripcionDAO;
import com.unev.sige.model.CatalogoPlanes;
import com.unev.sige.model.PlanServicio;
import com.unev.sige.model.Rol;
import com.unev.sige.model.SuscripcionCentro;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.sql.SQLException;

/** Planes de servicio del centro: plan vigente, uso de usuarios y cambio de plan (solo administración). */
public final class PlanesPanel extends VBox {

    private final Rol rol;
    private final VBox contenido = new VBox(12);

    public PlanesPanel(Rol rol) {
        this.rol = rol;
        setSpacing(12);
        ScrollPane desplazable = new ScrollPane(contenido);
        desplazable.setFitToWidth(true);
        VBox.setVgrow(desplazable, javafx.scene.layout.Priority.ALWAYS);
        getChildren().addAll(Dialogos.titulo("💳 Planes de servicio"),
                new Label("Los límites son informativos: no bloquean a nadie, avisan cuándo conviene subir de plan."), desplazable);
        recargar(null);
    }

    private void recargar(String aviso) {
        contenido.getChildren().clear();
        SuscripcionCentro actual = null;
        int usuarios = -1;
        String mensaje = aviso;
        try {
            SuscripcionDAO dao = new SuscripcionDAO();
            actual = dao.actual().orElse(null);
            usuarios = dao.usuariosActivos();
        } catch (SQLException e) {
            mensaje = SuscripcionDAO.tablaFaltante(e)
                    ? "El plan aún no está activado: ejecuta sql/fase_planes_servicio.sql en la base de datos."
                    : "No se pudo consultar el plan: " + e.getMessage();
        }
        if (mensaje != null) {
            Label l = new Label(mensaje);
            l.setWrapText(true);
            l.getStyleClass().add("plan-aviso-fx");
            contenido.getChildren().add(l);
        }
        PlanServicio vigente = actual == null ? null : CatalogoPlanes.buscar(actual.codigoPlan()).orElse(null);
        if (vigente != null) contenido.getChildren().add(resumenActual(vigente, actual, usuarios));
        FlowPane tarjetas = new FlowPane(14, 14);
        int i = 0;
        for (PlanServicio p : CatalogoPlanes.todos()) tarjetas.getChildren().add(tarjeta(p, vigente != null && vigente.codigo().equals(p.codigo()), i++));
        contenido.getChildren().add(tarjetas);
        if (!CatalogoPlanes.puedeCambiar(rol)) contenido.getChildren().add(new Label("Solo la administración puede cambiar el plan."));
    }

    private VBox resumenActual(PlanServicio p, SuscripcionCentro s, int usuarios) {
        VBox caja = new VBox(6);
        caja.getStyleClass().add("plan-actual-fx");
        caja.setStyle("-fx-border-color: " + p.acento() + "; -fx-border-width: 0 0 0 5;");
        Label titulo = new Label(p.icono() + "  Plan actual: " + p.nombre());
        titulo.getStyleClass().add("plan-titulo-fx");
        caja.getChildren().addAll(titulo, new Label("Desde " + s.desde() + " · registrado por " + s.cambiadoPor()));
        if (usuarios >= 0) {
            caja.getChildren().add(new Label(usuarios + " usuarios activos " + (p.ilimitado() ? "(sin límite)" : "de " + p.maxUsuarios()) + " · " + p.estadoUso(usuarios)));
            if (!p.ilimitado()) {
                ProgressBar barra = new ProgressBar(Math.min(1.0, usuarios / (double) p.maxUsuarios()));
                barra.setMaxWidth(Double.MAX_VALUE);
                barra.setStyle("-fx-accent: " + (usuarios > p.maxUsuarios() ? "#dc2626" : p.porcentajeUso(usuarios) >= 90 ? "#d97706" : p.acento()) + ";");
                caja.getChildren().add(barra);
            }
        }
        return caja;
    }

    private VBox tarjeta(PlanServicio p, boolean esActual, int orden) {
        VBox t = new VBox(6);
        t.setPrefWidth(250);
        t.setPadding(new Insets(14));
        t.getStyleClass().add("plan-tarjeta-fx");
        t.setStyle("-fx-border-color: " + p.acento() + "; -fx-border-width: " + (esActual ? "3" : "0 0 0 0") + "; -fx-border-radius: 10;");
        Label icono = new Label(p.icono());
        icono.setStyle("-fx-font-size: 28px;");
        Label nombre = new Label(p.nombre() + (esActual ? "  ✔ actual" : ""));
        nombre.getStyleClass().add("plan-titulo-fx");
        Label resumen = new Label(p.resumen());
        resumen.setWrapText(true);
        Label precio = new Label("RD$ " + CatalogoPlanes.precio(p.precioMensual()) + " / mes");
        precio.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + p.acento() + ";");
        t.getChildren().addAll(icono, nombre, resumen, precio);
        for (String linea : p.incluye()) {
            Label l = new Label("• " + linea);
            l.setWrapText(true);
            t.getChildren().add(l);
        }
        if (CatalogoPlanes.puedeCambiar(rol) && !esActual) {
            Button b = new Button("Cambiar a " + p.nombre());
            b.getStyleClass().add("primary");
            b.setOnAction(e -> cambiar(p));
            t.getChildren().add(b);
        }
        t.setOpacity(0);
        t.setTranslateY(12);
        FadeTransition f = new FadeTransition(Duration.millis(320), t);
        f.setToValue(1);
        f.setDelay(Duration.millis(90L * orden));
        TranslateTransition s = new TranslateTransition(Duration.millis(320), t);
        s.setToY(0);
        s.setDelay(Duration.millis(90L * orden));
        f.play();
        s.play();
        return t;
    }

    private void cambiar(PlanServicio p) {
        if (SesionActual.getUsuario() == null) {
            Dialogos.error("Inicia sesión para cambiar el plan.");
            return;
        }
        if (!Dialogos.confirmar("Cambiar de plan", "¿Cambiar el plan del centro a «" + p.nombre() + "» (RD$ " + CatalogoPlanes.precio(p.precioMensual()) + " / mes)?")) return;
        try {
            new SuscripcionDAO().cambiar(p.codigo(), SesionActual.getUsuario().getId());
            recargar("Plan actualizado a " + p.nombre() + ".");
        } catch (SQLException ex) {
            Dialogos.error(SuscripcionDAO.tablaFaltante(ex)
                    ? "El plan aún no está activado: ejecuta sql/fase_planes_servicio.sql en la base de datos."
                    : "No se pudo cambiar el plan: " + ex.getMessage());
        }
    }
}
