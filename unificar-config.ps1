# Unifica la configuracion de SIGE en config.properties. Ejecutar UNA vez, en la carpeta del proyecto
# (la que tiene pom.xml):   powershell -ExecutionPolicy Bypass -File .\unificar-config.ps1
$ErrorActionPreference = 'Stop'
if (-not (Test-Path .\pom.xml)) { throw 'Ejecuta este script en la carpeta del proyecto (donde esta pom.xml).' }
$utf8 = New-Object System.Text.UTF8Encoding($false)

# 1) Seguridad: si config.properties ya esta en Git, sacarlo del indice y avisar.
$rastreado = git ls-files config.properties
if ($rastreado) {
    git rm -q --cached config.properties
    Write-Host ''
    Write-Host '!!! config.properties ESTABA SUBIDO A GIT. Se quito del indice, pero sigue en el historial.' -ForegroundColor Red
    Write-Host '!!! Cambia la contrasena de la base de datos en Aiven y revisa la clave 2FA (ver mensaje de Claude).' -ForegroundColor Red
    Write-Host ''
}

# 2) Nombre antiguo -> nombre oficial.
if ((Test-Path .\db.properties) -and -not (Test-Path .\config.properties)) {
    Rename-Item .\db.properties config.properties
    Write-Host 'db.properties renombrado a config.properties'
} elseif ((Test-Path .\db.properties) -and (Test-Path .\config.properties)) {
    Write-Host 'AVISO: existen db.properties y config.properties. Revisa que config.properties tenga tus datos y borra db.properties.' -ForegroundColor Yellow
}

# 3) La plantilla antigua ya esta fusionada en config.example.properties.
git rm -q -f --ignore-unmatch db.properties.example | Out-Null

# 4) Que Git y Docker ignoren config.properties.
foreach ($f in @('.gitignore', '.dockerignore')) {
    $texto = if (Test-Path $f) { [IO.File]::ReadAllText((Resolve-Path $f).Path, $utf8) } else { '' }
    if ($texto -notmatch '(?m)^config\.properties\s*$') {
        $sep = if ($texto -and -not $texto.EndsWith("`n")) { "`r`n" } else { '' }
        [IO.File]::WriteAllText((Join-Path (Get-Location).Path $f), $texto + $sep + "config.properties`r`n", $utf8)
        Write-Host "$f : agregado config.properties"
    }
}

# 5) Actualizar nombres en documentos y scripts (sin tocar tildes: se lee y escribe UTF-8).
$archivos = @(Get-ChildItem -File -Include *.md, *.bat, *.sh, *.yml -Recurse -Path . |
    Where-Object { $_.FullName -notmatch '\\(target|\.git|node_modules)\\' })
foreach ($a in $archivos) {
    $t = [IO.File]::ReadAllText($a.FullName, $utf8)
    $n = $t.Replace('db.properties.example', 'config.example.properties').Replace('db.properties', 'config.properties')
    if ($n -ne $t) {
        [IO.File]::WriteAllText($a.FullName, $n, $utf8)
        Write-Host ('actualizado: ' + $a.FullName.Substring((Get-Location).Path.Length + 1))
    }
}

Write-Host ''
Write-Host 'Archivos de configuracion que Git sigue (deben ser solo las plantillas):'
git ls-files | Select-String -Pattern 'properties'
