@echo off
title VEXTIQ PRO - Kotlin Build
cd /d "%~dp0"

echo ========================================
echo   VEXTIQ PRO v11 - Kotlin Edition
echo ========================================
echo.

echo Checking Java...
java -version 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java not found!
    echo Download JDK 17+ from: https://adoptium.net/
    pause
    exit /b 1
)

echo.
echo Running VEXTIQ PRO...

REM Try gradlew first
if exist "gradlew.bat" (
    if exist "gradle\wrapper\gradle-wrapper.jar" (
        call gradlew.bat run
        goto end
    )
)

REM Try system gradle
gradle --version >nul 2>&1
if %errorlevel% equ 0 (
    gradle run
    goto end
)

REM Neither available - show instructions
echo.
echo [!] Gradle not found. Please install:
echo.
echo Option 1: Download Gradle Wrapper
echo   1. Go to: https://gradle.org/releases/
echo   2. Download gradle-8.5-bin.zip
echo   3. Extract to this folder
echo   4. Run: gradle wrapper
echo.
echo Option 2: Install Gradle
echo   1. Download from: https://gradle.org/install/
echo   2. Add to PATH
echo   3. Run this script again
echo.
pause

:end
