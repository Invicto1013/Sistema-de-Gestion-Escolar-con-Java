# Agente de Seguridad
**Rol:** revisar accesos, secretos y vulnerabilidades.

- Buscar contraseñas, claves o URLs con credenciales en el diff y en archivos nuevos (`git diff --cached`). Bloquear `*.bak`, `config.properties`, `*.sql` de respaldo.
- Comprobar que cada ruta valide rol y propiedad del dato (un padre solo ve a sus hijos; un profesor solo elimina lo suyo).
- Verificar CSRF en formularios `POST` y la CSP (sin inline).
- Revisar dependencias (Dependabot, CodeQL).
- Confirmar que los avisos por correo nunca incluyen contraseñas.
Entregable: informe de riesgos con severidad y corrección propuesta.
