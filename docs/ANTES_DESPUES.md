# Antes y después: mejoras de la segunda fase

| Área | Antes | Después |
|---|---|---|
| Períodos lectivos | Texto libre en cada inscripción; cualquier valor era válido | Tabla `periodos_lectivos` con estado ABIERTO o CERRADO; la matrícula exige un período abierto |
| Notas | El profesor podía editarlas siempre | Un período cerrado bloquea las notas; solo el administrador corrige, con motivo obligatorio |
| Historial académico | La tabla `records_academicos` existía sin usarse | `historial_academico` guarda promedio, materias aprobadas y reprobadas y resultado de cada estudiante al cerrar el período |
| Trazabilidad | Sin registro de quién cambió qué | Tabla `auditoria` de solo añadir: notas, cierres, matrículas, secciones, roles y cuentas, incluidos los intentos denegados |
| Consulta de auditoría | No existía | Página web con filtros y paginación, CSV para Excel y tabla de escritorio |
| Secciones y docentes | Cambios sin registro | Altas, cambios de docente y bajas auditados, solo por el administrador |
| Matrícula y retiro | Sin control de período | Bloqueados en períodos cerrados y auditados |
| Aula virtual | Parte del mismo programa | Aplicación aparte con acceso único (SSO firmado), usuario de base de datos de mínimos privilegios y hora correcta |
| Horas mostradas | Hora UTC del servidor | Hora de República Dominicana en auditoría y cierres |
| Tablas de escritorio | Listas completas sin búsqueda | Búsqueda dinámica sin acentos, orden global, paginación de 10, 25 o 50 filas y exportación a PDF |
| Pruebas automáticas | Base de pruebas del proyecto | Además: reglas de período, zona horaria, paginación y generación de PDF; el SQL nuevo se verificó con un analizador de MySQL |
| Documentación | Guías de despliegue y DevOps | Además: diagrama entidad-relación, manual de usuario y casos de prueba de la Fase D |

## Evidencia
- Producción en Render con períodos, cierre, notas bloqueadas, reapertura con motivo, auditoría filtrada y CSV (capturas del 3 de noviembre de 2026).
- Casos de prueba en [PRUEBAS_FASE_D.md](PRUEBAS_FASE_D.md).
