@echo off
REM Starts the REST API on http://localhost:8080 (Swagger UI at /swagger-ui). JDK 17+ only.
REM   server.bat            -> port 8080, seeded demo data
REM   server.bat 9090       -> another port
REM Stop with Ctrl+C.
REM Author: Jyoti Prakash Parida
setlocal
set PORT=%~1
if "%PORT%"=="" set PORT=8080
if exist out rmdir /s /q out
mkdir out
dir /s /b src\main\java\*.java > out\sources.txt
javac --release 17 -encoding UTF-8 -d out\classes @out\sources.txt || exit /b 1
xcopy /e /i /q /y src\main\resources out\classes >nul
java -Dserver.port=%PORT% -cp out\classes com.jyoti.carrental.ApiServerApplication
