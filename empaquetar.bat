@echo off
REM Genera el ejecutable de SIGE (Fase 9 de la rubrica).
REM   empaquetar.bat            -> JAR ejecutable + carpeta portable (jpackage app-image)
REM   empaquetar.bat exe        -> ademas, instalador .exe (requiere WiX Toolset 3.x o 4+)
REM Requisitos: JDK 21 (con jpackage) y Maven en el PATH, y config.properties en esta carpeta.
setlocal
cd /d "%~dp0"
set VERSION=1.0.0

if not exist config.properties (
  echo [AVISO] No existe config.properties. Copia config.example.properties como config.properties
  echo         y completa los datos de la base en la nube; se incluira junto al ejecutable.
  exit /b 1
)

echo == 1/3 Compilando y creando el JAR ejecutable ==
call mvn -B -q clean package -DskipTests
if errorlevel 1 ( echo [ERROR] Fallo Maven & exit /b 1 )

echo == 2/3 Preparando carpeta de entrega ==
if exist target\entrega rmdir /s /q target\entrega
mkdir target\entrega
copy /y target\sige-%VERSION%.jar target\entrega\sige.jar >nul
copy /y config.properties target\entrega\config.properties >nul

echo == 3/3 Creando ejecutable con jpackage ==
if exist target\dist rmdir /s /q target\dist
jpackage --type app-image --name SIGE --app-version %VERSION% --vendor "UNEV" ^
  --input target\entrega --main-jar sige.jar --main-class com.unev.sige.app.Launcher ^
  --java-options "-Xmx512m" --dest target\dist
if errorlevel 1 ( echo [ERROR] Fallo jpackage app-image & exit /b 1 )

if /i "%1"=="exe" (
  jpackage --type exe --name SIGE --app-version %VERSION% --vendor "UNEV" ^
    --input target\entrega --main-jar sige.jar --main-class com.unev.sige.app.Launcher ^
    --java-options "-Xmx512m" --win-menu --win-shortcut --win-dir-chooser --dest target\dist
  if errorlevel 1 ( echo [ERROR] Fallo el instalador. Instala WiX Toolset y reintenta & exit /b 1 )
)

echo.
echo Listo:
echo   JAR ejecutable : target\entrega\sige.jar  ^(+ config.properties en la misma carpeta^)
echo   App portable   : target\dist\SIGE\SIGE.exe
if /i "%1"=="exe" echo   Instalador     : target\dist\SIGE-%VERSION%.exe
endlocal
