@echo off
setlocal

set "PROJECT_ROOT=%~dp0.."
set "GRADLE_USER_HOME=%PROJECT_ROOT%\.gradle-home"

if exist "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot\bin\java.exe" (
  set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
)

call "%PROJECT_ROOT%\gradlew.bat" %*
exit /b %ERRORLEVEL%
