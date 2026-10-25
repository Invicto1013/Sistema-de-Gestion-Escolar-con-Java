# SIGE Web (primera entrega)

Para preparar una publicación en un servicio compatible con Docker, consulta [GUIA_DESPLIEGUE_NUBE.md](GUIA_DESPLIEGUE_NUBE.md). La imagen no incluye credenciales y el servidor acepta el puerto `PORT` asignado por la plataforma. Para CI, distribución y JUnit consulta [DEVOPS_PRUEBAS.md](DEVOPS_PRUEBAS.md); el workflow está en `.github/workflows/ci.yml`.

SIGE Web agrega una interfaz adaptable e instalable desde el navegador (PWA), manteniendo intacta la aplicación JavaFX. La barra superior usa un menú plegable accesible por teclado y adaptable a pantallas pequeñas. Incluye inicio de sesión con BCrypt, configuración/verificación TOTP de 2FA, perfil y cambio de contraseña, dashboard según el rol, portal de padres para consultar hijos vinculados, materias, boletas y asistencia, inscripción web de estudiantes, captura de calificaciones y toma de asistencia de profesores, reporte de inscripciones por rango de fechas y reporte de asistencia por sección para profesores y administración con exportación CSV, herramientas administrativas de cuentas con búsqueda por nombre/correo, filtros por rol y estado y exportación CSV de los resultados filtrados, restablecimiento de contraseña con clave temporal de un solo uso y cambio obligatorio, cambio de rol con validación de dependencias y perfil académico asociado, profesores, vínculos familiares, asignaturas y secciones, e historial de sesiones filtrable por usuario, estado y fecha, exportable a CSV y con opción para revocar sesiones activas. El administrador puede detener el servidor desde una sesión local. Los demás módulos de JavaFX todavía deben migrarse antes de que la versión web sustituya por completo al escritorio.

## Requisitos del servidor

- Java 21 y Maven.
- MySQL con la misma base `sige_db` de SIGE y los scripts de esquema/migración ya aplicados, en particular `sql/fase5_2fa.sql`.
- `db.properties` en la carpeta desde la que se inicia Maven, configurado con la conexión del **servidor**. Nunca publiques ese archivo ni las contraseñas.
- La misma clave AES configurada como `2fa.encryption-key` o `SIGE_2FA_ENCRYPTION_KEY` que ya protege los secretos 2FA de las cuentas.

## Ejecutar en el servidor

Desde la raíz del proyecto:

```powershell
mvn clean compile
mvn -Pweb javafx:run
```

En el propio servidor abre `http://127.0.0.1:8080`. El servidor escucha solo en localhost por defecto. Para una prueba en una red privada, puede iniciarse con `SIGE_WEB_HOST=0.0.0.0` y `SIGE_WEB_PORT=8080`; después se abre `http://IP-DEL-SERVIDOR:8080` desde otro dispositivo. El tráfico HTTP no cifra contraseñas; limita esta prueba a una red confiable y vuelve a localhost al terminar. Para evitar dejar la instancia ejecutándose, el administrador puede usar **Detener servidor** desde la página abierta localmente; también se puede detener con `Ctrl+C` en la terminal de Maven. Solo se ofrece el botón en conexiones de loopback/locales.

## Acceso desde Internet e instalación en el dispositivo

Para acceso fuera de la red local, aloja SIGE Web en un servidor siempre encendido, con MySQL accesible únicamente desde ese servidor y HTTPS terminado en un proxy TLS (por ejemplo, Caddy o Nginx). Configura `SIGE_WEB_SECURE_COOKIE=true` detrás de HTTPS y protege también los secretos en el servicio de alojamiento. No abras MySQL directamente a Internet.

La PWA necesita HTTPS para que el navegador ofrezca instalarla (localhost funciona para pruebas). En Android/Chrome usa **Instalar aplicación** o **Añadir a pantalla principal**; en iPhone/Safari usa **Compartir > Añadir a pantalla de inicio**; en escritorio, elige **Instalar SIGE** en el menú del navegador cuando esté disponible. La instalación crea un acceso directo, mientras que el backend y la base de datos siguen ejecutándose en el servidor.

La PWA solo guarda recursos visuales estáticos para la instalación; no almacena credenciales, páginas autenticadas ni datos académicos para uso sin conexión.

## Boletas de estudiantes y padres

Después de iniciar sesión, los estudiantes pueden abrir **Mi boleta** para consultar sus calificaciones por período. Las cuentas de padre/tutor pueden abrir **Boleta de mis hijos**, elegir un estudiante previamente vinculado y consultar sus notas. La consulta valida el vínculo en el servidor antes de devolver la boleta; otros roles no tienen acceso a esta vista web.

