# Evidencias de regresión

## Línea base registrada
| Fecha | Comando | Resultado real |
|---|---|---|
| 2026-11-13 16:13 (-04:00) | `mvn clean test "-Djacoco.skip=true"` | `Tests run: 187, Failures: 0, Errors: 0, Skipped: 15` — `BUILD SUCCESS` (34 s). JaCoCo omitido con `jacoco.skip`. |

Las 15 pruebas omitidas no cuentan como aprobadas: corresponden a pruebas que requieren recursos externos (por ejemplo base de datos de integración). Verificar cuáles con `mvn test` sin la omisión y la salida de Surefire en `target/surefire-reports/`.

## Ejecución tras la mejora de la práctica
Completar con datos reales después de ejecutar (no dejar texto inventado).

| Fecha | Rama / commit | Comando | Ejecutadas | Fallos | Errores | Omitidas | Evidencia (captura / log) |
|---|---|---|---|---|---|---|---|
| 2026-11-15 19:31 | `feature/practica-agentes` | `mvn clean test "-Djacoco.skip=true"` | 192 | 0 | 0 | 15 | capturas/05-suite-completa.png |

## Casos manuales
| Caso | Rol / Usuario | Pasos / Verificación | Resultado esperado | Resultado real | Evidencia |
|---|---|---|---|---|---|
| Sesión por rol | `ADMIN`, `DIRECTOR`, `SECRETARIA`, `PROFESOR`, `ESTUDIANTE`, `PADRE` | Inicio de sesión con 2FA y comprobación de menús permitidos | Cada rol ve estrictamente sus opciones según `Permisos.java` | | |
| Calificación y boleta | `PROFESOR` / `ESTUDIANTE` | Registrar calificaciones de período activo y consultar boleta final | Notas ponderadas (30/30/40) calculadas y visibles en boleta | | |
| Bloqueo en período cerrado | `PROFESOR` | Intentar modificar calificaciones en un período lectivo con estado CERRADO | Edición bloqueada (campos solo lectura o rechazo con código/evento `NOTA_DENEGADA` en auditoría) | | |
| Corrección excepcional de notas | `ADMIN` | Ejecutar corrección en `/corregir-calificacion` con motivo >= 10 caracteres | Nota actualizada, auditoría `NOTA_CORREGIDA` (valor anterior, nuevo y motivo) e historial ajustado | | |
| `/health` | No autenticado | `GET /health` | Código HTTP 200 con estado OK / UP del servicio | | |
| SSO al Aula | `PROFESOR` / `ESTUDIANTE` | Clic en «Aula Virtual» desde SIGE | Redirección con token SSO válido manteniendo sesión (SameSite=Lax) | | |
| Auditoría y exportación | `ADMIN` / `DIRECTOR` | Realizar acción sensible (ej. cambio/corrección de nota) y exportar reporte | Registro visible con hora RD (America/Santo_Domingo) y exportación a Excel/CSV exitosa | | |
| Envío de avisos de credenciales | `ADMIN` | Crear nuevo personal (`DIRECTOR` o `SECRETARIA`) con correo válido (Gmail/Outlook) | Envío exitoso vía API Brevo / SMTP sin exponer contraseñas en texto claro | | |

## Evidencia de CI
Pegar los enlaces y evidencias correspondientes a los flujos automatizados de GitHub Actions:

