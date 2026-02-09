#!/bin/bash

# Voting System - SQLite Enabled Startup Script
# This script runs the voting system with SQLite database support

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="lib"

echo "═══════════════════════════════════════════════════════════════"
echo "           ELECTION VOTING SYSTEM - SQL Edition               "
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Check if lib directory exists
if [ ! -d "$PROJECT_DIR/$LIB_DIR" ]; then
    echo "❌ ERROR: lib directory not found!"
    echo "   Expected location: $PROJECT_DIR/$LIB_DIR"
    exit 1
fi

# Check if SQLite JDBC driver exists
if [ ! -f "$PROJECT_DIR/$LIB_DIR/sqlite-jdbc-3.44.0.0.jar" ]; then
    echo "❌ ERROR: SQLite JDBC driver not found!"
    echo "   Expected location: $PROJECT_DIR/$LIB_DIR/sqlite-jdbc-3.44.0.0.jar"
    exit 1
fi

# Check if SLF4J API exists
if [ ! -f "$PROJECT_DIR/$LIB_DIR/slf4j-api-2.0.9.jar" ]; then
    echo "❌ ERROR: SLF4J API library not found!"
    echo "   Expected location: $PROJECT_DIR/$LIB_DIR/slf4j-api-2.0.9.jar"
    exit 1
fi

echo "✅ SQLite JDBC Driver: FOUND"
echo "✅ SLF4J Libraries: FOUND"
echo ""

# Check if compiled classes exist
if [ ! -d "$PROJECT_DIR/bin" ]; then
    echo "⚠️  Classes not compiled. Compiling now..."
    mkdir -p bin
    cd "$PROJECT_DIR"
    javac -cp "bin:$LIB_DIR/*" -d bin Main.java Framesg/*.java Data/*.java Entities/*.java Utils/*.java 2>&1
    if [ $? -eq 0 ]; then
        echo "✅ Compilation complete"
    else
        echo "❌ Compilation failed"
        exit 1
    fi
fi

echo ""
echo "📊 Database Configuration:"
echo "   Type: SQLite 3"
echo "   File: election_system.db"
echo "   Location: $PROJECT_DIR/election_system.db"
echo "   (Will be created on first use)"
echo ""

echo "🚀 Starting Voting System with SQL Database Support..."
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Run application with all required libraries
cd "$PROJECT_DIR"
java -cp "bin:$LIB_DIR/*" Main

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "             Application Terminated"
echo "═══════════════════════════════════════════════════════════════"
