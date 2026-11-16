@echo off
rem Starts RoomSync, or runs the tests with: run.bat test
setlocal
cd /d "%~dp0"
set "JAVA=java"
if exist "jdk\bin\java.exe" set "JAVA=jdk\bin\java.exe"
if not exist "out\com\roomsync\Main.class" (
    echo RoomSync is not built yet. Run build.bat first.
    exit /b 1
)
if /i "%~1"=="test" (
    "%JAVA%" -cp "out;lib/*" com.roomsync.TestRunner
) else (
    "%JAVA%" -cp "out;lib/*" com.roomsync.Main
)
