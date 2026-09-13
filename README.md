# SIGE - Sistema Integral de Gestión Escolar

Proyecto de la Tarea No. 1 de Programación II (UNEV). Sistema de gestión escolar
para colegios/liceos de primaria y secundaria, desarrollado en Java con JavaFX.

## Problema y solución

Ver el documento `Descripcion_Sistema_SIGE.docx` para la descripción completa
del problema, la solución propuesta, los usuarios y las funciones principales.

## Estructura del proyecto

```
src/main/java/com/unev/sige/
├── app/            → Clase principal (App.java)
├── controller/      → Controladores JavaFX (Login, Registro, etc.)
└── model/           → Clases del modelo (Usuario, Estudiante, Profesor,
                        Asignatura, Calificacion, Asistencia)
src/main/resources/com/unev/sige/view/  → Archivos FXML (Scene Builder)
```

## Clases principales

| Clase | Descripción |
|---|---|
| Usuario | Clase base con autenticación (login) |
| Estudiante | Hereda de Usuario, se matricula en asignaturas |
| Profesor | Hereda de Usuario, imparte asignaturas |
| Asignatura | Materia del plan de estudios |
| Calificacion | Nota de un estudiante en una asignatura |
| Asistencia | Registro de asistencia diaria |

## Cómo ejecutar

Requisitos: JDK 21+, Maven 3.9+.

```bash
mvn clean javafx:run
```

## Autor

Genesis Castro - Programación II - UNEV
