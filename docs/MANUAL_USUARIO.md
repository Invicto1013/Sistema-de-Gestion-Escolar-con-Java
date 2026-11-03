# Manual de usuario de SIGE

SIGE tiene dos formas de uso con los mismos datos: la **versión web** (cualquier navegador) y la **aplicación de escritorio** (JavaFX). Hay cuatro roles: administrador, profesor, estudiante y padre.

## 1. Entrar al sistema

1. Abre la dirección web del SIGE o la aplicación de escritorio.
2. Escribe tu correo y tu contraseña.
3. Si tu cuenta tiene verificación en dos pasos, escribe el código de 6 dígitos de tu aplicación autenticadora.
4. Si es tu primer acceso con una contraseña temporal, el sistema te pedirá cambiarla.

Después de varios intentos fallidos la cuenta se bloquea un tiempo. Si olvidaste la contraseña, pide al administrador que la restablezca.

## 2. Administrador

| Quiero… | Dónde |
|---|---|
| Aprobar cuentas nuevas, cambiar roles, activar o desactivar cuentas, restablecer contraseñas | Usuarios y Solicitudes |
| Crear una asignatura y vincularla a un área y a grados | Asignaturas |
| Crear una sección, asignar docente, aula, horario y cupo | Secciones |
| Matricular a un estudiante en una sección | Inscripciones (solo permite períodos abiertos) |
| Crear, cerrar o reabrir un período | Períodos |
| Corregir una nota de un período cerrado | Calificaciones (con motivo obligatorio) |
| Consultar quién hizo qué, filtrar y descargar | Auditoría |

**Cerrar un período.** En Períodos, pulsa *Cerrar período*, revisa la vista previa (cuántos aprueban, reprueban o quedan incompletos), marca la confirmación y confirma. Se guarda el historial académico de cada estudiante y las notas y matrículas del período quedan bloqueadas.

**Reabrir un período.** Escribe un motivo (mínimo 10 caracteres), marca *Confirmar* y pulsa *Reabrir*. Queda en la auditoría.

**Corrección excepcional de una nota.** Solo en la versión web, en Calificaciones, sobre un período cerrado: cambia las notas, escribe el motivo (10 a 300 caracteres) y guarda. Se registra la nota anterior, la nueva, quién la cambió y por qué.

**Auditoría.** Filtra por texto, acción, resultado y fechas. En la web, *Exportar resultados CSV* descarga un archivo que abre directamente en Excel. En el escritorio, la tabla permite buscar y exportar a PDF.

## 3. Profesor

- **Mis secciones:** lista de las secciones asignadas.
- **Calificaciones:** registra parcial 1 (30 %), parcial 2 (30 %) y examen final (40 %). La nota final y la literal se calculan solas; se aprueba con 70 o más.
- **Asistencia:** marca presentes y ausentes por fecha.
- Si el período está **cerrado**, las notas aparecen bloqueadas. Solo el administrador puede corregirlas.
- **Aula virtual:** publica tareas, materiales y anuncios, califica entregas y responde comentarios.

## 4. Estudiante

- **Mis materias, Mis calificaciones, Mi asistencia y Boleta.**
- En la web puedes inscribirte en secciones con cupo dentro de un período abierto.
- **Aula virtual:** entra desde el botón del SIGE (acceso directo sin escribir otra contraseña), mira las tareas, sube tus entregas y consulta tus notas.

## 5. Padre o tutor

- **Mis hijos:** estudiantes vinculados a tu cuenta por el administrador.
- **Calificaciones, asistencia y boleta** de cada hijo. Solo ves los datos de tus hijos.

## 6. Tablas del escritorio: búsqueda, orden, páginas y PDF

En Secciones, Asignaturas, Períodos, Auditoría e Historial de sesiones:

- **Buscar:** escribe en el campo; la tabla se filtra mientras escribes, sin importar acentos ni mayúsculas. Varias palabras deben aparecer todas.
- **Ordenar:** haz clic en el encabezado de una columna; el orden aplica a todos los datos, no solo a la página visible.
- **Páginas:** elige 10, 25 o 50 filas y usa « ‹ › » para moverte.
- **Exportar PDF:** guarda todas las filas que cumplen la búsqueda actual en un PDF horizontal.

## 7. Preguntas frecuentes

**No puedo guardar una nota.** El período puede estar cerrado; pide al administrador una corrección.
**No puedo matricular.** Debe existir un período abierto y la sección debe tener cupo.
**La hora de la auditoría no coincide.** Se muestra en hora de República Dominicana (-04:00).
**El Aula tarda en abrir.** El servicio gratuito se duerme tras 15 minutos sin uso; espera unos segundos y reintenta.
