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

Requisitos: JDK 21+, Maven 3.9+ y una base MySQL 8 (la de la nube o una local).

### 1. Configurar la conexión
Copia `config.example.properties` como `config.properties` y escribe el servidor, la base, el
usuario, la contraseña y la clave 2FA. Ese archivo está en `.gitignore`: nunca se sube al
repositorio. La base se crea importando `sql/sige_completo.sql` (estructura, roles, usuarios de prueba y datos de ejemplo; las credenciales de prueba están al inicio del script).

### 2. Desde el código fuente
```bash
mvn clean javafx:run
```

### 3. Ejecutable distribuible (Fase 9)
```bash
mvn clean package          # genera target/sige-1.0.0.jar (incluye JavaFX y las librerías)
java -jar target/sige-1.0.0.jar
```
El `config.properties` debe estar en la misma carpeta que el `.jar` (o en la carpeta desde donde
se ejecuta). El `.jar` ejecutable usa la clase `Launcher`; no incluye credenciales.

> El `.jar` contiene las librerías de JavaFX del sistema operativo donde se compila
> (Windows → ejecuta en Windows). Compílalo en el sistema donde se va a usar.

Para crear la aplicación portable de Windows (carpeta con `SIGE.exe`, no requiere tener Java
instalado) o el instalador:

```bat
empaquetar.bat        :: JAR + app portable en target\dist\SIGE\
empaquetar.bat exe    :: ademas, instalador .exe (requiere WiX Toolset)
```
El script copia `config.properties` dentro de la aplicación empaquetada para que se conecte a la
base en la nube sin pasos adicionales.

## Autor

Elian Castro - Programación II - UNEV
