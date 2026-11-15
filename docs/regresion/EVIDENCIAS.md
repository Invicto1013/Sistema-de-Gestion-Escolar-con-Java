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
| | | | | | | | |

## Casos manuales
| Caso (ver matriz) | Fecha | Resultado (aprobado/fallido/no ejecutado) | Evidencia |
|---|---|---|---|
| Sesión por rol | | | |
| Calificación y boleta | | | |
| `/health` | | | |
| SSO al Aula | | | |

## Evidencia de CI
Pegar el enlace a la ejecución de GitHub Actions (`ci.yml`, `cobertura.yml`, `codeql.yml`) y una captura en verde.

## Demostración: la regresión se detecta (rama `feature/prueba-regresion-permisos`)
Prueba nueva: `PermisosRegresionTest` (fija la tabla completa de permisos por rol).

1. **Antes (línea base):** `mvn test "-Djacoco.skip=true" -Dtest=PermisosRegresionTest` → anotar el resultado real.
2. **Cambio que rompe a propósito (solo para la demostración, sin hacer commit):** en `Permisos.publicaAnuncios` añadir `Rol.PROFESOR` a la lista.
3. **Durante:** repetir el comando; debe **fallar** con «publicaAnuncios para PROFESOR». Guardar captura de la salida.
4. **Revertir:** `git restore src/main/java/com/unev/sige/model/Permisos.java`.
5. **Después:** ejecutar `mvn clean test "-Djacoco.skip=true"` completo y anotar el total en la tabla de arriba.

| Paso | Fecha | Resultado real | Evidencia |
|---|---|---|---|
| 1. Antes | | | |
| 3. Con el cambio roto | | | |
| 5. Después (suite completa) | | | |
