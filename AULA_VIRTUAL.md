# Aula virtual y entregas

En SIGE Web, profesores y estudiantes encuentran el enlace **Aula y tareas** en la navegación y en sus perfiles.

1. Ejecuta `sql/aula_virtual.sql` en la base `sige_db`.
2. Inicia SIGE Web. El profesor puede publicar materiales de clase o tareas en una de sus secciones asignadas; la fecha límite es opcional.
3. El estudiante ve lo publicado en las secciones donde está inscrito y usa **Subir entrega** para adjuntar su archivo. Puede volver a subir una versión corregida; se conserva la última entrega.
4. El profesor puede descargar cada entrega que corresponda a una de sus secciones.

Se admiten todos los formatos como datos adjuntos. Por seguridad, no se ejecutan ni se muestran dentro del navegador: se descargan como archivo adjunto. El límite es **20 MB por archivo**. El almacenamiento usa identificadores aleatorios fuera del árbol público de la aplicación; la ruta por defecto es `data/uploads`. En un servidor persistente se puede configurar `SIGE_UPLOAD_DIR` para apuntar a un volumen privado con espacio y copias de seguridad. En hosting efímero, los archivos locales pueden perderse al reiniciar o redesplegar; configura almacenamiento persistente antes de publicar SIGE en producción.

Los registros de materiales y entregas se guardan en MySQL. Cada lectura y descarga vuelve a comprobar rol, sección, inscripción o propiedad del archivo.
