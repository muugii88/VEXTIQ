@echo off
:: VEXTIQ PRO - Run as Administrator
:: This script ensures VEXTIQ runs with admin rights for system optimization

:: Check for admin rights
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo Requesting Administrator privileges...
    powershell -Command "Start-Process '%~f0' -Verb RunAs"
    exit /b
)

:: We have admin rights, run VEXTIQ
title VEXTIQ PRO v2.0 - Administrator
cd /d "%~dp0"

echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║        VEXTIQ PRO v2.0 - Running as Administrator         ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.

:: Check Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found!
    echo Download JDK 21 from: https://adoptium.net/
    pause
    exit /b 1
)

echo [OK] Running with Administrator privileges
echo [>>] Starting VEXTIQ PRO...
echo.

:: Run with gradle
set GRADLE_PATH=C:\gradle-9.4.1\bin\gradle.bat
if exist "%GRADLE_PATH%" (
    call "%GRADLE_PATH%" run
) else (
    if exist "gradlew.bat" (
        call gradlew.bat run
    ) else (
        gradle run
    )
)

pause
