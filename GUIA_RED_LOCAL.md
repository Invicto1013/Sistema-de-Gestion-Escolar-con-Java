# Acceso a SIGE Web desde otros dispositivos de la red

Esta guía permite abrir SIGE desde un teléfono o computadora conectados a la misma red privada que el equipo donde se ejecutan SIGE y MySQL. No publica el sistema en Internet.

## 1. Comprueba la conexión MySQL en el servidor

Inicia sesión en SIGE desde la computadora que ejecuta la aplicación. Si aparece `Public Key Retrieval is not allowed`, actualiza `db.url` en el archivo local `db.properties` con la URL apropiada para MySQL local:

```properties
db.url=jdbc:mysql://localhost:3306/sige_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Santo_Domingo
```

Esta opción es solo para desarrollo con MySQL local sin TLS. `db.properties` contiene credenciales y no debe compartirse ni subirse al repositorio.

## 2. Inicia el servidor escuchando en la red local

Detén la instancia anterior con `Ctrl+C`. En PowerShell, desde la carpeta del proyecto:

```powershell
$env:SIGE_WEB_HOST = "0.0.0.0"
$env:SIGE_WEB_PORT = "8081"
mvn -Pweb javafx:run
```

Mantén esa terminal abierta mientras utilizas SIGE.

## 3. Encuentra la dirección IP del equipo

Abre otra terminal PowerShell en la computadora servidor y ejecuta:

```powershell
Get-NetIPAddress -AddressFamily IPv4 |
  Where-Object { $_.IPAddress -notlike '127.*' -and $_.AddressState -eq 'Preferred' } |
  Select-Object IPAddress, InterfaceAlias
```

Usa la IP de la interfaz Wi-Fi o Ethernet de tu red privada. En el otro dispositivo, abre `http://IP_DEL_EQUIPO:8081/login`, sustituyendo `IP_DEL_EQUIPO` por esa dirección. Por ejemplo: `http://192.168.1.25:8081/login`.

## 4. Si la conexión es rechazada

En Windows Defender Firewall, crea una regla de entrada para TCP, puerto `8081`, perfil **Privado**. No la habilites para perfiles públicos. Verifica que ambos dispositivos estén en la misma red Wi-Fi y que el router no tenga activado el aislamiento entre dispositivos.

## Uso fuera de la red local

No reenvíes el puerto `8081` directamente a Internet. El servidor HTTP integrado está pensado para desarrollo y redes privadas. Para acceso público se necesita desplegarlo detrás de HTTPS, configurar cookies seguras y TLS para MySQL, proteger los secretos con variables de entorno y revisar controles de seguridad y operación antes de publicarlo.
