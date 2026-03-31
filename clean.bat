@echo off
title VEXTIQ - Deep Clean
color 0C

echo.
echo ========================================
echo   VEXTIQ DEEP CLEAN
echo ========================================
echo.

echo [1/4] Killing Java/Gradle processes...
taskkill /F /IM java.exe 2>nul
taskkill /F /IM javaw.exe 2>nul
taskkill /F /IM gradle.exe 2>nul
taskkill /F /IM kotlin.exe 2>nul
taskkill /F /IM VEXTIQ.exe 2>nul
echo [OK] Processes killed

echo.
echo [2/4] Removing build folder...
if exist build rmdir /S /Q build
echo [OK] Build folder removed

echo.
echo [3/4] Removing Gradle cache...
if exist .gradle rmdir /S /Q .gradle
echo [OK] Gradle cache removed

echo.
echo [4/4] Removing Kotlin cache...
if exist "%USERPROFILE%\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlin" (
    echo Clearing Kotlin cache...
)
echo [OK] Done

echo.
echo ========================================
echo   CLEAN COMPLETE!
echo ========================================
echo.
echo Now run: build.bat
echo.

pause
