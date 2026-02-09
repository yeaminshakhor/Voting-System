@echo off
REM Voting System - SQL Database Startup Script (Windows)
REM This batch file runs the voting system with SQLite and SLF4J support

setlocal enabledelayedexpansion

echo ===============================================================
echo        ELECTION VOTING SYSTEM - SQL Edition (Windows)
echo ===============================================================
echo.

REM Get the project directory
set PROJECT_DIR=%~dp0
set LIB_DIR=%PROJECT_DIR%lib

REM Check if lib directory exists
if not exist "%LIB_DIR%" (
    echo ❌ ERROR: lib directory not found!
    echo    Expected location: %LIB_DIR%
    echo.
    pause
    exit /b 1
)

REM Check if SQLite JDBC driver exists
if not exist "%LIB_DIR%\sqlite-jdbc-3.44.0.0.jar" (
    echo ❌ ERROR: SQLite JDBC driver not found!
    echo    Expected location: %LIB_DIR%\sqlite-jdbc-3.44.0.0.jar
    echo.
    pause
    exit /b 1
)

REM Check if SLF4J exists
if not exist "%LIB_DIR%\slf4j-api-2.0.9.jar" (
    echo ❌ ERROR: SLF4J API library not found!
    echo    Expected location: %LIB_DIR%\slf4j-api-2.0.9.jar
    echo.
    pause
    exit /b 1
)

echo ✅ SQLite JDBC Driver: FOUND
echo ✅ SLF4J Libraries: FOUND
echo.

REM Check if compiled classes exist
if not exist "%PROJECT_DIR%bin" (
    echo ⚠️  Classes not compiled. Compiling now...
    mkdir "%PROJECT_DIR%bin"
    cd /d "%PROJECT_DIR%"
    javac -cp "bin;%LIB_DIR%\*" -d bin Main.java Framesg\*.java Data\*.java Entities\*.java Utils\*.java 2>&1
    if errorlevel 1 (
        echo ❌ Compilation failed
        pause
        exit /b 1
    )
    echo ✅ Compilation complete
)

echo.
echo 📊 Database Configuration:
echo    Type: SQLite 3
echo    File: election_system.db
echo    Location: %PROJECT_DIR%election_system.db
echo    (Will be created on first use)
echo.

echo 🚀 Starting Voting System with SQL Database Support...
echo ===============================================================
echo.

REM Change to project directory and run with all libraries
cd /d "%PROJECT_DIR%"
java -cp "bin;%LIB_DIR%\*" Main

echo.
echo ===============================================================
echo               Application Terminated
echo ===============================================================
pause
