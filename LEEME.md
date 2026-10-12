# SIGE - Cambios aplicados

## Pasos para integrarlo en tu proyecto

1. **Base de datos:** ejecuta `sql/asistencias.sql` en MySQL Workbench (crea la tabla `asistencias`).
2. **Dependencias en `pom.xml`** (dentro de `<dependencies>`; si ya tienes el driver de MySQL, agrega solo jbcrypt):

   ```xml
   <dependency>
       <groupId>com.mysql</groupId>
       <artifactId>mysql-connector-j</artifactId>
       <version>8.4.0</version>
   </dependency>
   <dependency>
       <groupId>org.mindrot</groupId>
       <artifactId>jbcrypt</artifactId>
       <version>0.4</version>
   </dependency>
   ```

   Si tu proyecto tiene `module-info.java`, agrega `requires java.sql;` y `requires jbcrypt;`
   (y verifica que tenga `opens com.unev.sige.controller to javafx.fxml;`).
3. **Copia** las carpetas `src/main/java/...` y `src/main/resources/...` sobre las de tu proyecto.
4. **Borra** los archivos que ya no se usan:
   - `data/UsuarioRepositorio.java` (reemplazado por `UsuarioDAO`)
   - `model/TipoCalificacion.java` (la nota ahora tiene 4 campos)
   - `application.properties` (era de Spring y nada lo leia)
5. **Credenciales:** copia `db.properties.example` como `db.properties` en la raiz del proyecto
   y escribe tu usuario/clave de MySQL. Ese archivo ya esta en `.gitignore`.
   Prueba con `PruebaConexion`.
6. **Crea el administrador** (una sola vez) ejecutando `CrearAdmin` con los argumentos:
   `Elian Castro admin@sige.edu.do TuClaveSegura`
7. Ejecuta `App` e inicia sesion con ese correo.

## Que cambio

- **Login por correo** (`login.fxml`, `LoginController`). Se quito el campo "Usuario".
- **Registro** (`registro.fxml`, `RegistroController`): guarda en MySQL, hashea la clave con BCrypt y
  pide datos segun el rol (matricula y grado / codigo y especialidad / telefono y ocupacion).
  "Cancelar" ahora vuelve al login en vez de cerrar la aplicacion.
- **Modelo alineado a la BD:** `Usuario` (nombre, apellido, correo, hash), `Estudiante`, `Profesor`,
  `Padre` (rol fijo PADRE), `Asignatura`, y nuevas `Seccion`, `Inscripcion`, `DetalleInscripcion`.
  `Calificacion` ahora tiene parcial1, parcial2, examen final, nota final y literal (validadas de 0 a 100).
  `Asistencia` apunta a `detalle_inscripciones`.
- **Datos:** `ConexionBD` (credenciales fuera del codigo), `UsuarioDAO` (JDBC, transaccion en el registro),
  `CrearAdmin`, `PruebaConexion` actualizado.
- Nuevo `app/Navegacion` para cambiar de pantalla.
- Sin cambios: `App`, `Rol`, `ControladorSesion`, `ReporteAcademico`.

## Fase 2: panel principal por rol

- Al iniciar sesion se abre `dashboard.fxml` (`DashboardController`) con un menu segun el rol:
  - **Todos:** "Mi perfil" (nombre, correo, rol y los datos propios: matricula/grado/promedio,
    codigo/especialidad o telefono/ocupacion).
  - **Administrador:** "Usuarios" (tabla con todos los usuarios registrados).
  - Los modulos que aun no existen (asignaturas, secciones, calificaciones, asistencia...) aparecen
    deshabilitados con "(pronto)".
- **Sesiones:** cada login se guarda en `controlador_sesiones` (`SesionDAO`) y se cierra con
  "Cerrar sesion" o al cerrar la ventana (`App.stop`). Si la BD falla al registrar la sesion, el acceso no se bloquea.
- `SesionActual` guarda quien esta autenticado para que las pantallas lo lean.
- No hay tablas nuevas en esta fase. Copia los archivos nuevos/modificados sobre tu proyecto:
  nuevos `SesionActual`, `SesionDAO`, `DashboardController`, `dashboard.fxml`;
  modificados `App`, `LoginController`, `UsuarioDAO`.

## Fase 3: asignaturas y secciones

- **Administrador > Asignaturas:** tabla con nueva / editar / eliminar (`AsignaturasPanel`).
  Valida largo de campos, creditos de 0 a 99 y codigo unico. No deja eliminar una asignatura que tiene secciones.
- **Administrador > Secciones:** crear / editar / eliminar secciones eligiendo asignatura, profesor
  (o "Sin profesor"), aula, horario y cupo (`SeccionesPanel`). La tabla muestra "Inscritos / Cupo".
  No deja bajar el cupo por debajo de los inscritos ni eliminar una seccion con estudiantes inscritos.
