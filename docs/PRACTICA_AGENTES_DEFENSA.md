# Práctica: equipo de agentes de IA, regresión y despliegue — SIGE

Autor: Elian Castro · Universidad Nacional Evangélica (UNEV)

## Cómo cubre SIGE la rúbrica (10 puntos)
| Criterio | Dónde está | Qué falta por demostrar |
|---|---|---|
| Estructura y reglas (2) | `AGENTS.md`, `docs/`, `agents/`, `.github/workflows/` | Captura del repositorio |
| Instrucciones de agentes (2) | `agents/` (orquestador, protector, regresión, trazabilidad, integrador, deploy, seguridad) | Mostrar un agente aplicado a un cambio real |
| Pruebas de regresión (2) | `docs/regresion/MATRIZ_REGRESION.md` + 187 pruebas unitarias | Ejecutar y completar `EVIDENCIAS.md` tras la mejora |
| Automatización y deploy (2) | CI (`ci.yml`, `codeql.yml`, `cobertura.yml`, `integration.yml`), `release.yml`, Render, plan en `docs/deploy/` | Staging real y SFTP en servidor de pruebas (hoy: simulación documentada) |
| Trazabilidad y defensa (2) | `docs/INCIDENTES.md`, `docs/change-log.md` | Completar fechas y commits con `git log` |

## Mejora pequeña propuesta para la demostración
Elegir un cambio verificable y de bajo riesgo, por ejemplo: añadir una prueba de regresión nueva sobre `Permisos` (por ejemplo, que `SECRETARIA` no pueda eliminar conducta ajena) y registrar su ejecución antes y después. Rama: `feature/prueba-regresion-permisos`.

## Respuestas orientativas a las preguntas de reflexión
- **AGENTS.md vs. agente que actúa:** el archivo son instrucciones; el agente es la herramienta con permisos que las lee y ejecuta comandos. Sin herramienta, el archivo no hace nada.
- **Por qué quien escribe el código no aprueba el despliegue:** para separar funciones y evitar sesgo; el Integrador, la Seguridad y un responsable humano revisan el trabajo del Desarrollador.
- **Cómo una mejora en ventas rompe devoluciones (ejemplo POS):** al cambiar el cálculo del total sin actualizar el reintegro, que usa el mismo cálculo. En SIGE: cambiar la escala de calificaciones rompe promedios y boletas.
- **Evidencia de que una prueba se ejecutó:** salida de Maven con conteos, `target/surefire-reports`, ejecución verde en GitHub Actions con enlace.
- **Riesgos de FTP sin cifrado:** usuario y contraseña viajan en claro; usar SFTP o FTPS.
- **Evitar publicar configuraciones sensibles:** `.gitignore` (`config.properties`, `*.bak`), variables de entorno en Render, revisión de `git status` antes de cada commit (ocurrió en INC-005).
- **Requisitos de un rollback seguro:** versiones anteriores conservadas, respaldo de la base, migraciones reversibles o respaldo previo, procedimiento probado.
- **Cuándo es indispensable la aprobación humana:** producción, migraciones destructivas, cambios de permisos y fusión a `main`.

## Límites honestos
- Los agentes aquí son plantillas de instrucciones; las herramientas (Claude Code, Antigravity, Copilot) las usan solo si se configuran para ello.
- No hay servidor SFTP ni staging creados todavía: están preparados, no ejecutados.
