@echo off
setlocal
cd /d "%~dp0"

call gradlew.bat --no-daemon clean build
if errorlevel 1 exit /b %errorlevel%

if not exist "dist" mkdir "dist"
copy /y "build\libs\voxy-compatibility-patch-1.20.1-1.0.2.jar" "dist\voxy-compatibility-patch-1.20.1-1.0.2.jar" >nul
certutil.exe -hashfile "dist\voxy-compatibility-patch-1.20.1-1.0.2.jar" SHA256

echo Build complete: %~dp0dist\voxy-compatibility-patch-1.20.1-1.0.2.jar
