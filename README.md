# SIGE - Sistema de Gestión Educativa

Aplicación de gestión escolar para primaria y secundaria, desarrollada con Java 21, JavaFX, MySQL y un módulo web adaptable. La aplicación de escritorio sirve a la operación local; SIGE Web es un servidor Java HTTP que reutiliza la capa de datos de MySQL. La interfaz web no es una aplicación JavaFX ejecutada en el navegador.

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

`sql/sige_completo.sql` crea la estructura completa (26 tablas, con currículo por grados, aula virtual, períodos lectivos, auditoría e historial académico), los roles, un usuario de prueba por rol y datos de ejemplo. Las credenciales de prueba están al inicio del script. Para actualizar una base que ya existe, aplica en orden `sql/fase_curriculo_grados.sql`, `sql/aula_virtual.sql` y `sql/fase_periodos_auditoria.sql`.

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
