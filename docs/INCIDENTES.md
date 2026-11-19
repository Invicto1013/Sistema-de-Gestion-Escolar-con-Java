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

## INC-009: No se podían guardar calificaciones en producción (TiDB)

- **Fecha:** 2026-11-18
- **Detectado por:** prueba manual de regresión (caso 2, calificaciones), rol PROFESOR
- **Síntoma:** al guardar notas de ING-101 aparecía "No se pudieron guardar las notas.
  Verifica la conexión con MySQL." La conexión a la base de datos estaba bien.
- **Causa raíz:** `PeriodoDAO.estadoCompartido` usa `SELECT … FOR SHARE`. TiDB solo lo
  implementa como "noop" y lo rechaza por defecto
  (SQLState 42000, código 1235, `tidb_enable_noop_functions`).
  En MySQL local y en CI funciona, por eso las pruebas no lo detectaron.
- **Por qué tardó en diagnosticarse:** el `catch (SQLException)` de `saveGrade` mostraba
  un mensaje genérico y no registraba el error real. Los logs de Render no mostraban nada útil.
- **Detección:** se añadió logging en `saveGrade` (SQLState, código y stack trace),
  se desplegó y se repitió la acción. El log mostró el error exacto a las 5:10 p.m.
- **Corrección:** se añadió `&sessionVariables=tidb_enable_noop_functions=ON`
  a `SIGE_DB_URL` en Render. Verificado: "La calificación se guardó correctamente."
- **Riesgo residual:** el bloqueo compartido pasa a ser un no-op en TiDB. Se acepta
  porque solo el profesor de la sección edita esas notas. Alternativa futura:
  cambiar la consulta a `FOR UPDATE` en el código.
- **Prevención:**
  - Registrar el error real en los `catch` de operaciones críticas (73 `catch (SQLException)`
    en `WebApp.java`; empezar por guardar, eliminar y crear).
  - Añadir pruebas de integración contra TiDB, o una lista de consultas incompatibles
    (`FOR SHARE`, `LOCK IN SHARE MODE`, `SAVEPOINT`, `LOCK TABLES`).
- **Estado:** cerrado.