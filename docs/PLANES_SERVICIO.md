# Planes de servicio

Suscripción del software por centro: **Esencial** (hasta 150 usuarios), **Estándar** (hasta 600) e **Institucional** (sin límite).

- Los planes, precios (RD$/mes de referencia) y límites viven en `CatalogoPlanes`; se ajustan ahí.
- El plan contratado se guarda en `suscripcion_centro` (una fila por cambio; vale la más reciente). Sin FK a `usuarios`, para no bloquear su borrado.
- Los límites son **informativos**: no bloquean el acceso. La pantalla muestra uso, barra de progreso y aviso «Cerca del límite» (≥ 90 %) o «Excede el límite».
- Roles: ADMIN ve y cambia el plan; DIRECTOR solo consulta. Web: `/planes`. Escritorio: «Planes de servicio».
- Activación: ejecutar `sql/fase_planes_servicio.sql` (idempotente; siembra «Estándar» solo si la tabla está vacía). Sin la tabla, la pantalla muestra el catálogo y un aviso.
- No incluye cobro en línea ni facturación: es el registro del plan y su uso.
