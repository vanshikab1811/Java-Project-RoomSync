@echo off
rem Compiles RoomSync into the out folder. Needs JDK 17+ (uses .\jdk if present, otherwise javac on PATH).
setlocal
cd /d "%~dp0"
set "JAVAC=javac"
if exist "jdk\bin\javac.exe" set "JAVAC=jdk\bin\javac.exe"
if exist out rmdir /s /q out
mkdir out
"%JAVAC%" -encoding UTF-8 -d out -cp "lib/*" src\com\roomsync\*.java src\com\roomsync\model\*.java src\com\roomsync\db\*.java src\com\roomsync\logic\*.java src\com\roomsync\ui\*.java
if errorlevel 1 (
    echo Build failed.
    exit /b 1
)
echo Build succeeded. Start the app with run.bat, or run the tests with: run.bat test
