@echo off
REM Builds and runs the demo with only a JDK (17+). No Maven needed.
REM Author: Jyoti Prakash Parida
setlocal
if exist out rmdir /s /q out
mkdir out
dir /s /b src\main\java\*.java > out\sources.txt
javac --release 17 -encoding UTF-8 -d out\classes @out\sources.txt || exit /b 1
java -cp out\classes com.jyoti.carrental.CarRentalApplication
