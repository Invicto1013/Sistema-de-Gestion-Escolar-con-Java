# Eliminar usuario (solo ADMIN)

**Dónde:** Usuarios → fila de la cuenta → "Eliminar cuenta…" (no aparece en tu propia fila).

**Qué hace:** borra la cuenta y sus datos en una sola transacción (si algo falla, no se borra nada).

| Rol | Se borra |
|---|---|
| ESTUDIANTE | inscripciones, notas, asistencia, historial, récords, reportes, conducta, vínculos con padres, entregas del aula y sus archivos |
| PROFESOR | ficha, materiales del aula (con las entregas recibidas y sus archivos), observaciones de conducta, anuncios; sus secciones quedan **sin profesor** (no se borran) |
| PADRE | ficha y vínculos con estudiantes (los estudiantes no se tocan) |
| ADMIN / DIRECTOR / SECRETARIA | cuenta, anuncios y sesiones |
| Todos | sesiones web, historial de sesiones y accesos (incluido el correo en intentos fallidos) |

**Auditoría:** se conserva. Las filas antiguas quedan sin vínculo (`id_usuario` NULL) y el correo se
sustituye por `[cuenta eliminada]` en su texto. Se añade `CUENTA_ELIMINADA` con solo id y rol.

**Protecciones:** solo ADMIN (se vuelve a comprobar en la base de datos); hay que escribir el correo exacto
de la cuenta; nadie puede eliminarse a sí mismo (así nunca se elimina al último administrador).

**Código:** `EliminacionUsuarioDAO` (borrado explícito, sin depender de `ON DELETE CASCADE`, que en TiDB
puede no aplicarse) y `WebApp.deleteUser` (ruta `POST /eliminar-usuario`).

**Pruebas:** `EliminacionUsuarioDAOTest` (sin BD) y `EliminacionUsuarioDAOIntegracionTest` (MySQL local).
Prueba manual sugerida: crear un estudiante de prueba, inscribirlo y calificarlo, eliminarlo y comprobar que
desaparece de Usuarios, boletas y reportes, y que Auditoría muestra `CUENTA_ELIMINADA`.
