@echo off
title MetaTrader VPS Bridge Server
color 0A
echo ==========================================================
echo        MetaTrader VPS Bridge Server for Android
echo ==========================================================
echo.

:: Check if Python is installed
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [!] Python is NOT detected in PATH.
    echo Please install Python 3 from https://www.python.org/downloads/
    echo Be sure to check the box "Add Python to PATH" during installation.
    echo.
    pause
    exit /b
)

:: Ensure Port 8080 is open in Windows Firewall
echo [*] Checking Windows Firewall for Port 8080...
netsh advfirewall firewall show rule name="MT_VPS_Bridge_8080" >nul 2>&1
if %errorlevel% neq 0 (
    echo [*] Opening Port 8080 in Windows Firewall...
    netsh advfirewall firewall add rule name="MT_VPS_Bridge_8080" dir=in action=allow protocol=TCP localport=8080 >nul 2>&1
    if %errorlevel% equ 0 (
        echo [+] Port 8080 successfully opened in Windows Firewall!
    ) else (
        echo [!] Please run this script as Administrator to auto-open Port 8080.
    )
) else (
    echo [+] Firewall rule for Port 8080 already active.
)

echo.
echo [*] Starting Python Bridge Server on Port 8080...
echo [*] Connect your Android phone to: ws://<YOUR_VPS_PUBLIC_IP>:8080
echo ==========================================================
echo.
python bridge.py 8080
pause