| Workflow | Propósito | Enlace / Ejecución | Estado | Evidencia |
|---|---|---|---|---|
| `ci.yml` (Verify and package) | Compilación multiplataforma (Windows, Linux, macOS), pruebas unitarias y generación de artefactos portables | [PR #13](https://github.com/Invicto1013/Sistema-de-Gestion-Escolar-con-Java/pull/13) | Aprobado (linux, macos, windows) | capturas/06-ci-pr13.png |
| `cobertura.yml` (JaCoCo) | Análisis de cobertura de código y generación de informe HTML | [PR #13](https://github.com/Invicto1013/Sistema-de-Gestion-Escolar-con-Java/pull/13) | Aprobado | capturas/06-ci-pr13.png |
| `codeql.yml` (CodeQL) | Análisis de seguridad estática (SAST: prevención de SQLi, XSS, etc.) | [PR #13](https://github.com/Invicto1013/Sistema-de-Gestion-Escolar-con-Java/pull/13) | Aprobado | capturas/06-ci-pr13.png |

## Demostración: la regresión se detecta (rama `feature/practica-agentes`)
Prueba nueva: `PermisosRegresionTest` (fija la tabla completa de permisos por rol).

1. **Antes (línea base):** `mvn test "-Djacoco.skip=true" -Dtest=PermisosRegresionTest` → anotar el resultado real.
2. **Cambio que rompe a propósito (solo para la demostración, sin hacer commit):** en `Permisos.publicaAnuncios` añadir `Rol.PROFESOR` a la lista.
3. **Durante:** repetir el comando; debe **fallar** con «publicaAnuncios para PROFESOR». Guardar captura de la salida.
4. **Revertir:** `git restore src/main/java/com/unev/sige/model/Permisos.java`.
5. **Después:** ejecutar `mvn clean test "-Djacoco.skip=true"` completo y anotar el total en la tabla de arriba.

| Paso | Fecha | Resultado real | Evidencia |
|---|---|---|---|
| 1. Antes | 2026-11-15 19:09 | `PermisosRegresionTest`: 5 ejecutadas, 0 fallos, 0 errores. BUILD SUCCESS | capturas/01-antes.png |
| 2–3. Con el cambio roto (`Rol.PROFESOR` añadido a `publicaAnuncios`, sin commit) | 2026-11-15 19:25 | 5 ejecutadas, 1 fallo: `publicaAnuncios para PROFESOR ==> expected: <false> but was: <true>`. BUILD FAILURE | capturas/02-cambio-roto-y-fallo.png, capturas/03-fallo-resumen.png |
| 4. Revertido | 2026-11-15 (después de las 19:25) | `git restore` y `git diff` sin salida | capturas/04-revertido-diff-vacio.png |
| 5. Después (suite completa) | 2026-11-15 19:31 | 192 ejecutadas, 0 fallos, 0 errores, 15 omitidas. BUILD SUCCESS | capturas/05-suite-completa.png |

Nota: un primer intento a las 17:15 también falló como se esperaba, pero sus capturas se tomaron después de revertir y no mostraban la línea modificada; por eso la evidencia principal es la repetición de las 19:xx.

---

## Mejoras al protocolo de pruebas (casos definidos, ejecución pendiente)
Los puntos marcados `[x]` indican que el caso quedó **definido en este documento**; su ejecución manual está pendiente en la tabla «Casos manuales».

- [x] **Completitud y trazabilidad de casos manuales:** Se definieron roles exactos, pasos estructurados y resultados esperados para cada caso manual, alineados con `Permisos.java`.
- [x] **Estructuración de tabla de evidencias de CI:** Se especificó el propósito de cada pipeline (`ci.yml`, `cobertura.yml`, `codeql.yml`) con campos para enlaces, estado y captura verde.
- [x] **Regresión de período cerrado (Fase D):** Se incorporó la validación manual de bloqueo de edición de calificaciones para profesores en períodos lectivos con estado `CERRADO` y generación del evento `NOTA_DENEGADA`.
- [x] **Regresión de corrección excepcional de calificaciones:** Se añadió la comprobación de `/corregir-calificacion` garantizando auditoría con motivo mínimo de 10 caracteres (`NOTA_CORREGIDA`).
- [x] **Verificación de auditoría y zona horaria (Mejoras Fase D/E):** Se sumó el caso de prueba para constatar el registro de auditoría con zona horaria de República Dominicana (`America/Santo_Domingo`) y exportación funcional a CSV/Excel.
- [x] **Notificación de credenciales segura:** Se añadió verificación del flujo de correo vía Brevo/SMTP sin inclusión de contraseña en claro al dar de alta personal.
- [ ] **Aislamiento de pruebas de integración:** Configurar un perfil Maven específico (ej. `-P integration`) o contenedor de base de datos para ejecutar de forma reproducible las 15 pruebas actualmente marcadas como *Skipped*.
- [ ] **Automatización de cobertura en CI:** Reactivar JaCoCo en el flujo de CI para fijar un umbral mínimo de cobertura de código sobre `com.unev.sige.model` y controladores críticos.
- [ ] **Validación automatizada de contratos de API REST:** Implementar pruebas automáticas que verifiquen códigos HTTP y cabeceras de seguridad (`SameSite=Lax`, `Content-Security-Policy`, `X-Frame-Options`) en los endpoints clave.