package com.unev.sige.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.unev.sige.model.Estudiante;
import com.unev.sige.model.LineaBoleta;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Generador de documentos oficiales imprimibles (Boleta Oficial de Calificaciones y Carnets con QR).
 * Diseñado con CSS @media print optimizado para guardar en PDF con formato de alta resolución.
 */
public final class GeneradorDocumentos {

    private GeneradorDocumentos() {
    }

    /**
     * Genera el HTML completo y autónomo de la Boleta Oficial de Calificaciones.
     */
    public static String generarBoletaHtml(Estudiante estudiante, List<LineaBoleta> lineas, String periodo) {
        StringBuilder filas = new StringBuilder();
        double sumaNotas = 0;
        int materiasEvaluadas = 0;

        for (LineaBoleta l : lineas) {
            if (periodo != null && !periodo.isBlank() && !periodo.equalsIgnoreCase(l.periodo())) {
                continue;
            }
            double nf = l.notaFinal();
            if (nf > 0) {
                sumaNotas += nf;
                materiasEvaluadas++;
            }
            String estadoClass = "APROBADA".equalsIgnoreCase(l.estado()) ? "aprobada" : "reprobada";
            filas.append("<tr>")
                    .append("<td>").append(escape(l.codigoAsignatura())).append("</td>")
                    .append("<td><strong>").append(escape(l.asignatura())).append("</strong></td>")
                    .append("<td class='num'>").append(String.format(Locale.ROOT, "%.2f", l.parcial1())).append("</td>")
                    .append("<td class='num'>").append(String.format(Locale.ROOT, "%.2f", l.parcial2())).append("</td>")
                    .append("<td class='num'>").append(String.format(Locale.ROOT, "%.2f", l.examenFinal())).append("</td>")
                    .append("<td class='num destacada'>").append(String.format(Locale.ROOT, "%.2f", l.notaFinal())).append("</td>")
                    .append("<td class='center'><strong>").append(escape(l.literal())).append("</strong></td>")
                    .append("<td class='center'><span class='tag ").append(estadoClass).append("'>")
                    .append(escape(l.estado())).append("</span></td>")
                    .append("</tr>");
        }

        if (filas.length() == 0) {
            filas.append("<tr><td colspan='8' class='center empty'>No hay calificaciones registradas para el período seleccionado.</td></tr>");
        }

        double promedio = materiasEvaluadas > 0 ? (sumaNotas / materiasEvaluadas) : 0;
        String fechaHoy = LocalDate.now().format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "ES")));

        return """
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <title>Boleta Oficial de Calificaciones - %s</title>
            <style>
                @page { size: letter portrait; margin: 1.5cm; }
                body {
                    font-family: 'Segoe UI', Helvetica, Arial, sans-serif;
                    color: #1e293b;
                    margin: 0;
                    padding: 20px;
                    background: #fff;
                    font-size: 13px;
                    line-height: 1.4;
                }
                .documento {
                    max-width: 800px;
                    margin: 0 auto;
                    border: 1px solid #cbd5e1;
                    padding: 32px;
                    border-radius: 8px;
                    background: #ffffff;
                }
                .header {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    border-bottom: 2px solid #0f766e;
                    padding-bottom: 16px;
                    margin-bottom: 20px;
                }
                .header-title h1 {
                    margin: 0;
                    font-size: 22px;
                    color: #0f766e;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }
                .header-title h2 {
                    margin: 4px 0 0;
                    font-size: 14px;
                    color: #475569;
                    font-weight: 500;
                }
                .info-grid {
                    display: grid;
                    grid-template-columns: repeat(2, 1fr);
                    gap: 10px 24px;
                    background: #f8fafc;
                    padding: 16px;
                    border-radius: 6px;
                    margin-bottom: 24px;
                    border: 1px solid #e2e8f0;
                }
                .info-item { display: flex; justify-content: space-between; }
                .info-label { color: #64748b; font-weight: 600; font-size: 12px; text-transform: uppercase; }
                .info-value { font-weight: 600; color: #0f172a; }
                
                table {
                    width: 100%%;
                    border-collapse: collapse;
                    margin-bottom: 24px;
                }
                th {
                    background: #0f766e;
                    color: #ffffff;
                    text-align: left;
                    padding: 8px 10px;
                    font-size: 11px;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }
                td {
                    padding: 8px 10px;
                    border-bottom: 1px solid #e2e8f0;
                }
                tr:nth-child(even) td { background: #f8fafc; }
                .num { text-align: right; }
                .center { text-align: center; }
                .destacada { font-weight: bold; color: #0f766e; }
                .tag {
                    display: inline-block;
                    padding: 2px 6px;
                    border-radius: 4px;
                    font-size: 10px;
                    font-weight: bold;
                }
                .tag.aprobada { background: #dcfce7; color: #15803d; }
                .tag.reprobada { background: #fee2e2; color: #b91c1c; }
                .empty { color: #94a3b8; font-style: italic; padding: 24px; }
                
                .resumen-box {
                    display: flex;
                    justify-content: flex-end;
                    gap: 20px;
                    margin-bottom: 40px;
                }
                .resumen-card {
                    background: #f0fdfa;
                    border: 1px solid #ccfbf1;
                    padding: 10px 20px;
                    border-radius: 6px;
                    text-align: right;
                }
                .resumen-card .label { font-size: 11px; color: #0f766e; text-transform: uppercase; font-weight: bold; }
                .resumen-card .valor { font-size: 20px; color: #0f766e; font-weight: bold; margin-top: 2px; }
                
                .firmas {
                    display: flex;
                    justify-content: space-around;
                    margin-top: 60px;
                    padding-top: 20px;
                }
                .firma-line {
                    border-top: 1px solid #94a3b8;
                    width: 220px;
                    text-align: center;
                    padding-top: 6px;
                    font-size: 12px;
                    color: #475569;
                    font-weight: 500;
                }
                .btn-imprimir {
                    display: block;
                    width: 100%%;
                    max-width: 200px;
                    margin: 0 auto 20px;
                    padding: 10px 16px;
                    background: #0f766e;
                    color: white;
                    border: none;
                    border-radius: 6px;
                    font-size: 14px;
                    font-weight: bold;
                    cursor: pointer;
                    text-align: center;
                    text-decoration: none;
                }
                .btn-imprimir:hover { background: #115e59; }
                @media print {
                    .no-print { display: none !important; }
                    body { padding: 0; background: white; }
                    .documento { border: none; padding: 0; }
                }
            </style>
        </head>
        <body>
            <div class="no-print" style="text-align: center;">
                <button class="btn-imprimir" onclick="window.print()">🖨 Imprimir / Guardar PDF</button>
            </div>
            <div class="documento">
                <div class="header">
                    <div class="header-title">
                        <h1>Sistema de Gestión Educativa (SIGE)</h1>
                        <h2>Boleta Oficial de Calificaciones Académicas</h2>
                    </div>
                    <div style="text-align: right; font-size: 11px; color: #64748b;">
                        Fecha de expedición:<br><strong>%s</strong>
                    </div>
                </div>
                
                <div class="info-grid">
                    <div class="info-item"><span class="info-label">Estudiante:</span><span class="info-value">%s</span></div>
                    <div class="info-item"><span class="info-label">Matrícula:</span><span class="info-value">%s</span></div>
                    <div class="info-item"><span class="info-label">Grado Académico:</span><span class="info-value">%s</span></div>
                    <div class="info-item"><span class="info-label">Período Lectivo:</span><span class="info-value">%s</span></div>
                </div>
                
                <table>
                    <thead>
                        <tr>
                            <th style="width: 12%%;">Código</th>
                            <th>Asignatura</th>
                            <th class="num" style="width: 10%%;">P1 (30%%)</th>
                            <th class="num" style="width: 10%%;">P2 (30%%)</th>
                            <th class="num" style="width: 10%%;">Examen (40%%)</th>
                            <th class="num" style="width: 12%%;">Nota Final</th>
                            <th class="center" style="width: 8%%;">Literal</th>
                            <th class="center" style="width: 14%%;">Estado</th>
                        </tr>
                    </thead>
                    <tbody>
                        %s
                    </tbody>
                </table>
                
                <div class="resumen-box">
                    <div class="resumen-card">
                        <div class="label">Materias Evaluadas</div>
                        <div class="valor">%d</div>
                    </div>
                    <div class="resumen-card">
                        <div class="label">Promedio Ponderado</div>
                        <div class="valor">%.2f</div>
                    </div>
                </div>
                
                <div class="firmas">
                    <div class="firma-line">Dirección Académica</div>
                    <div class="firma-line">Secretaría Docente</div>
                </div>
            </div>
        </body>
        </html>
        """.formatted(
                escape(estudiante.getNombreCompleto()),
                fechaHoy,
                escape(estudiante.getNombreCompleto()),
                escape(estudiante.getMatricula() != null ? estudiante.getMatricula() : "S/M"),
                escape(estudiante.getGrado() != null ? estudiante.getGrado() : "Sin grado"),
                escape(periodo != null && !periodo.isBlank() ? periodo : "General acumulado"),
                filas.toString(),
                materiasEvaluadas,
                promedio
        );
    }

    /**
     * Genera el carnet escolar imprimible en alta calidad con código QR embebido.
     */
    public static String generarCarnetHtml(Estudiante estudiante) {
        String qrBase64 = generarQrBase64("SIGE:" + estudiante.getMatricula() + ":" + estudiante.getId());
        String fechaHoy = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));

        return """
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <title>Carnet Estudiantil - %s</title>
            <style>
                @page { size: auto; margin: 1cm; }
                body {
                    font-family: 'Segoe UI', Helvetica, Arial, sans-serif;
                    background: #f1f5f9;
                    margin: 0;
                    padding: 30px;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                }
                .carnet-wrap {
                    display: flex;
                    gap: 24px;
                    flex-wrap: wrap;
                    justify-content: center;
                }
                .carnet {
                    width: 340px;
                    height: 215px;
                    background: linear-gradient(135deg, #0f766e 0%%, #134e4a 100%%);
                    border-radius: 12px;
                    box-shadow: 0 10px 25px rgba(0,0,0,0.15);
                    color: white;
                    padding: 16px 20px;
                    box-sizing: border-box;
                    display: flex;
                    flex-direction: column;
                    justify-content: space-between;
                    position: relative;
                    overflow: hidden;
                }
                .carnet::after {
                    content: "";
                    position: absolute;
                    width: 180px;
                    height: 180px;
                    background: rgba(255,255,255,0.06);
                    border-radius: 50%%;
                    right: -40px;
                    bottom: -40px;
                }
                .carnet-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    border-bottom: 1px solid rgba(255,255,255,0.25);
                    padding-bottom: 8px;
                }
                .carnet-header h1 {
                    margin: 0;
                    font-size: 15px;
                    letter-spacing: 1px;
                    text-transform: uppercase;
                }
                .carnet-header span {
                    font-size: 10px;
                    background: #ef6530;
                    padding: 2px 8px;
                    border-radius: 10px;
                    font-weight: bold;
                }
                .carnet-body {
                    display: flex;
                    gap: 16px;
                    align-items: center;
                }
                .avatar {
                    width: 70px;
                    height: 70px;
                    background: #ffffff;
                    color: #0f766e;
                    border-radius: 8px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 28px;
                    font-weight: bold;
                    border: 2px solid rgba(255,255,255,0.8);
                }
                .datos { flex: 1; }
                .datos .nombre { font-size: 15px; font-weight: bold; margin-bottom: 4px; line-height: 1.2; }
                .datos .matricula { font-size: 12px; color: #ccfbf1; font-weight: 600; }
                .datos .grado { font-size: 11px; color: #e2e8f0; margin-top: 4px; }
                .carnet-footer {
                    display: flex;
                    justify-content: space-between;
                    align-items: flex-end;
                    font-size: 9px;
                    color: #cbd5e1;
                }
                .qr-img {
                    width: 52px;
                    height: 52px;
                    background: white;
                    padding: 2px;
                    border-radius: 4px;
                }
                .btn-imprimir {
                    margin-bottom: 24px;
                    padding: 10px 20px;
                    background: #0f766e;
                    color: white;
                    border: none;
                    border-radius: 6px;
                    font-size: 14px;
                    font-weight: bold;
                    cursor: pointer;
                }
                @media print {
                    .no-print { display: none !important; }
                    body { background: white; padding: 0; }
                    .carnet { box-shadow: none; border: 1px solid #94a3b8; }
                }
            </style>
        </head>
        <body>
            <button class="no-print btn-imprimir" onclick="window.print()">🖨 Imprimir Carnet</button>
            <div class="carnet-wrap">
                <div class="carnet">
                    <div class="carnet-header">
                        <h1>SIGE Escolar</h1>
                        <span>ESTUDIANTE</span>
                    </div>
                    <div class="carnet-body">
                        <div class="avatar">%s</div>
                        <div class="datos">
                            <div class="nombre">%s</div>
                            <div class="matricula">Matrícula: %s</div>
                            <div class="grado">%s</div>
                        </div>
                    </div>
                    <div class="carnet-footer">
                        <div>Válido ciclo %s<br>República Dominicana</div>
                        <img class="qr-img" src="%s" alt="QR Matrícula">
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.formatted(
                escape(estudiante.getNombreCompleto()),
                obtenerIniciales(estudiante.getNombreCompleto()),
                escape(estudiante.getNombreCompleto()),
                escape(estudiante.getMatricula() != null ? estudiante.getMatricula() : "S/M"),
                escape(estudiante.getGrado() != null ? estudiante.getGrado() : "Primaria / Secundaria"),
                fechaHoy,
                qrBase64
        );
    }

    private static String generarQrBase64(String texto) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(texto, BarcodeFormat.QR_CODE, 160, 160);
            StringBuilder path = new StringBuilder();
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) {
                    if (matrix.get(x, y)) {
                        path.append('M').append(x).append(',').append(y).append("h1v1h-1z");
                    }
                }
            }
            String svg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 " + matrix.getWidth() + " " + matrix.getHeight() + "'>"
                    + "<rect width='100%' height='100%' fill='white'/>"
                    + "<path d='" + path + "' fill='#0f172a'/>"
                    + "</svg>";
            return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return "";
        }
    }

    private static String obtenerIniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) return "E";
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length == 1) return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        return ("" + partes[0].charAt(0) + partes[1].charAt(0)).toUpperCase();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