- **Profesor > Mis secciones:** lista de solo lectura de las secciones que tiene asignadas (`MisSeccionesPanel`).
- Los paneles estan construidos en codigo (no hay FXML nuevos) y usan `Dialogos` para alertas y confirmaciones.
- Datos: nuevos `AsignaturaDAO` y `SeccionDAO`; `UsuarioDAO.listarProfesores()`; `Seccion` ahora tiene `inscritos`.
- Esto no crea tablas nuevas. Copia sobre tu proyecto los archivos nuevos y modificados:
  nuevos `AsignaturaDAO`, `SeccionDAO`, `AsignaturasPanel`, `SeccionesPanel`, `MisSeccionesPanel`, `Dialogos`;
  modificados `Seccion`, `UsuarioDAO`, `DashboardController`.
- Orden recomendado para probar: crear una asignatura > registrar un profesor (pantalla de registro) >
  crear una seccion con ese profesor > iniciar sesion como el profesor y ver "Mis secciones".

## Fase 4: inscripcion de estudiantes en secciones

- **Administrador > Inscripciones:** se elige un estudiante (lista "Apellido, Nombre (matricula)"), la tabla muestra
  sus materias de todos los periodos y hay dos botones: "Inscribir en seccion" y "Retirar materia"
  (`InscripcionesPanel`). Al inscribir se elige una seccion con cupo y el periodo lectivo (por defecto "<año>-1",
  editable; el formato lo defines tu: "2026-1", "2026-A"...).
- **Estudiante > Mis materias:** sus materias con periodo, creditos, profesor, aula y horario (`MisMateriasPanel`).
- **Reglas** (en `InscripcionDAO.inscribir`, dentro de una transaccion): la seccion debe tener cupo; el estudiante
  no puede repetir la misma asignatura (en cualquier seccion) dentro del mismo periodo; se reutiliza la inscripcion
  activa del periodo o se crea una nueva; y se deja lista la fila de `calificaciones` (notas en 0) para la fase siguiente.
- **Retirar** borra la materia y, en cascada, su calificacion (la pantalla lo avisa antes de confirmar).
- Con esto el contador "Inscritos / Cupo" de Secciones ya cuenta de verdad, y esas secciones no se pueden eliminar.
- No hay tablas nuevas. Copia sobre tu proyecto:
  nuevos `InscripcionDAO`, `InscripcionInvalidaException`, `InscripcionesPanel`, `MisMateriasPanel`, `FilaMateria`;
  modificados `UsuarioDAO` (agrega `listarEstudiantes`) y `DashboardController`.
- Para probar: registra un estudiante (matricula y grado) > entra como admin > Inscripciones > elige al estudiante >
  inscribelo en una seccion > cierra sesion y entra como el estudiante > "Mis materias".

## Fase 5: calificaciones

- **Profesor > Calificaciones:** elige una de sus secciones y ve a sus estudiantes con parcial 1, parcial 2,
  examen final, nota final, letra y estado. Con "Editar notas" (o doble clic) captura las tres notas; un campo
  vacio cuenta como 0 y el dialogo muestra la nota final y la letra en vivo (`CalificacionesPanel`).
- **Estudiante > Mis calificaciones:** sus notas por materia y su promedio (`MisCalificacionesPanel`).
- **Las reglas estan en un solo archivo: `model/EscalaCalificaciones.java`.** Valores que puse por defecto
  (SON SUPUESTOS, ajustalos a tu institucion):
  - nota final = 30% parcial 1 + 30% parcial 2 + 40% examen final;
  - se aprueba con 70 o mas;
  - letras: A 90-100, B 80-89, C 70-79, D 60-69, F menos de 60.
- **Al guardar** (`CalificacionDAO.guardar`, en una transaccion):
  - solo el profesor de esa seccion puede calificar (si no, se rechaza);
  - la nota final y la letra se recalculan en el servidor, no se confia en lo que llegue de la pantalla;
  - el estado de la materia pasa a "Aprobada" o "Reprobada" cuando ya hay examen final (mientras no, sigue "Inscrita");
  - `estudiantes.promedio_academico` se actualiza con el promedio de sus materias ya evaluadas
    (aparece en "Mi perfil" del estudiante).
- Nota: si pones el examen final en 0 la materia vuelve a "Inscrita" (en curso).
- No hay tablas nuevas. Copia sobre tu proyecto (ademas de las fases 3 y 4):
  nuevos `EscalaCalificaciones`, `NotaEstudiante`, `CalificacionDAO`, `PermisoDenegadoException`,
  `CalificacionesPanel`, `MisCalificacionesPanel`;
  modificados `Calificacion`, `InscripcionDAO`, `Dialogos`, `DashboardController`.
- Para probar: entra como profesor (con una seccion que tenga un estudiante inscrito) > Calificaciones >
  doble clic en el estudiante > pon 80, 90 y 100 (debe dar 91, A) > entra como el estudiante y mira
  "Mis calificaciones" y "Mi perfil".

## Pendiente (siguientes pasos)

- Vincular padres con estudiantes (tabla `estudiante_padre`).
- Asistencia (el profesor la toma por seccion y dia; hay que crear la tabla con `sql/asistencias.sql`).
- Reportes academicos (boleta de calificaciones) y que el administrador pueda ver/corregir notas.
- Decidir si el registro publico debe permitir el rol PROFESOR (hoy permite ESTUDIANTE, PROFESOR y PADRE).
