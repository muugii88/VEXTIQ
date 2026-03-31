@echo off
title Gradle Wrapper
cd /d "%~dp0"

echo Running Gradle tasks directly...
echo.

REM Try to run gradle directly
gradle %*

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Gradle not found or failed to run
    echo Please install Gradle or check your PATH
    pause
    exit /b 1
)

echo.
echo Gradle task completed successfully