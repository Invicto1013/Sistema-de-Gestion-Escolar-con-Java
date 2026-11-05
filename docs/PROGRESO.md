# Avance del proyecto SIGE

**88.0 %** `██████████████████████████░░░░`

Actualizado el 2026-11-05. Meta: Demo del viernes 6 de noviembre de 2026.

El porcentaje es la suma de peso × avance de cada elemento, dividida entre la suma de pesos (92.4 de 105 puntos). Un cambio nuevo se agrega con su peso y sube el porcentaje al terminarse y al verificarse en producción. Se recalcula con `python docs/progreso.py` desde `docs/progreso.json`.

## Resumen por área

| Área | Puntos logrados | Puntos totales | Avance |
| --- | ---: | ---: | ---: |
| Base | 43.0 | 43 | 100 % |
| Fase D | 18.0 | 18 | 100 % |
| Fase E | 7.0 | 8 | 88 % |
| Fase F | 8.0 | 8 | 100 % |
| Seguridad | 7.4 | 11 | 68 % |
| Pendiente | 4.0 | 12 | 33 % |
| Mejoras | 5.0 | 5 | 100 % |

## Detalle

| Área | Elemento | Peso | Avance |
| --- | --- | ---: | ---: |
| Base | Núcleo del sistema: roles, inicio de sesión con 2FA, usuarios, secciones, inscripciones, notas, asistencia y boletas | 17 | 100 % |
| Base | Lista de 18 mejoras | 8 | 100 % |
| Base | DevOps y pruebas automáticas (CI, informe, respaldos) | 8 | 100 % |
| Base | Aula virtual separada, acceso único (SSO) y despliegue en Render | 10 | 100 % |
| Fase D | Períodos lectivos, cierre, bloqueo de notas, auditoría e historial académico (código) | 12 | 100 % |
| Fase D | Fase D verificada en producción (casos de prueba 1 a 8) | 6 | 100 % |
| Fase E | Tablas de escritorio: búsqueda, orden, paginación y PDF (código) | 6 | 100 % |
| Fase E | Fase E verificada en el escritorio (Auditoría probada; faltan Secciones y Asignaturas) | 2 | 50 % |
| Fase F | Documentación: diagrama ER, manual, antes y después, casos de prueba | 8 | 100 % |
| Seguridad | Revisión de los 20 puntos y correcciones (archivos, CodeQL, Dependabot, contraseñas) | 7 | 100 % |
| Seguridad | Pruebas de seguridad en producción S1 a S9 (hecha: S6) | 4 | 11 % |
| Pendiente | Verificación del correo en el registro | 3 | 0 % |
| Pendiente | Corrección excepcional de notas también en el escritorio | 2 | 0 % |
| Pendiente | Ensayo completo de la demo con el período 2026-DEMO (realizado con éxito) | 4 | 100 % |
| Pendiente | Evidencia con capturas integrada en docs/PRUEBAS_FASE_D.md | 3 | 0 % |
| Mejoras | Hora de República Dominicana en auditoría y cierres, y CSV para Excel | 1 | 100 % |
| Mejoras | Nombre correcto del PDF exportado (auditoria.pdf) | 1 | 100 % |
| Mejoras | Exportación ejecutiva a Excel de la auditoría (hoja de registros y hoja de resumen) | 2 | 100 % |
| Mejoras | Sesión de SIGE se mantiene al volver desde el Aula (cookie SameSite=Lax) y verificada | 1 | 100 % |

## Historial de avance

| Fecha | Cambio | Avance total |
| --- | --- | ---: |
| 2026-11-04 | Punto de partida del tablero (Fases D, E y F entregadas; seguridad revisada) | 83.4 % |
| 2026-11-04 | Hora de República Dominicana y CSV para Excel | 83.6 % |
| 2026-11-04 | Nombre correcto del PDF exportado | 83.8 % |
| 2026-11-04 | Exportación ejecutiva a Excel de la auditoría | 84.1 % |
| 2026-11-05 | Sesión se mantiene al volver desde el Aula | 84.2 % |
| 2026-11-05 | Ensayo completo de la demo realizado con éxito | 88.0 % |
