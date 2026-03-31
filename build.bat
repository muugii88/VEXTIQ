@echo off
chcp 65001 >nul
title VEXTIQ PRO V2.0 - Build System
color 0B

:: Check for admin rights
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo [!] Requesting Administrator privileges...
    powershell -Command "Start-Process '%~f0' -Verb RunAs"
    exit /b
)

echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║        VEXTIQ PRO V2.0 - BUILD SYSTEM (Administrator)     ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.

:: Check for Gradle
set GRADLE_PATH=C:\gradle-9.4.1\bin\gradle.bat
if not exist "%GRADLE_PATH%" (
    echo [!] Gradle not found at %GRADLE_PATH%
    echo [i] Trying system gradle...
    set GRADLE_PATH=gradle
)

:: Check/Download PresentMon
set RESOURCES_DIR=%~dp0src\main\resources
set PRESENTMON_PATH=%RESOURCES_DIR%\PresentMon.exe
if not exist "%PRESENTMON_PATH%" (
    echo [i] PresentMon.exe not found in resources
    echo [>>] Downloading PresentMon...
    powershell -NoProfile -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://github.com/GameTechDev/PresentMon/releases/download/v1.10.0/PresentMon-1.10.0-x64.exe' -OutFile '%PRESENTMON_PATH%' -UseBasicParsing" 2>nul
    if exist "%PRESENTMON_PATH%" (
        echo [OK] PresentMon downloaded successfully!
    ) else (
        echo [!] Failed to download PresentMon. FPS monitoring may not work.
        echo [i] You can manually download from: https://github.com/GameTechDev/PresentMon/releases
    )
) else (
    echo [OK] PresentMon.exe found in resources
)

:menu
echo.
echo  [1] Run App (Development)
echo  [2] Build MSI (Installer)
echo  [3] Clean Build
echo  [4] Build + Run
echo  [5] Exit
echo.
set /p choice="Select option [1-5]: "

if "%choice%"=="1" goto run
if "%choice%"=="2" goto package
if "%choice%"=="3" goto clean
if "%choice%"=="4" goto buildrun
if "%choice%"=="5" goto end
goto menu

:run
echo.
echo [>>] Starting VEXTIQ PRO...
echo.
call %GRADLE_PATH% run
goto menu

:package
echo.
echo [>>] Stopping any running VEXTIQ instances...
taskkill /F /IM "VEXTIQ PRO.exe" /T >nul 2>&1
taskkill /F /IM "PresentMon.exe" /T >nul 2>&1
taskkill /F /IM java.exe /FI "WINDOWTITLE eq VEXTIQ*" >nul 2>&1
taskkill /F /IM javaw.exe >nul 2>&1
timeout /t 2 >nul

echo [>>] Cleaning deep build directory for fresh installer...
rmdir /s /q build\compose 2>nul
rmdir /s /q build\kotlin 2>nul
rmdir /s /q build\classes 2>nul

echo [>>] Building MSI package...
echo [i] This may take a few minutes...
echo.
call %GRADLE_PATH% --stop
call %GRADLE_PATH% packageMsi --no-daemon
if %ERRORLEVEL% EQU 0 (
    echo.
    echo [OK] Build successful!
    echo [i] MSI location: build\compose\binaries\main\msi\
    explorer build\compose\binaries\main\msi 2>nul
) else (
    echo.
    echo [!] Build failed. Try option [3] Clean Build first.
)
goto menu

:clean
echo.
echo [>>] Stopping Gradle daemons...
call %GRADLE_PATH% --stop
echo [>>] Cleaning build files...
rmdir /s /q build 2>nul
rmdir /s /q .gradle 2>nul
echo [OK] Clean complete!
goto menu

:buildrun
echo.
echo [>>] Stopping any running instances...
taskkill /F /IM java.exe /FI "WINDOWTITLE eq VEXTIQ*" >nul 2>&1
call %GRADLE_PATH% --stop
echo [>>] Clean build and run...
echo.
call %GRADLE_PATH% clean run --no-daemon
goto menu

:end
echo.
echo [>>] Stopping Gradle daemons...
call %GRADLE_PATH% --stop >nul 2>&1
echo [i] Goodbye!
exit /b 0
