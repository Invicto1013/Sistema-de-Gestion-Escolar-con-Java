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

## Fase 6: asistencia

- **Profesor > Asistencia:** elige una sección y una fecha; cada estudiante puede quedar como Presente,
  Ausente o Sin registrar. Solo los estados explícitos se guardan. El profesor solo puede modificar
  listas de sus propias secciones y el guardado de una fecha ocurre en una transacción.
- **Estudiante > Mi asistencia:** muestra presentes, ausentes, total de clases registradas y porcentaje
  por materia inscrita. El porcentaje usa como denominador los días que ya tienen lista registrada.
- Antes de usar estas pantallas, ejecuta `sql/asistencias.sql` una vez en MySQL Workbench sobre `sige_db`.
- Nuevos archivos: `data/AsistenciaDAO`, `model/FilaAsistencia`, `model/ResumenAsistencia`,
  `controller/AsistenciaPanel` y `controller/MiAsistenciaPanel`; `DashboardController` habilita las opciones.

## Fase 7: vinculación de padres e hijos

- **Administrador > Vincular padres:** selecciona una cuenta de padre/tutor y un estudiante ya registrados,
  indica el parentesco y crea o elimina el vínculo. La relación admite varios tutores por estudiante y
  varios estudiantes por tutor; un vínculo duplicado se evita.
- **Padre/tutor > Mis hijos:** muestra solamente estudiantes vinculados a la cuenta autenticada, con matrícula,
  grado y promedio académico.
- **Padre/tutor > Calificaciones / Asistencia:** permite consultar las notas y el resumen de asistencia de
  cualquiera de sus hijos vinculados. Cada consulta verifica el vínculo en la base de datos.
- Ejecuta `sql/vinculacion_padres.sql` una vez en MySQL Workbench sobre `sige_db`. Si la tabla `estudiantes`
  todavía no existe, completa primero la instalación base del esquema.
- Nuevos archivos: `sql/vinculacion_padres.sql`, `data/VinculacionDAO`, `model/VinculoPadreEstudiante`,
  `controller/VinculacionesPanel` y `controller/MisHijosPanel`; `DashboardController` agrega las opciones.
- La consulta familiar agrega `model/CalificacionHijo`, `controller/CalificacionesHijosPanel` y
  `controller/AsistenciaHijosPanel`, además de consultas protegidas en `CalificacionDAO` y `AsistenciaDAO`.

## Fase 8: boleta de calificaciones

- **Estudiante > Boleta:** emite su propia boleta por período.
- **Padre/tutor > Boleta:** selecciona un hijo vinculado; la consulta vuelve a verificar la relación
  antes de devolver notas.
- **Administrador > Boletas:** selecciona cualquier estudiante para consultar su boleta.
- La boleta incluye matrícula, grado, período, notas parciales, examen final, nota final, literal,
  estado y promedio de las materias evaluadas. El botón de impresión abre el diálogo del sistema;
  para crear un PDF, selecciona una impresora PDF disponible (por ejemplo, Microsoft Print to PDF).
- Nuevos archivos: `data/BoletaDAO`, `model/LineaBoleta` y `controller/BoletaPanel`;
  `DashboardController` habilita el acceso según el rol.

## Fase 9: administración de calificaciones

- **Administrador > Calificaciones:** puede consultar las calificaciones de cualquier sección y corregir
  parcial 1, parcial 2 y examen final. La nota final, el literal, el estado de la materia y el promedio
  del estudiante se recalculan igual que al guardar como profesor.
- El guardado administrativo verifica el rol ADMIN en la base de datos; el flujo del profesor sigue
  limitado a sus propias secciones.
- No requiere cambios de esquema. `CalificacionesPanel` admite modo de administrador y
  `CalificacionDAO.guardarComoAdministrador` aplica la autorización y transacción.

## Fase 10: alta administrativa de profesores

- El registro público solo ofrece los roles ESTUDIANTE y PADRE; no permite crear usuarios ADMIN ni PROFESOR.
- **Administrador > Crear profesor:** registra el nombre, correo, código, especialidad y contraseña inicial.
- `UsuarioDAO.registrarProfesorComoAdmin` comprueba el rol ADMIN dentro de la misma transacción antes de
  insertar al usuario y su perfil de profesor.
- No requiere cambios de esquema.

## Fase 11: cambio de contraseña

- Las cuentas de profesor existentes quedan marcadas para cambiar su contraseña inicial al primer inicio
  tras ejecutar `sql/cambio_contrasena.sql`; los profesores nuevos se marcan al crearlos.
- El inicio de sesión exige completar el cambio antes de abrir el dashboard. La contraseña nueva se guarda
  con BCrypt y el indicador se limpia en la misma transacción.
- Todos los roles pueden cambiar su contraseña desde **Mi perfil**, confirmando primero la contraseña actual.
- Ejecuta `sql/cambio_contrasena.sql` una sola vez (el script detecta si la columna ya existe) antes de usar
  esta versión. Las cuentas no docentes conservan su contraseña actual.
- Nuevos archivos: `sql/cambio_contrasena.sql` y `controller/CambioContrasenaDialog`; `Usuario`, `UsuarioDAO`,
  `LoginController` y `DashboardController` añaden el flujo correspondiente.

## Fase 12: historial de sesiones

- **Administrador > Historial de sesiones:** consulta los últimos 500 accesos con usuario, correo,
  inicio, cierre, dirección IP y estado.
- La consulta vuelve a comprobar el rol ADMIN en MySQL y no requiere cambios de esquema.
- Se agregan `model/SesionRegistrada` y `controller/HistorialSesionesPanel`; `SesionDAO` consulta el registro
  existente y `DashboardController` agrega el acceso administrativo.

## Pendiente (siguientes pasos)

- Reportes academicos (boleta de calificaciones) y que el administrador pueda ver/corregir notas.
