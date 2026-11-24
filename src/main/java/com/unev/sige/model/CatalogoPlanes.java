package com.unev.sige.model;

import java.util.List;
import java.util.Optional;

/** Planes de servicio disponibles, de menor a mayor. Los límites informan; no bloquean el uso. */
public final class CatalogoPlanes {

    public static final String PLAN_INICIAL = "ESTANDAR";

    private static final List<PlanServicio> PLANES = List.of(
            new PlanServicio("ESENCIAL", "Esencial", "🌱", "#0f766e", 2500, 150,
                    List.of("Hasta 150 usuarios activos", "Matrícula, notas y asistencia", "Boletas y carnets",
                            "Herramientas escolares", "Soporte por correo"),
                    "Para centros pequeños que empiezan a digitalizarse."),
            new PlanServicio("ESTANDAR", "Estándar", "🚀", "#2563eb", 6500, 600,
                    List.of("Hasta 600 usuarios activos", "Todo lo de Esencial", "Aula virtual y tareas",
                            "Reportes y auditoría", "Registros en PDF", "Soporte prioritario"),
                    "El equilibrio para la mayoría de los colegios."),
            new PlanServicio("INSTITUCIONAL", "Institucional", "🏛️", "#7c3aed", 15000, 0,
                    List.of("Usuarios sin límite", "Todo lo de Estándar", "Respaldos programados",
                            "Capacitación al personal", "Soporte dedicado"),
                    "Para redes de centros y grandes instituciones."));

    private CatalogoPlanes() { }

    public static List<PlanServicio> todos() {
        return PLANES;
    }

    public static Optional<PlanServicio> buscar(String codigo) {
        if (codigo == null) return Optional.empty();
        return PLANES.stream().filter(p -> p.codigo().equalsIgnoreCase(codigo.trim())).findFirst();
    }

    /** RD$ con separador de miles: «6,500». */
    public static String precio(int monto) {
        return String.format(java.util.Locale.US, "%,d", monto);
    }

    /** Solo la administración cambia de plan; la dirección lo consulta. */
    public static boolean puedeCambiar(Rol rol) {
        return rol == Rol.ADMIN;
    }

    public static boolean puedeVer(Rol rol) {
        return rol == Rol.ADMIN || rol == Rol.DIRECTOR;
    }
}
