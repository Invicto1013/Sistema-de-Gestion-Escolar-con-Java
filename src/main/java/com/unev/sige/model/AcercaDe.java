package com.unev.sige.model;

import java.util.List;

/** Información del proyecto que muestra el icono «?» (Acerca de), compartida por la web y el ejecutable. */
public final class AcercaDe {
    public static final String NOMBRE = "SIGE";
    public static final String NOMBRE_COMPLETO = "Sistema de Gestión Educativa";
    public static final String VERSION = "1.3.0";
    public static final String AUTOR = "Elian Castro";
    public static final String INSTITUCION = "Universidad Nacional Evangélica (UNEV)";
    public static final String REPOSITORIO = "https://github.com/Invicto1013/Sistema-de-Gestion-Escolar-con-Java";

    /** Un bloque de información: título y líneas de texto. */
    public record Seccion(String titulo, List<String> lineas) { }

    private AcercaDe() { }

    public static String resumen() {
        return "Aplicación de gestión escolar para primaria y secundaria: matrícula, inscripciones, calificaciones, asistencia, "
                + "boletas, horarios, anuncios y seguimiento de conducta, con un ejecutable de escritorio y un módulo web.";
    }

    public static List<Seccion> secciones() {
        return List.of(
            new Seccion("Qué incluye", List.of(
                "Aplicación de escritorio (Windows) con JavaFX.",
                "Módulo web adaptable a móvil y tableta, con instalación como aplicación (PWA).",
                "Aula virtual: tareas y archivos, conectada con SIGE sin volver a iniciar sesión.")),
            new Seccion("Roles", List.of(
                "Administrador, director/a, secretario/a, profesor, estudiante y padre/tutor.",
                "Cada cargo ve solo las funciones que le corresponden.")),
            new Seccion("Funciones principales", List.of(
                "Períodos académicos, grados y currículo, secciones e inscripciones.",
                "Calificaciones, asistencia, boleta oficial y carnet con código QR.",
                "Horarios, anuncios y circulares, cuaderno de conducta y méritos.",
                "Reportes en CSV, Excel y PDF, y auditoría de acciones.")),
            new Seccion("Seguridad", List.of(
                "Contraseñas cifradas, política de contraseña y bloqueo por intentos fallidos.",
                "Verificación en dos pasos, protección CSRF y sesiones con caducidad.",
                "Historial de sesiones y registro de auditoría.")),
            new Seccion("Accesibilidad y apariencia", List.of(
                "Modo claro y oscuro, y modos de visión para daltonismo y alto contraste.",
                "Estaciones del año automáticas y animaciones que se pueden desactivar.")),
            new Seccion("Tecnologías", List.of(
                "Java 21, JavaFX, servidor web propio, MySQL, Maven y GitHub Actions.")),
            new Seccion("Créditos y licencias", List.of(
                "Autor: " + AUTOR + ".",
                "Institución: " + INSTITUCION + ".",
                "Proyecto académico. Se desarrolló con apoyo de una herramienta de IA (Claude) para código y documentación.",
                "Las licencias de las dependencias y la verificación de originalidad están en docs/CREDITOS_Y_ORIGINALIDAD.md.")));
    }
}
