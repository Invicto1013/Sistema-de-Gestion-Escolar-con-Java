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
## Administración y reportes (Tarea 4, fase 4)

- **Administrador > Usuarios:** crea cuentas de profesor, cambia el rol/perfil de cuentas sin relaciones
  académicas, activa o desactiva cuentas y restablece el estado de 2FA. Un rol no se cambia si al perfil
  actual todavía le pertenecen inscripciones, secciones o vínculos familiares; así se protegen los datos.
- Los cambios administrativos vuelven a validar en MySQL que quien los ejecuta sea un administrador activo.
  Los cambios de rol marcan la cuenta para cambiar su contraseña en el siguiente acceso.
- La autenticación rechaza las cuentas desactivadas y las contraseñas continúan almacenadas con BCrypt.
- **Reporte de inscripciones:** filtro por rango de fechas y alcance por rol (todos para ADMIN, secciones del
  profesor, cuenta del estudiante o hijos vinculados del padre/tutor). Desde la vista se imprime o se guarda
  como PDF; el encabezado incluye SIGE, fecha de emisión, usuario, rol y rango consultado.
- Antes de iniciar esta versión, ejecuta una vez `sql/fase4_gestion_usuarios.sql` con el esquema `sige_db`
  seleccionado en MySQL Workbench. Agrega el estado de cuenta y los campos para restablecer y bloquear el 2FA.
- `ReporteDAO` aplica fechas y permisos en SQL; `ReporteInscripcionesPanel` presenta/exporta el resultado.
# Fase 5: autenticación de dos factores

La aplicación solicita configurar una app compatible con TOTP (por ejemplo, Google Authenticator o Microsoft Authenticator) en el primer inicio válido. Después de escanear el QR, confirma con el código temporal de 6 dígitos. Los siguientes inicios requieren ese código. Tres códigos erróneos bloquean temporalmente el acceso durante 15 minutos. Los éxitos y fallos se guardan en `bitacora_accesos`.

## Preparación de Fase 5

1. En MySQL Workbench abre y ejecuta `sql/fase5_2fa.sql` en el esquema `sige_db`. La cuenta de MySQL de SIGE debe tener permisos para crear esa tabla. La migración de Fase 4 debe haberse ejecutado antes, porque allí se agregaron los campos de 2FA a `usuarios`.
2. Si aún no tienes `db.properties`, copia `db.properties.example` como `db.properties` en la carpeta desde donde ejecutas SIGE y completa los datos locales de MySQL.
3. Genera una clave local AES-256 desde PowerShell, en el directorio del proyecto:

   ```powershell
   $clave2FA = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
   Add-Content -LiteralPath .\db.properties -Value "`n2fa.encryption-key=$clave2FA"
   Remove-Variable clave2FA
   ```

   Protege `db.properties`: contiene secretos locales y no se debe subir a Git. No cambies ni pierdas esta clave después de configurar cuentas; es necesaria para descifrar los secretos TOTP guardados. También se admite la variable de entorno `SIGE_2FA_ENCRYPTION_KEY`.
4. Ejecuta `mvn clean compile` y luego inicia la aplicación desde VS Code. Al iniciar sesión por primera vez, escanea el QR y confirma el código de seis dígitos.

La dependencia ZXing Core 3.5.4 genera el QR. Si Maven no puede descargar dependencias por un certificado/proxy local, configura el acceso de Maven Central en esa computadora y vuelve a compilar.

## Fase 6: dashboard del sistema

Al iniciar sesión, la opción Inicio carga cuatro KPI, un gráfico de inscripciones por asignatura, un gráfico del estado de las materias y una tabla con las inscripciones recientes. Todos los valores salen de MySQL. El DAO limita las consultas al usuario autenticado: administrador ve el conjunto institucional, profesor sus secciones, estudiante sus materias y padre/tutor los estudiantes vinculados. Las consultas se ejecutan en un `Task` JavaFX para que la pantalla no se congele. Si faltan datos, los gráficos y la tabla muestran el estado vacío.

## Fase 7: escritorio, tableta y teléfono

La escena aplica los modos `modo-escritorio` (1024 px o más), `modo-tableta` (600 a 1023 px) y `modo-telefono` (menos de 600 px) según el ancho disponible. En teléfono, el menú lateral se sustituye por un selector compacto superior, los KPI se apilan en una columna, los gráficos pasan a filas y la tabla oculta columnas secundarias. En tableta se conservan dos columnas para KPI y se reduce el menú; en escritorio se usan cuatro columnas. Login y registro reducen sus márgenes y campos para la referencia de 412 px. Para las evidencias de la tarea, redimensiona la ventana a 1366x768, 1024x768, 768x1024 y 412x915 y captura login, dashboard y un CRUD.
