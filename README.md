# SIGE - Sistema de Gestión Educativa

Autor: **Elian Castro** · Institución: **Universidad Nacional Evangélica (UNEV)**

Aplicación de gestión escolar para primaria y secundaria, desarrollada con Java 21, JavaFX, MySQL y un módulo web adaptable. La aplicación de escritorio sirve a la operación local; SIGE Web es un servidor Java HTTP que reutiliza la capa de datos de MySQL. La interfaz web no es una aplicación JavaFX ejecutada en el navegador.

## Roles de director/a y secretario/a

Además de administrador, profesor, estudiante y padre/tutor, SIGE tiene dos cargos administrativos:

| Función | Admin | Director/a | Secretario/a |
|---|:-:|:-:|:-:|
| Informes, boletas, carnets y asistencia de todos los grupos | Sí | Sí | Sí |
| Publicar anuncios | Sí | Sí | Sí |
| Registrar conducta y eliminar observaciones ajenas | Sí | Sí | No |
| Auditoría | Sí | Sí | No |
| Solicitudes de registro y vínculos padre-estudiante | Sí | No | Sí |
| Horarios | Sí | No | Sí |
| Crear cuentas de personal | Sí | No | No |

El administrador crea estas cuentas en **Crear personal** (web y ejecutable). La contraseña inicial se entrega por otro medio y la persona debe cambiarla al entrar por primera vez.

### Aviso por correo al crear un perfil

El aviso nunca incluye la contraseña y solo se envía a correos personales de Gmail, Outlook, Hotmail, Live o MSN. Hay tres métodos, en este orden:

1. **API HTTPS de Brevo (recomendado en Render gratis, que bloquea SMTP).** Crea una cuenta gratuita en brevo.com, verifica tu correo remitente en *Senders* y genera una clave en *SMTP & API > API Keys*. Define en Render: `SIGE_BREVO_API_KEY`, `SIGE_MAIL_FROM` (el remitente verificado) y, si quieres, `SIGE_MAIL_FROM_NAME`.
2. **SMTP** (para uso local): `SIGE_SMTP_HOST`, `SIGE_SMTP_PORT`, `SIGE_SMTP_USER`, `SIGE_SMTP_PASSWORD` y `SIGE_SMTP_FROM`. Con Gmail se necesita una contraseña de aplicación.
3. **Manual, sin configurar nada:** si no hay método automático o falla, aparece el botón «Enviar aviso desde mi correo», que abre tu programa de correo con el mensaje listo (en el ejecutable, si no se puede abrir, lo copia al portapapeles).

## Acerca de SIGE

El icono de interrogación (❓) de la barra superior abre la información del proyecto: versión, qué incluye, roles, seguridad, tecnologías y créditos. En la web lleva a `/acerca` (disponible también antes de iniciar sesión) y en el ejecutable abre una ventana. El texto está en `AcercaDe.java`; al publicar una versión nueva, actualiza allí la constante `VERSION`.

## Requisitos

- JDK 21 (JavaFX runtime se resuelve con Maven).
- Maven 3.9 o posterior.
- MySQL 8 y el esquema de `sql/` aplicado.
- Credenciales locales en `db.properties`, creado desde `db.properties.example`. El archivo real es ignorado por Git.

## Ejecutar escritorio

```powershell
Copy-Item db.properties.example db.properties
# Completa db.url, db.user, db.password y 2fa.encryption-key en db.properties
mvn clean javafx:run
```

## Ejecutable distribuible (JAR y jpackage)

```powershell
Copy-Item config.example.properties config.properties
# Completa db.url, db.user, db.password y 2fa.encryption-key en config.properties
mvn clean package            # genera target/sige-1.0.0.jar (incluye JavaFX y las librerías)
java -jar target\sige-1.0.0.jar
.\empaquetar.bat             # app portable en target\dist\SIGE\SIGE.exe (con "exe", instalador; requiere WiX)
```

`config.properties` (o `db.properties`) debe estar junto al `.jar` o en la carpeta desde donde se ejecuta; está ignorado por Git y no se empaqueta dentro del JAR. El JAR contiene las librerías de JavaFX del sistema donde se compila.

## Base de datos

`sql/sige_completo.sql` crea la estructura completa (29 tablas, con currículo por grados, aula virtual, períodos lectivos, auditoría, historial académico, anuncios escolares, horarios de secciones y cuaderno de conducta), los roles, un usuario de prueba por rol y datos de ejemplo. Las credenciales de prueba están al inicio del script. Para actualizar una base que ya existe, aplica en orden `sql/fase_curriculo_grados.sql`, `sql/aula_virtual.sql`, `sql/fase_periodos_auditoria.sql` y `sql/fase_extensiones_escolares.sql`.

## Extensiones escolares avanzadas

- **Tablón de anuncios y circulares:** comunicados institucionales emitidos por dirección con nivel de prioridad (normal, importante, urgente) y segmentación por rol.
- **Horarios escolares y aulas:** planificación semanal de bloques de clase de lunes a sábado con prevención automática de choques o cruces de horario para profesores.
- **Cuaderno de observaciones y conducta:** registro docente de reconocimientos/méritos, tardanzas, compromisos de tareas e incidencias disciplinarias, visible por estudiantes y padres/tutores.
- **Boleta oficial imprimible / PDF:** formato membretado con promedio ponderado, asignaturas evaluadas y firmas oficiales escolares listo para imprimir o guardar como PDF.
- **Carnet estudiantil con código QR:** credencial escolar imprimible con datos del alumno y código QR de matrícula de alta resolución para control de acceso y biblioteca.

