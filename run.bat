@echo off
REM Builds and runs the demo with only a JDK (17+). No Maven needed.
REM   run.bat          -> console shows ERROR only, files get INFO+
REM   run.bat debug    -> console and files get DEBUG
REM   run.bat trace    -> everything, including lock and repository traces
REM Logs: logs\car-rental-0.log (all) and logs\audit-0.log (audit trail)
REM Author: Jyoti Prakash Parida
setlocal
set LOG_OPTS=
if /i "%~1"=="debug" set LOG_OPTS=-Dlog.level=DEBUG -Dlog.console=DEBUG
if /i "%~1"=="trace" set LOG_OPTS=-Dlog.level=TRACE -Dlog.console=TRACE
if exist out rmdir /s /q out
mkdir out
dir /s /b src\main\java\*.java > out\sources.txt
javac --release 17 -encoding UTF-8 -d out\classes @out\sources.txt || exit /b 1
xcopy /e /i /q /y src\main\resources out\classes >nul
java %LOG_OPTS% -cp out\classes com.jyoti.carrental.CarRentalApplication
