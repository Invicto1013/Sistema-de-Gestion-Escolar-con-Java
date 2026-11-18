# REGLAS DEL PROYECTO — SIGE (Sistema de Gestión Escolar)

Autor: Elian Castro · Universidad Nacional Evangélica (UNEV)
Pila: Java 21, JavaFX (escritorio), servidor web propio (`WebApp.java`), MySQL/TiDB, Maven, Render (Docker).
Objetivo: mejorar el sistema sin destruir funciones anteriores.

> Este archivo contiene instrucciones. Por sí solo no ejecuta nada: lo leen las herramientas (Claude Code, Antigravity, Copilot…) que tengan permisos para actuar. Los roles detallados están en `agents/`.

## Reglas
1. Inspeccionar arquitectura y dependencias antes de editar (`src/main/java/...`, `sql/`, `pom.xml`).
2. Trabajar en una rama Git (`feature/...`); no modificar `main` directamente.
3. Preservar rutas web, contratos públicos y comportamiento existente (ver `docs/regresion/MATRIZ_REGRESION.md`).
4. Proponer el cambio mínimo que satisfaga el requisito.
5. Crear o actualizar pruebas y ejecutar la regresión completa: `mvn clean test` (en PowerShell: `mvn clean test "-Djacoco.skip=true"` si JaCoCo falla con el JDK local).
6. Registrar archivos afectados, causas y evidencias en `docs/change-log.md` y `docs/INCIDENTES.md`.
7. Nunca exponer claves, tokens ni datos sensibles. `config.properties`, `db.properties`, `*.bak` y respaldos `.sql` no se suben a Git.
8. No usar datos reales de estudiantes en pruebas ni en capturas.
9. No publicar con pruebas críticas fallidas.
10. Pedir aprobación humana para producción y para migraciones destructivas (`DROP`, `TRUNCATE`, cambios de columnas).
11. Antes de publicar: respaldo de la base (`Respaldo.java`), comprobación de salud (`/health`) y plan de rollback (`docs/deploy/PLAN_DEPLOY_ROLLBACK.md`).
12. Distinguir siempre pruebas **aprobadas**, **fallidas** y **no ejecutadas**; no declarar éxito de lo que no se ejecutó.

## Restricciones técnicas específicas
- Política CSP estricta en la web: sin scripts ni estilos en línea; los formularios y preferencias funcionan con cookies y redirecciones.
- Las plantillas con `String.formatted` escapan `%` como `%%`.
- Los permisos viven en `model/Permisos.java`; no repartir comprobaciones de rol por la interfaz.
- CSS nuevo va antes del marcador `/* ---- Tema oscuro` y requiere sus reglas oscuras.
- Aula Virtual (`sige-aula`) es un repositorio aparte que comparte la base de datos.

## Agentes (plantillas en `agents/`)
Orquestador · Protector de módulos · Regresión y QA · Trazabilidad · Integrador · Deploy · Seguridad.
