package com.unev.sige.model;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** PDF de los registros de las herramientas escolares (un registro o un listado). */
public final class ReportePdfHerramientas {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int ANCHO_LINEA = 70;

    private ReportePdfHerramientas() { }

    /** Parte un texto en renglones de a lo sumo {@code ancho} caracteres, respetando los saltos de línea. */
    static List<String> envolver(String texto, int ancho) {
        List<String> r = new ArrayList<>();
        for (String linea : (texto == null ? "" : texto).split("\\r?\\n", -1)) {
            String resto = linea.strip();
            if (resto.isEmpty()) {
                r.add("");
                continue;
            }
            while (resto.length() > ancho) {
                int corte = resto.lastIndexOf(' ', ancho);
                if (corte < ancho / 2) corte = ancho;
                r.add(resto.substring(0, corte).strip());
                resto = resto.substring(corte).strip();
            }
            r.add(resto);
        }
        return r;
    }

    /** PDF con todos los datos y el resultado de un registro. */
    public static byte[] registro(RegistroHerramienta r) {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[] {"DATOS INGRESADOS", ""});
        for (RegistroHerramienta.Dato d : r.datos()) {
            List<String> partes = envolver(d.valor(), ANCHO_LINEA);
            for (int i = 0; i < partes.size(); i++) filas.add(new String[] {i == 0 ? d.etiqueta() : "", partes.get(i)});
        }
        filas.add(new String[] {"", ""});
        filas.add(new String[] {"RESULTADO", ""});
        for (String linea : envolver(r.resultado(), ANCHO_LINEA)) filas.add(new String[] {"", linea});
        String subtitulo = "Registro N.º " + r.id() + " · " + (r.autor() == null ? "" : r.autor()) + " · "
                + (r.creado() == null ? "" : r.creado().format(FECHA)) + " · Solo consulta, sin valor oficial";
        return PdfTabla.generar(r.titulo(), subtitulo, List.of("Campo", "Valor"), filas);
    }

    /** PDF con el listado de registros (fecha, herramienta, autor y primera línea del resultado). */
    public static byte[] listado(String titulo, List<RegistroHerramienta> registros, boolean conAutor) {
        List<String> enc = conAutor ? List.of("Fecha", "Herramienta", "Autor", "Resultado") : List.of("Fecha", "Herramienta", "Resultado");
        List<String[]> filas = new ArrayList<>();
        for (RegistroHerramienta r : registros) {
            String fecha = r.creado() == null ? "" : r.creado().format(FECHA);
            String primera = envolver(r.resultado(), 200).get(0);
            filas.add(conAutor ? new String[] {fecha, r.titulo(), r.autor(), primera} : new String[] {fecha, r.titulo(), primera});
        }
        return PdfTabla.generar(titulo, registros.size() + " registro(s)", enc, filas);
    }
}
