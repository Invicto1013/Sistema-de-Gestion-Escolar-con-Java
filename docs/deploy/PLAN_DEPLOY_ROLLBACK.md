# Plan de despliegue y rollback — SIGE

## Entornos
| Entorno | Dónde | Base de datos | Observaciones |
|---|---|---|---|
| Local | PC del autor (`mvn`, JavaFX) | `config.properties` (no versionado) | Pruebas diarias |
| Staging (propuesto) | Segundo servicio de Render apuntando a la rama `staging` | Base separada `sige_staging` en TiDB Starter | Datos ficticios; no existe aún, hay que crearlo |
| Producción | Render (Docker) desde `main` | `sige_db` en TiDB Cloud | Variables `SIGE_DB_URL`, `SIGE_DB_USER`, `SIGE_DB_PASSWORD` en Render |

Desplegar en Render no usa SFTP: se hace con `git push`. El flujo SFTP de la guía se practica como simulación con `deploy/winscp/` (solo vista previa).

## Circuito (según la guía)
| Etapa | Acción | Condición para continuar |
|---|---|---|
| 1 | Crear rama `feature/...` y cambio mínimo | Requerimiento definido |
| 2 | Protector revisa impacto | Sin impacto injustificado |
| 3 | Regresión: `mvn clean test` | Pruebas críticas aprobadas |
| 4 | Integrador: PR y CI en GitHub Actions | Build y CodeQL en verde |
| 5 | Responsable autoriza staging | Aprobación humana |
| 6 | Deploy a staging (push a `staging`) | `/health` correcto |
| 7 | Smoke tests manuales | Casos de la matriz aprobados |
| 8 | Autorización de producción | Respaldo y rollback listos |
| 9 | Merge a `main` y tag `vX.Y.Z` | Release publicado y `/health` estable |

## Respaldo antes de publicar
```powershell
java Respaldo.java <host> 4000 <usuario> sige_db sige_respaldo.sql   # la contraseña se escribe en consola
```
Guardar el `.sql` fuera del repositorio.

## Rollback
1. **Código:** en Render, *Deploys* → elegir el último despliegue estable → *Rollback*; o `git revert <commit>` y push.
2. **Release:** no mover tags publicados; publicar una versión nueva con la corrección.
3. **Base de datos:** si hubo migración, restaurar con `Cargar.java` desde el respaldo previo a una base nueva y apuntar `SIGE_DB_URL` a ella. Las migraciones destructivas no se revierten solas: por eso requieren aprobación y respaldo.
4. **Escritorio:** reinstalar la versión anterior desde *Releases* de GitHub.
5. Registrar el incidente en `docs/INCIDENTES.md`.

## Qué nunca se publica
`config.properties`, `db.properties`, `*.bak`, respaldos `.sql`, claves de Brevo.
