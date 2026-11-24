package com.unev.sige.web;

import com.unev.sige.model.CatalogoPlanes;
import com.unev.sige.model.PlanServicio;
import com.unev.sige.model.Rol;
import com.unev.sige.model.SuscripcionCentro;

/** HTML de «Planes de servicio». Sin estado: se prueba sin servidor ni base de datos. */
final class PlanesWeb {

    private PlanesWeb() { }

    /**
     * @param actual     suscripción vigente, o null si aún no hay tablas/filas
     * @param usuarios   usuarios activos, o -1 si no se pudo contar
     * @param aviso      mensaje opcional (ya escapado por quien lo arma o texto plano; aquí se escapa)
     */
    static String pagina(Rol rol, SuscripcionCentro actual, int usuarios, String csrf, String aviso) {
        PlanServicio vigente = actual == null ? null : CatalogoPlanes.buscar(actual.codigoPlan()).orElse(null);
        boolean cambia = CatalogoPlanes.puedeCambiar(rol);
        StringBuilder b = new StringBuilder(4096);
        b.append("<section class='card plan-hero'><div class='eyebrow'>SUSCRIPCIÓN</div><h1>💳 Planes de servicio</h1>")
                .append("<p>Elige el plan que mejor se ajusta al tamaño del centro. Los límites son informativos: ")
                .append("no bloquean a nadie, avisan cuándo conviene subir de plan.</p></section>");
        if (aviso != null && !aviso.isEmpty()) b.append("<section class='card plan-aviso' role='status'>").append(HerramientasWeb.esc(aviso)).append("</section>");
        if (vigente != null) b.append(actual(vigente, actual, usuarios));
        else b.append("<section class='card plan-aviso' role='status'>Aún no hay un plan registrado. Ejecuta sql/fase_planes_servicio.sql en la base de datos para activarlo.</section>");
        b.append("<div class='plan-rejilla'>");
        int i = 0;
        for (PlanServicio p : CatalogoPlanes.todos()) {
            boolean esActual = vigente != null && vigente.codigo().equals(p.codigo());
            b.append("<article class='card plan-tarjeta").append(esActual ? " plan-actual" : "").append("' style='--acento:").append(p.acento())
                    .append(";--i:").append(i++).append("'>");
            if (esActual) b.append("<span class='plan-etiqueta'>Plan actual</span>");
            b.append("<div class='plan-icono' aria-hidden='true'>").append(p.icono()).append("</div><h2>").append(HerramientasWeb.esc(p.nombre())).append("</h2>")
                    .append("<p class='plan-resumen'>").append(HerramientasWeb.esc(p.resumen())).append("</p>")
                    .append("<p class='plan-precio'><span>RD$</span> ").append(CatalogoPlanes.precio(p.precioMensual())).append("<small> / mes</small></p><ul>");
            for (String linea : p.incluye()) b.append("<li>").append(HerramientasWeb.esc(linea)).append("</li>");
            b.append("</ul>");
            if (cambia && !esActual) {
                b.append("<form method='post' action='/planes'><input type='hidden' name='_csrf' value='").append(HerramientasWeb.esc(csrf))
                        .append("'><input type='hidden' name='plan' value='").append(p.codigo()).append("'><button class='primary' type='submit'>Cambiar a ")
                        .append(HerramientasWeb.esc(p.nombre())).append("</button></form>");
            }
            b.append("</article>");
        }
        b.append("</div>");
        if (!cambia) b.append("<p class='plan-nota'>Solo la administración puede cambiar el plan.</p>");
        return b.toString();
    }

    private static String actual(PlanServicio p, SuscripcionCentro s, int usuarios) {
        StringBuilder b = new StringBuilder();
        b.append("<section class='card plan-uso' style='--acento:").append(p.acento()).append("'><h2>")
                .append(p.icono()).append(" Plan actual: ").append(HerramientasWeb.esc(p.nombre())).append("</h2>")
                .append("<p class='plan-meta'>Desde ").append(HerramientasWeb.esc(s.desde())).append(" · registrado por ").append(HerramientasWeb.esc(s.cambiadoPor())).append("</p>");
        if (usuarios >= 0) {
            int pct = Math.min(100, p.porcentajeUso(usuarios));
            String limite = p.ilimitado() ? "sin límite" : "de " + p.maxUsuarios();
            b.append("<p><strong>").append(usuarios).append("</strong> usuarios activos ").append(limite).append(" · ")
                    .append(HerramientasWeb.esc(p.estadoUso(usuarios))).append("</p>");
            if (!p.ilimitado()) {
                String clase = usuarios > p.maxUsuarios() ? " plan-exceso" : pct >= 90 ? " plan-alerta" : "";
                b.append("<div class='plan-barra").append(clase).append("' role='progressbar' aria-valuemin='0' aria-valuemax='100' aria-valuenow='")
                        .append(pct).append("'><span style='width:").append(pct).append("%'></span></div>");
            }
        }
        return b.append("</section>").toString();
    }
}