Estudiantes también pueden abrir **Mi asistencia** para ver clases registradas, presentes, ausencias y porcentaje por materia y período. Padres/tutores pueden elegir un hijo previamente vinculado en **Asistencia de mis hijos**. La consulta del servidor vuelve a comprobar el vínculo; otros roles no tienen acceso a esta vista web.

En **Mis materias**, estudiantes pueden consultar asignaturas inscritas, períodos, créditos, profesores, horarios, aulas y estado. Padres/tutores pueden revisar la misma información de sus hijos vinculados; la consulta vuelve a validar el vínculo directamente en la base de datos.

Los estudiantes pueden abrir **Inscripciones** para consultar secciones con cupo e inscribirse indicando el período lectivo. La operación usa la identidad de la sesión, incluye protección CSRF y conserva en la base de datos las validaciones de cupo y materias repetidas. Este formulario web no permite a padres ni a otros roles inscribir estudiantes.

Los profesores pueden abrir **Calificaciones**, seleccionar una sección asignada y registrar o corregir notas parciales y examen. La calificación final, literal, estado de la materia y promedio se recalculan en el servidor; se valida que la sección pertenezca al profesor. El administrador puede consultar secciones y corregir notas mediante la misma vista con validación de su rol.

Los profesores pueden abrir **Tomar asistencia**, seleccionar una de sus secciones y la fecha de la clase, marcar ausencias y guardar la lista. Los estudiantes se marcan presentes por defecto; las ausencias se desmarcan. El servidor valida de nuevo la asignación de la sección antes de guardar.

El **Reporte de inscripciones** permite consultar un rango de fechas y descargar los resultados en CSV. La consulta aplica el alcance definido por rol: todos los registros para administración, secciones propias para profesores, datos propios para estudiantes y registros de hijos vinculados para padres/tutores.

El administrador puede gestionar el catálogo desde **Asignaturas**: crear y editar materias, comprobar que el código no esté duplicado y eliminar solo asignaturas sin secciones asociadas. La eliminación requiere confirmación y el servidor bloquea asignaturas que podrían arrastrar inscripciones o calificaciones.

En **Secciones**, administración puede asignar asignatura, profesor, horario, aula y cupo. El sistema impide reducir el cupo por debajo de los estudiantes inscritos, cambiar la asignatura de una sección ocupada y eliminar secciones que tengan estudiantes inscritos.

En **Usuarios**, administración puede activar o desactivar cuentas y restablecer la configuración de 2FA. El sistema impide desactivar la propia cuenta o el último administrador activo; las acciones requieren confirmación y la capa de datos vuelve a validar el rol.

En **Vincular padres**, administración puede asociar cuentas de padre/tutor con estudiantes existentes y quitar vínculos. La vista lista las asociaciones actuales y evita duplicados.

Padres y tutores tienen la página **Mis hijos**, que resume matrícula, grado y promedio de cada estudiante vinculado, con accesos rápidos a sus materias, boleta y asistencia. Esas páginas vuelven a comprobar cada vínculo antes de mostrar información académica.

En **Crear profesor**, administración registra el correo personal del docente, el código y la especialidad. La contraseña inicial se almacena con BCrypt y debe cambiarse en el primer inicio. Cuando SMTP está configurado con una cuenta personal Gmail o Microsoft, SIGE envía una notificación al correo del profesor con los datos básicos del perfil; nunca incluye la contraseña. Por ahora, solo se admiten remitentes y destinatarios personales `gmail.com`, `googlemail.com`, `outlook.com`, `outlook.es`, `outlook.co.uk`, `hotmail.com`, `hotmail.es`, `hotmail.co.uk`, `live.com`, `live.es`, `live.co.uk` y `msn.com`; se omiten los dominios institucionales. Si falla el correo, el perfil queda creado y la pantalla informa que debe revisarse SMTP. El registro de estudiantes y padres/tutores también envía un aviso de creación de perfil (solo a las direcciones personales permitidas); en la web, el administrador recibe el resultado al asignar por primera vez esos roles. El correo del padre/tutor aclara que la consulta académica requiere vincular su cuenta con el estudiante. Si SMTP no está configurado o falla, la cuenta queda guardada y se informa que la notificación no pudo enviarse.

El **Historial de sesiones** muestra hasta 500 accesos recientes, fechas de inicio y cierre, dirección IP y estado. Solo está disponible para administración; la consulta del DAO vuelve a verificar el rol.

## Configuración del servidor

Variables opcionales:

