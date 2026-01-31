@echo off
REM Windows Installer Upgrade Helper
REM Kill running Organize Photos process, uninstall old version, install new version
REM Usage: upgrade.bat "path\to\new\installer.msi"

setlocal enabledelayedexpansion

if "%~1"=="" (
    echo Usage: %~n0 "path\to\new\installer.msi"
    exit /b 1
)

set NEW_INSTALLER=%~1
if not exist "!NEW_INSTALLER!" (
    echo Error: Installer not found: !NEW_INSTALLER!
    exit /b 1
)

echo [1/3] Terminating Organize Photos process...
taskkill /IM OrganizePhotos.exe /F >nul 2>&1
taskkill /IM java.exe /F >nul 2>&1
timeout /t 2 /nobreak >nul

echo [2/3] Uninstalling previous version...
REM Find and uninstall the old MSI via Windows Registry
for /f "tokens=2*" %%a in ('reg query "HKEY_CLASSES_ROOT\Installer\Products" ^| findstr /i "OrganizePhotos"') do (
    set PRODUCT_CODE=%%a
    if defined PRODUCT_CODE (
        msiexec /x "!PRODUCT_CODE!" /qn /norestart
        timeout /t 3 /nobreak >nul
    )
)

echo [3/3] Installing new version...
msiexec /i "!NEW_INSTALLER!" /qn /norestart

if %ERRORLEVEL% equ 0 (
    echo.
    echo ✓ Upgrade completed successfully!
    echo Organize Photos will start on next launch.
) else (
    echo.
    echo ✗ Installation failed with error code: %ERRORLEVEL%
    exit /b %ERRORLEVEL%
)

endlocal
exit /b 0
