# Simulación de transferencia SFTP (WinSCP)

SIGE se publica en Render por Git, así que esta carpeta documenta el procedimiento SFTP pedido en la guía como **práctica en un servidor de pruebas**.

1. Instalar WinSCP y crear una sesión guardada llamada `SFTP-PRUEBAS` (host, puerto, usuario de despliegue con permisos limitados, clave o contraseña guardadas en WinSCP, no en Git). Aceptar y verificar la huella del servidor.
2. Construir el artefacto: `.\empaquetar.bat` (genera `target\entrega`).
3. Ejecutar solo la vista previa:
   ```powershell
   winscp.com /script="deploy\winscp\deploy-preview.txt" /log="deploy.log"
   ```
4. Revisar `deploy.log` y el listado. Guardar el log como evidencia (sin credenciales).
5. Solo tras autorización en staging, crear un script aparte sin `-preview`. Nunca añadir `-delete` por defecto ni sincronizar datos persistentes.

Estado: preparado; **no ejecutado** hasta disponer de un servidor SFTP de prácticas.
