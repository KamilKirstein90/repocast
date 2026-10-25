# Builds a signed release APK and publishes it as GitHub release "v<versionCode>".
# The app's update check compares that tag with its own versionCode.
$ErrorActionPreference = "Stop"
$env:JAVA_HOME = "$HOME\.jdks\ms-21.0.8"
Set-Location $PSScriptRoot

# One-time: create the signing key. Keep a backup of it - updates must be signed with the same key.
$keystore = "$HOME\.android\repocast-release.jks"
if (-not (Test-Path keystore.properties)) {
    $password = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 24 | ForEach-Object { [char]$_ })
    & "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -keystore $keystore -alias repocast -keyalg RSA -keysize 2048 `
        -validity 10000 -storepass $password -keypass $password -dname "CN=RepoCast, O=Kamil Kirstein"
    @"
storeFile=$($keystore -replace '\\', '/')
storePassword=$password
keyAlias=repocast
keyPassword=$password
"@ | Set-Content -Encoding ascii keystore.properties
    Write-Host "Created signing key $keystore (password in android\keystore.properties) - back both up!"
}

$latest = gh release list --repo KamilKirstein90/repocast --limit 1 --json tagName --jq ".[0].tagName" 2>$null
$versionCode = if ($latest -match '^v(\d+)$') { [int]$Matches[1] + 1 } else { 1 }

.\gradlew.bat assembleRelease "-PversionCode=$versionCode" --console=plain
if ($LASTEXITCODE -ne 0) { throw "Build failed" }

$apk = "app\build\outputs\apk\release\RepoCast-v$versionCode.apk"
Copy-Item app\build\outputs\apk\release\app-release.apk $apk -Force
gh release create "v$versionCode" $apk --repo KamilKirstein90/repocast --title "RepoCast $versionCode" --notes "Android-App, Version 1.0.$versionCode"
Write-Host "Published v$versionCode"
