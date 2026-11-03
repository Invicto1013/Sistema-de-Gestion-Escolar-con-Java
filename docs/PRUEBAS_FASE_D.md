# Pruebas de la Fase D: períodos, bloqueo de notas y auditoría

Prerrequisito: haber ejecutado `sql/fase_periodos_auditoria.sql`. Usuarios: un ADMIN, un PROFESOR con sección asignada y un ESTUDIANTE. Período de ejemplo: `2026-1`.

| # | Tipo | Caso | Pasos | Resultado esperado |
|---|------|------|-------|--------------------|
| 1 | Éxito | Crear período | Admin, Períodos, nombre `2026-1`, fechas válidas | Aparece ABIERTO; auditoría `PERIODO_CREADO` |
| 2 | Éxito | Matricular y registrar notas | Admin matricula al estudiante en una sección de `2026-1`; el profesor guarda parciales y examen | Nota final calculada (30/30/40); auditoría `MATRICULA` y `NOTA_GUARDADA` |
| 3 | Éxito | Cerrar período | Admin, Cerrar período, confirmar vista previa | Estado CERRADO; historial por estudiante con resultado; auditoría `PERIODO_CERRADO` |
| 4 | Validación | Período duplicado o inválido | Crear `2026-1` otra vez; crear `26-1`; fin anterior al inicio | Mensaje de error y no se crea nada |
| 5 | Acceso no autorizado | Profesor edita nota en período cerrado | Profesor intenta guardar notas de `2026-1` | Rechazado (campos de solo lectura o error); auditoría `NOTA_DENEGADA` con resultado DENEGADO |
| 6 | Acceso no autorizado | No admin intenta corregir | Profesor envía POST a `/corregir-calificacion` | Rechazado; auditoría `CORRECCION_NO_AUTORIZADA` |
| 7 | Validación | Corrección sin motivo válido | Admin corrige con motivo vacío o menor de 10 caracteres | Mensaje de error; la nota no cambia |
| 8 | Éxito | Corrección excepcional y consulta | Admin corrige con motivo válido; abrir Auditoría y descargar CSV | Nota actualizada; auditoría `NOTA_CORREGIDA` con valor anterior, nuevo y motivo; historial actualizado; el CSV incluye la fila |

Casos adicionales recomendados: matricular en período cerrado (rechazado), retirar materia en período cerrado (rechazado), reabrir con motivo (auditoría `PERIODO_REABIERTO`).

Registrar para cada caso: fecha, usuario, resultado obtenido y captura de pantalla.
