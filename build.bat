@echo off
setlocal
cd /d "%~dp0"
if not exist build mkdir build
powershell -NoProfile -Command "Get-ChildItem src,tests -Recurse -Filter *.java | ForEach-Object { [char]34 + ($_.FullName -replace '\\','/') + [char]34 } | Set-Content -Encoding ascii build/sources.txt"
if errorlevel 1 exit /b 1
javac -encoding UTF-8 -source 8 -target 8 -Xlint:all -d build @build/sources.txt
exit /b %errorlevel%
