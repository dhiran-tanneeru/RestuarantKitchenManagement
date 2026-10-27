@echo off
cd /d "%~dp0"
call build.bat
if errorlevel 1 exit /b 1
java -cp build restuarant.Review1Tests %*
if errorlevel 1 exit /b 1
python tests/console_smoke.py
