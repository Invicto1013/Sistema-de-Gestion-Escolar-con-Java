# SIGE - Sistema de Gestión Educativa

Aplicación de gestión escolar para primaria y secundaria, desarrollada con Java 25, JavaFX, MySQL y un módulo web adaptable. La aplicación de escritorio sirve a la operación local; SIGE Web es un servidor Java HTTP que reutiliza la capa de datos de MySQL. La interfaz web no es una aplicación JavaFX ejecutada en el navegador.

## Requisitos

- JDK 25 (JavaFX runtime se resuelve con Maven).
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

`sql/sige_completo.sql` crea la estructura completa (21 tablas, con currículo por grados y aula virtual), los roles, un usuario de prueba por rol y datos de ejemplo. Las credenciales de prueba están al inicio del script. Para actualizar una base que ya existe, aplica en orden `sql/fase_curriculo_grados.sql` y `sql/aula_virtual.sql`.

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