| Variable | Valor por defecto | Uso |
| --- | --- | --- |
| `SIGE_WEB_HOST` | `127.0.0.1` | Dirección de escucha del servidor |
| `SIGE_WEB_PORT` | `8080` | Puerto HTTP interno |
| `SIGE_WEB_SECURE_COOKIE` | `false` | Cámbiala a `true` cuando el acceso use HTTPS |
| `SIGE_DB_URL`, `SIGE_DB_USER`, `SIGE_DB_PASSWORD` | `db.properties` | Conexión MySQL del servidor |
| `SIGE_2FA_ENCRYPTION_KEY` | `db.properties` | Clave AES de 2FA ya usada por las cuentas |
| `SIGE_SMTP_HOST`, `SIGE_SMTP_PORT` | `db.properties` | Servidor SMTP y puerto del proveedor de correo |
| `SIGE_SMTP_AUTH`, `SIGE_SMTP_USER`, `SIGE_SMTP_PASSWORD` | `db.properties` | Autenticación del remitente de notificaciones |
| `SIGE_SMTP_FROM` | `db.properties` | Dirección autorizada para enviar notificaciones |
| `SIGE_SMTP_STARTTLS`, `SIGE_SMTP_SSL` | `db.properties` | Seguridad TLS del servidor SMTP; usa los valores exigidos por tu proveedor |

Para desarrollo local con MySQL 8 y autenticación `caching_sha2_password`, la URL de ejemplo incluye `allowPublicKeyRetrieval=true` para evitar el error `Public Key Retrieval is not allowed`. Esta opción está pensada únicamente para la conexión local sin TLS (`localhost`). Si `SIGE_DB_URL` está definida, tiene prioridad sobre `db.url` y también debe incluir los parámetros necesarios. Para un servidor accesible desde otros dispositivos, configura MySQL con TLS y no reutilices la URL local de ejemplo.

Consulta [GUIA_RED_LOCAL.md](GUIA_RED_LOCAL.md) para iniciar el servidor en una red privada y conectarte desde otro dispositivo.

## Siguiente trabajo de la migración

Pendiente: confirmar el inicio de sesión tras corregir la conexión MySQL; probar la guía de acceso en la red local; desplegar SIGE Web con HTTPS si se requiere acceso público; y migrar progresivamente los módulos que todavía existen solo en JavaFX. La versión web aún no sustituye por completo al escritorio.


## Catálogo curricular de grados

La página administrativa /curriculo presenta los seis grados de Primaria y los seis de Secundaria, agrupados por ciclo y con sus áreas curriculares base. Para cargar el catálogo, ejecuta una vez sql/fase_curriculo_grados.sql en la base sige_db. Las horas semanales quedan vacías/configurables; los centros pueden enriquecer la oferta conforme a su jornada, modalidad y plan. En Asignaturas el administrador asigna un área y uno o varios grados; Secciones exige que la asignatura pertenezca al grado indicado. Calificaciones y las vistas del profesor muestran el grado de cada sección.

### Notificaciones por correo

La notificación SMTP permanece sin activar hasta que SIGE esté estable. Cuando llegue ese momento, usa únicamente una cuenta personal de Gmail/Googlemail u Outlook/Hotmail/Live/MSN como remitente y destinatario; los correos institucionales se omiten por ahora. Para Gmail personal se suele usar `smtp.gmail.com`, puerto `587`, STARTTLS; el acceso puede requerir contraseña de aplicación según la elegibilidad actual de la cuenta. Para una cuenta personal Microsoft, configura el host y método de autenticación que el proveedor indique para esa cuenta. No uses la contraseña normal si el proveedor exige una contraseña de aplicación. Los valores ejemplo de `db.properties.example` son marcadores, no credenciales activas. El usuario y la clave SMTP deben guardarse fuera de Git. Los perfiles se guardan aunque el correo se omita o falle; SIGE nunca envía contraseñas iniciales.

## Aula virtual y tareas

El profesor y el estudiante tienen **Aula y tareas** en el menú y en sus perfiles. El profesor publica material de clase o tareas dentro de sus secciones; puede adjuntar archivos en cualquier formato. Cada estudiante inscrito ve esas publicaciones y tiene el botón **Subir entrega** para adjuntar su trabajo. El profesor puede descargar los archivos enviados por los estudiantes de sus secciones. La última entrega sustituye a la anterior.

Antes de usar esta página, ejecuta `sql/aula_virtual.sql` en `sige_db`. Los archivos se guardan en `data/uploads` fuera del área pública; configura `SIGE_UPLOAD_DIR` para usar un directorio privado persistente en el servidor. El límite actual es 20 MB por archivo. Todos los formatos se tratan como descargas, sin ejecución ni vista previa en el navegador. Configura almacenamiento persistente y copias de seguridad antes de usarlo en producción.
