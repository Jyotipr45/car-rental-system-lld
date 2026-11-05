@echo off
REM Compiles main + test code and runs the test suite with only a JDK (17+).
REM Author: Jyoti Prakash Parida
setlocal
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > out\sources.txt
javac --release 17 -encoding UTF-8 -d out\classes @out\sources.txt || exit /b 1
xcopy /e /i /q /y src\main\resources out\classes >nul
java -cp out\classes com.jyoti.carrental.AllTests
