@echo off
REM ==================================================================
REM  Build the WAR with Maven and deploy it to a local Tomcat 10.1.
REM  Usage:  set CATALINA_HOME=C:\apache-tomcat-10.1.x
REM          deploy.bat
REM ==================================================================
setlocal

if "%CATALINA_HOME%"=="" (
    echo [ERROR] CATALINA_HOME is not set. Example:
    echo         set CATALINA_HOME=C:\apache-tomcat-10.1.34
    exit /b 1
)

echo [1/4] Building project with Maven...
call mvn clean package -q
if errorlevel 1 (
    echo [ERROR] Maven build failed.
    exit /b 1
)

echo [2/4] Stopping Tomcat (ignore errors if it was not running)...
call "%CATALINA_HOME%\bin\shutdown.bat" >nul 2>&1
timeout /t 3 /nobreak >nul

echo [3/4] Deploying NetBanking.war...
if exist "%CATALINA_HOME%\webapps\NetBanking" rmdir /s /q "%CATALINA_HOME%\webapps\NetBanking"
copy /y "target\NetBanking.war" "%CATALINA_HOME%\webapps\" >nul

echo [4/4] Starting Tomcat...
call "%CATALINA_HOME%\bin\startup.bat"

echo.
echo Open http://localhost:8080/NetBanking/ in your browser (wait a few seconds for deployment).
endlocal