## Accesibilidad visual y botones dinámicos

- **Modos de visión:** Normal, Daltonismo (azul y naranja, paleta Okabe-Ito, más símbolos ✔ ⚠ para no depender del color) y Alto contraste.
- **Escritorio:** icono de ojo en el encabezado del panel (menú con los modos de visión); recuerda la elección entre sesiones.
- **Web:** icono de ojo en la barra superior (menú con los modos de visión); la elección se guarda en una cookie (`sige_vision`) sin datos personales y no usa JavaScript.
- **Botones dinámicos:** el escritorio los anima con JavaFX (crecen al pasar el cursor y se hunden al pulsar) y la web con transiciones CSS; se desactivan con «reducir movimiento» del sistema. No requiere bibliotecas nuevas.
- **Iconos:** cada apartado del menú lleva un icono decorativo (oculto para lectores de pantalla) y la aplicación y el ejecutable usan el logo de SIGE (`recursos/sige.ico`, aplicado por `empaquetar.bat`).
- **Tema claro y oscuro:** un icono de brillo (luna/sol) en la barra superior cambia entre tema claro y oscuro, en el escritorio y en la web; la web admite además el modo Automático (sigue el tema del sistema) (`sige_tema`; en Aula `aula_tema`, por defecto Automático). El ejecutable usa el mismo código, así que también lo incluye. El modo de visión (daltonismo, alto contraste) siempre prevalece sobre el tema.
- **Panel principal animado:** el encabezado y el resumen entran con una animación suave, la ilustración (libro, barras, anillo y destellos) se mueve y las tarjetas se elevan al pasar el cursor; en el ejecutable el encabezado tiene círculos flotantes y las tarjetas se animan. Si Windows tiene activado «mostrar animaciones: desactivado», las animaciones web se ocultan; en el menú del ojo > Animaciones elige «Siempre activas» para verlas.
- **Estaciones del año:** los fondos, colores de acento y animaciones (flores, sol, hojas, nieve) cambian solos 7 días antes del inicio de cada estación (hemisferio norte: 13-mar, 14-jun, 15-sep y 14-dic). En la web se puede elegir la temporada o desactivar los efectos desde el menú del ojo; en el escritorio, con la variable `SIGE_ESTACION` (`primavera`, `verano`, `otono`, `invierno` o `ninguna`). Los efectos se desactivan con «reducir movimiento» del sistema y en el modo de alto contraste.

## Ejecutar web local

```powershell
mvn -Pweb javafx:run
```

Abre `http://127.0.0.1:8080`. Para despliegue remoto consulta [README_WEB.md](README_WEB.md) y [GUIA_DESPLIEGUE_NUBE.md](GUIA_DESPLIEGUE_NUBE.md). Configura los secretos del servidor en variables de entorno; no versionar contraseñas, tokens, claves AES o copias de base de datos.

## Verificar y probar

```powershell
mvn clean verify
```

La suite JUnit cubre la nota ponderada, los límites de la escala literal y el límite de aprobación. Las pruebas actuales son unitarias, no requieren MySQL ni reemplazan las pruebas de integración manuales con una base de prueba.

## CI y artefactos

GitHub Actions está configurado en `.github/workflows/ci.yml`. Al hacer push a `main`, abrir un pull request o iniciar el workflow manualmente, ejecuta `mvn clean verify` en Windows, Linux y macOS, y deja un artefacto de esa ejecución para descargar desde la pestaña Actions. Los artefactos de Actions son temporales; todavía se debe crear una GitHub Release para tener una versión pública estable.

La evidencia y análisis de DevOps se encuentran en `DEVOPS_PRUEBAS.md`. El informe de la tarea documenta qué partes están implementadas y qué evidencias deben completarse en GitHub o en el intercambio de aula.

## Aula virtual

Consulta [AULA_VIRTUAL.md](AULA_VIRTUAL.md) para instalar las tablas del aula, configurar el almacenamiento de archivos y usar las publicaciones y entregas de profesores y estudiantes.


## Créditos y originalidad

Licencias de las dependencias, recursos propios, uso de IA y la verificación de similitud con otros proyectos están en [`docs/CREDITOS_Y_ORIGINALIDAD.md`](docs/CREDITOS_Y_ORIGINALIDAD.md).

## Documentación

- [Manual de usuario](docs/MANUAL_USUARIO.md)
- [Diagrama entidad-relación](docs/DIAGRAMA_ER.md)
- [Casos de prueba de la Fase D](docs/PRUEBAS_FASE_D.md)
- [Antes y después](docs/ANTES_DESPUES.md)
- [Revisión de seguridad](docs/SEGURIDAD.md)
- [Avance del proyecto](docs/PROGRESO.md)
- [Seguridad: revisión de los 20 puntos](docs/SEGURIDAD.md)
