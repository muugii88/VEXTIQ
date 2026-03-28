@echo off
title VEXTIQ PRO - Setup
cd /d "%~dp0"

echo ========================================
echo   VEXTIQ PRO v11 - Initial Setup
echo ========================================
echo.

REM Check Java
echo Checking Java...
java -version 2>nul
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Java JDK 17+ required!
    echo Download from: https://adoptium.net/
    pause
    exit /b 1
)
echo [OK] Java found
echo.

REM Check Gradle
echo Checking Gradle...
where gradle >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Gradle found
    echo.
    echo ========================================
    echo   Running VEXTIQ PRO...
    echo ========================================
    echo.
    gradle run
    goto end
)

echo [ERROR] Gradle not found in PATH
echo Please check your PATH settings
pause

:end
