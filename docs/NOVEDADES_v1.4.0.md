# SIGE v1.4.0 — Novedades

Añade los roles de director/a y secretario/a, la creación de personal por el administrador y los avisos por correo, sobre la v1.3.0.

## Roles y permisos
- **Director/a** y **secretario/a** como nuevos roles, con menú y funciones propias (informes, anuncios, horarios, solicitudes, vínculos, auditoría y conducta según el rol).
- El administrador **crea perfiles de personal** desde la web y desde la aplicación de escritorio.
- Permisos centralizados y probados (`Permisos`, `PermisosTest`).

## Avisos por correo al crear perfiles
- Envío por **Brevo** (API HTTPS, apta para Render gratis), por **SMTP** o de forma **manual** desde el correo del administrador.
- Solo a correos personales (Gmail, Outlook, Hotmail, Live, MSN); **nunca se envía la contraseña**.
- Variables de entorno: `SIGE_BREVO_API_KEY`, `SIGE_MAIL_FROM`, `SIGE_MAIL_FROM_NAME` (y `SIGE_SMTP_*`).

## Interfaz
- Icono **❓ Acerca de** con la información del proyecto, el autor (Elian Castro) y la institución (Universidad Nacional Evangélica, UNEV), también en el pie de página, README y créditos.
- Menú lateral de escritorio con **desplazamiento** y formularios web mejor organizados.

## Infraestructura
- Base de datos migrada a **TiDB Cloud Starter** (MySQL compatible, TLS obligatorio). Configuración por `SIGE_DB_URL`, `SIGE_DB_USER` y `SIGE_DB_PASSWORD`.
- `*.bak` ignorados en Git para no subir copias de configuración con credenciales.

## Actualizar una instalación existente
Vuelve a empaquetar el ejecutable (`empaquetar.bat`) si cambiaste `config.properties`. No hay scripts SQL nuevos.
