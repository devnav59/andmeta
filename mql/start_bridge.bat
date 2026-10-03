@echo off
title MetaTrader VPS Bridge Server
color 0B
cls
echo ==========================================================
echo        MetaTrader VPS Bridge Server for Android
echo ==========================================================
echo.

:: 1. Open Firewall Port 8080 using netsh (Universal for all Windows versions)
echo [*] Checking Windows Firewall for Port 8080...
netsh advfirewall firewall add rule name="MT_Bridge_8080" dir=in action=allow protocol=TCP localport=8080 >nul 2>&1
if %errorlevel% equ 0 (
    echo [+] Firewall Port 8080 is OPEN and ALLOWED!
) else (
    echo [!] Note: Run as Administrator to ensure Firewall Port 8080 is open.
)
echo.

:: 2. Check Python installation
echo [*] Checking Python installation...
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ==========================================================
    echo [ERROR] Python 3 is NOT installed on this VPS!
    echo ==========================================================
    echo You need Python to run bridge.py.
    echo 1. Download Python: https://www.python.org/downloads/
    echo 2. IMPORTANT: During installation, CHECK the box:
    echo    "Add Python to PATH"
    echo 3. After installing, run this file (start_bridge.bat) again.
    echo ==========================================================
    echo.
    pause
    exit /b 1
)

:: Python version found
for /f "tokens=*" %%i in ('python --version') do set PYVER=%%i
echo [+] Detected: %PYVER%
echo.

:: 3. Run bridge.py and keep console open on any error
echo [*] Starting MetaTrader VPS Bridge on Port 8080...
echo [*] Press Ctrl+C anytime to stop.
echo ==========================================================
echo.
python bridge.py 8080

if %errorlevel% neq 0 (
    echo.
    echo ==========================================================
    echo [!] Server stopped with error code %errorlevel%.
    echo Check if Port 8080 is already used by another application.
    echo ==========================================================
)
echo.
pause
