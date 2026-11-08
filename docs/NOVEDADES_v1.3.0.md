# SIGE v1.3.0 — Novedades

Añade las extensiones escolares (web y escritorio) sobre la v1.2.0, con permisos por rol y correcciones de seguridad.

## Extensiones escolares
- **Tablón de anuncios y circulares:** comunicados con prioridad (normal, importante, urgente) y destinatarios por rol; solo el administrador publica y elimina.
- **Horarios escolares y aulas:** bloques semanales por sección, con aviso de choque de horario del profesor.
- **Cuaderno de conducta y méritos:** mérito, tardanza, tarea pendiente, falta disciplinaria y nota informativa, visibles para estudiante, padre/tutor, profesor y administración.
- **Boleta oficial imprimible / PDF** y **carnet estudiantil con código QR.**
- Paneles equivalentes en la aplicación de escritorio.
- Nuevas tablas en `sql/fase_extensiones_escolares.sql` (el esquema completo pasa a 29 tablas).

## Seguridad
- Un padre/tutor solo accede al carnet, la boleta oficial y la conducta de sus propios hijos.
- Un profesor solo elimina las observaciones de conducta que él registró.
- Los valores de prioridad, destinatarios y tipo se validan en el servidor.
- El borrado de anuncios, horarios y observaciones pide confirmación en una pantalla propia (compatible con la política de seguridad del sitio).

## Calidad y herramientas
- JaCoCo 0.8.15: compatible con Java 26 (el proyecto sigue compilando para Java 21).
- Corrección de la boleta oficial (porcentajes en la plantilla HTML).
- `dependabot.yml` válido (Maven, GitHub Actions y Docker).

## Actualizar una instalación existente
Ejecuta `sql/fase_extensiones_escolares.sql` en la base de datos antes de abrir la nueva versión.
