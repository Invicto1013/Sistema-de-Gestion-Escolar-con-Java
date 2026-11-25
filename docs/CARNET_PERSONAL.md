# Carnet de identidad del personal

Además del carnet estudiantil, SIGE genera el carnet de **profesores, dirección y secretaría**, con el mismo diseño imprimible y un color por rol.

| Rol | Color | Área en el carnet | Puede ver |
| --- | --- | --- | --- |
| Profesor | verde azulado | Personal docente | el suyo |
| Director/a | azul | Dirección del centro | el suyo y el de cualquier miembro del personal |
| Secretario/a | morado | Secretaría | el suyo y el de cualquier miembro del personal |
| Administrador | n/a (no tiene carnet propio) | n/a | el de cualquier miembro del personal |

- **Contenido:** nombre, cargo, `ID SIGE` (formato `P-00042`, a partir del id de usuario), área, ciclo y QR con `SIGE:PERSONAL:<ROL>:<id>`. No incluye cédula, teléfono ni correo.
- **Web:** `/carnet-personal` (el propio) y `/carnet-personal?usuario=<id>` (solo admin, dirección y secretaría); `/personal-carnets` lista el personal activo. Sin sesión no entrega nada (401); si un profesor pide el carnet de otra persona, 403; un id que no es personal, 404.
- **Escritorio:** «Mi carnet» (profesor) o «Carnets del personal» (admin, dirección y secretaría); se abre en el navegador para imprimir.
- **Reglas:** `Permisos.tieneCarnetPersonal` y `Permisos.emiteCarnetsDelPersonal`; generador `GeneradorDocumentos.generarCarnetPersonalHtml` (comparte plantilla con el carnet estudiantil, que no cambia).
- No requiere cambios en la base de datos.
