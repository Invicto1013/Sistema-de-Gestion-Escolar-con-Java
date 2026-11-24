package com.unev.sige.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Un cálculo de «Herramientas» guardado: quién lo hizo, con qué datos y qué resultado dio. */
public record RegistroHerramienta(int id, int idUsuario, String autor, String herramienta, String titulo,
                                  String resultado, LocalDateTime creado, List<Dato> datos) {

    public static final int MAX_VALOR = 1500;
    public static final int MAX_RESULTADO = 4000;
    /** Tope de registros guardados por usuario, para que la tabla no crezca sin control. */
    public static final int MAX_POR_USUARIO = 200;
    public static final String NO_SE_GUARDA = "(no se guarda)";

    public record Dato(String campo, String etiqueta, String valor) { }

    public static String recortar(String texto, int max) {
        if (texto == null) return "";
        String t = texto.strip();
        return t.length() <= max ? t : t.substring(0, max);
    }

    /** Datos del formulario listos para guardar: en orden, recortados y sin los campos sensibles. */
    public static List<Dato> datosAGuardar(HerramientaEscolar h, Map<String, String> valores) {
        List<Dato> r = new ArrayList<>();
        for (HerramientaEscolar.Campo c : h.campos()) {
            String valor = CatalogoHerramientas.noSeGuarda(h.id(), c.nombre())
                    ? NO_SE_GUARDA : recortar(valores.getOrDefault(c.nombre(), ""), MAX_VALOR);
            r.add(new Dato(c.nombre(), recortar(c.etiqueta(), 120), valor));
        }
        return r;
    }

    /** Ven el registro su dueño, el administrador y la dirección. */
    public boolean puedeVer(int idQuienPregunta, Rol rol) {
        return idQuienPregunta == idUsuario || rol == Rol.ADMIN || rol == Rol.DIRECTOR;
    }

    /** Lo elimina su dueño o el administrador. */
    public boolean puedeEliminar(int idQuienPregunta, Rol rol) {
        return idQuienPregunta == idUsuario || rol == Rol.ADMIN;
    }
}
