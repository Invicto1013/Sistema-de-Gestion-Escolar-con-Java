# Checklist de entrega y guion de defensa — Práctica de agentes IA (SIGE)

Autor: Elian Castro · Universidad Nacional Evangélica (UNEV)
Marcar cada casilla solo cuando exista la evidencia. Estado `[x]` = hecho y comprobado; `[ ]` = pendiente.

## 1. Checklist por criterio de la rúbrica (10 puntos)

### Estructura del proyecto y reglas de trabajo (2)
- [x] `AGENTS.md` en la raíz con las reglas del proyecto.
- [x] Carpetas `agents/`, `docs/`, `deploy/`, `.github/workflows/`.
- [ ] Captura del repositorio en GitHub mostrando esa estructura en la rama de la práctica.

### Instrucciones de agentes y responsabilidades (2)
- [x] Siete agentes en `agents/`: orquestador, protector, regresión, trazabilidad, integrador, deploy y seguridad.
- [ ] Un ejemplo de uso real: copiar a la entrega un fragmento de cómo el agente Protector aplicó al cambio (prueba nueva sobre `Permisos`) su lista «antes / durante / después».

### Pruebas reales de regresión y protección (2)
- [x] `docs/regresion/MATRIZ_REGRESION.md` (12 funciones con sus pruebas).
- [x] `PermisosRegresionTest` (5 pruebas) ejecutada.
- [x] Línea base: 187 pruebas, 0 fallos, 0 errores, 15 omitidas (2026-11-13).
- [x] Cambio roto a propósito: falló con `publicaAnuncios para PROFESOR` (2026-11-15 17:15, sin commit).
- [x] Suite completa tras revertir: 192 pruebas, 0 fallos, 0 errores, 15 omitidas (2026-11-15 17:17).
- [ ] Capturas de los pasos 1, 3 y 5, y del `git diff` vacío tras la reversión.

### Automatización y despliegue seguro (2)
- [x] CI existente: `ci.yml`, `codeql.yml`, `cobertura.yml`, `integration.yml`; `release.yml` para publicar.
- [x] `docs/deploy/PLAN_DEPLOY_ROLLBACK.md` con entornos, circuito de 9 etapas, respaldo y rollback.
- [x] `deploy/winscp/deploy-preview.txt` (vista previa, sin `-delete`) y su instructivo.
- [ ] Pull Request abierto hacia `main` con los checks en verde (enlace y captura).
- [ ] (Opcional) Servidor SFTP de prácticas y ejecución real de la vista previa con su `deploy.log`.
- [ ] (Opcional) Staging: segundo servicio en Render con la rama `staging` y base `sige_staging`.

### Trazabilidad, evidencias y defensa técnica (2)
- [x] `docs/INCIDENTES.md` con 8 incidentes reales y 2 decisiones técnicas.
- [x] `docs/change-log.md` con las versiones.
- [ ] Completar fechas y commits marcados «completar»: `git log --date=short --format="%ad %h %s"`.
- [ ] `EVIDENCIAS.md` con las tablas llenas y el enlace del CI.
- [ ] Practicar la defensa (sección 3).

## 2. Qué NO está hecho (decirlo en la defensa)
- No existe aún servidor SFTP ni staging: están **preparados y documentados, no ejecutados**.
- Los agentes son **plantillas de instrucciones**. Las ejecutan herramientas como Claude Code, Antigravity o Copilot si se configuran con permisos; el archivo solo no actúa.
- Las 15 pruebas omitidas son de integración con base de datos: no cuentan como aprobadas.
- Escritorio (JavaFX): las pruebas automáticas cubren el modelo y la lógica; las pantallas se verifican a mano.

## 3. Guion de defensa (5–7 minutos)
1. **Problema (30 s):** mejorar un sistema existente sin romper lo que ya funcionaba.
2. **Reglas y agentes (1 min):** mostrar `AGENTS.md` y explicar que cada agente tiene un rol, entregables y límites; el humano aprueba producción.
3. **Demostración de regresión (2 min):** mostrar las capturas: la prueba pasa, se rompe `publicaAnuncios` a propósito, la prueba falla con el mensaje exacto, se revierte y la suite completa vuelve a verde (192 pruebas).
4. **CI y despliegue (1 min):** PR con checks, flujo de 9 etapas, respaldo antes de publicar, rollback en Render.
5. **Trazabilidad (1 min):** `INCIDENTES.md`; contar un caso real, por ejemplo INC-005 (un `.bak` con credenciales casi se sube; se detectó en `git status`, no entró al historial y se añadió `*.bak` a `.gitignore`).
6. **Límites y cierre (30 s):** lo que falta (SFTP/staging reales) y que los agentes asisten, no reemplazan el juicio del programador.

## 4. Preguntas probables y respuestas
1. **¿Qué diferencia hay entre AGENTS.md y un agente que ejecuta acciones?** El archivo son instrucciones; el agente es la herramienta con permisos que las lee y corre comandos. Sin herramienta no hay acción.
2. **¿Por qué quien escribe el código no aprueba su despliegue?** Para separar funciones: Regresión, Seguridad, Integrador y una persona revisan lo que el Desarrollador hizo.
3. **¿Qué evidencia prueba que una prueba se ejecutó?** La salida de Maven con los conteos y la hora, `target/surefire-reports`, y el CI en GitHub Actions con enlace.
4. **¿Por qué falló a propósito la prueba?** Para demostrar que la red de seguridad funciona: si alguien da a PROFESOR el permiso de publicar anuncios sin querer, la prueba lo detecta antes del despliegue.
5. **¿Qué pasó en el primer intento de romperla?** La ejecución de las 17:08 pasó porque el cambio no estaba guardado: Maven dijo «Nothing to compile». Se detectó leyendo el log y se repitió con `mvn clean test`; la de las 17:15 sí falló. Lección: comprobar con `git diff` antes de afirmar un resultado.
6. **¿Cómo se evita publicar configuraciones sensibles?** `.gitignore` (`config.properties`, `*.bak`), variables de entorno en Render, revisar `git status` y `git diff --cached` antes de cada commit.
7. **¿Qué requisitos tiene un rollback seguro?** Versiones anteriores conservadas, respaldo de la base antes de migrar, migraciones reversibles o respaldo previo, y el procedimiento probado.
8. **¿Cuándo es indispensable la aprobación humana?** Producción, migraciones destructivas, cambios de permisos y fusión a `main`.
9. **¿Por qué SFTP y no FTP?** FTP envía usuario y contraseña sin cifrar; SFTP usa SSH.
10. **¿Cómo puede una mejora en un módulo romper otro?** Porque comparten reglas: en SIGE, cambiar la escala de calificaciones afecta promedios y boletas; cambiar `Permisos` afecta la web y el escritorio a la vez. Por eso existe la matriz.
11. **¿Qué harías si el deploy falla en producción?** Rollback al despliegue estable en Render, comprobar `/health`, restaurar la base solo si hubo migración, y registrar el incidente.
