# Bitácora de incidentes y decisiones

Los datos de commit y fecha marcados «completar» se obtienen con `git log --date=short --format="%ad %h %s"`. No se anotan contraseñas.

| ID | Fecha | Módulo | Síntomas | Causa | Corrección | Estado |
|---|---|---|---|---|---|---|
| INC-001 | completar | Cobertura (JaCoCo) | `mvn clean test` falla con «Unsupported class file major version 70» | JaCoCo 0.8.12 no soporta Java 26 (el JDK local) | Subir JaCoCo a 0.8.15 en `pom.xml`; mientras tanto `"-Djacoco.skip=true"` | Resuelto |
| INC-002 | completar | `GeneradorDocumentos` (boleta) | 1 error en `GeneradorDocumentosTest`: `UnknownFormatConversion` en `;` | `100%;` dentro de una plantilla `String.formatted` | Escapar como `100%%;` | Resuelto |
| INC-003 | completar | CI (`dependabot.yml`) | Verificación fallida en GitHub tras un commit | Archivo de configuración con formato inválido | Reescribir con ecosistemas Maven, GitHub Actions y Docker | Resuelto |
| INC-004 | 2026-11-13 | Release | `tag 'v1.1.0' already exists` al publicar | El tag ya estaba publicado | No mover tags publicados; crear una versión nueva (`v1.4.0`) | Resuelto |
| INC-005 | 2026-11-13 | Seguridad / Git | `git status` mostró `config.properties.aiven.bak` preparado para commit | Copia de la configuración anterior con credenciales | `git restore --staged`, mover el archivo fuera del repo y añadir `*.bak` a `.gitignore`; el archivo nunca llegó al historial (`git log`) | Resuelto |
| INC-006 | 2026-11-12 | Aula / base de datos | «SSO: base de datos no disponible», `Connection is not available, request timed out` | Tras migrar a TiDB, el Aula seguía con la URL de la base anterior | Actualizar `AULA_DB_URL`, `AULA_DB_USER`, `AULA_DB_PASSWORD` en Render | Resuelto |
| INC-007 | completar | Escritorio / empaquetado | «No se pudo conectar con la base de datos» tras cambiar la configuración | El instalador llevaba una copia antigua de `config.properties` | Volver a ejecutar `empaquetar.bat` | Resuelto |
| INC-008 | completar | Migración de datos | 57 errores al cargar el respaldo en TiDB | El respaldo usaba comillas dobles (ANSI_QUOTES) | La herramienta de carga fija `sql_mode` con `ANSI_QUOTES` en la sesión; segunda carga: 0 errores, 33 tablas | Resuelto |

## Decisiones técnicas
- **DEC-001:** el dominó del Aula guarda las partidas en memoria (sin tablas nuevas). Limitación: se pierden si Render reinicia el servicio.
- **DEC-002:** los avisos por correo usan la API HTTPS de Brevo porque Render gratis bloquea SMTP; el envío manual por `mailto` queda como respaldo.
