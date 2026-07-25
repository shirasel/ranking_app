@echo off
setlocal
call "%~dp0gradle-local.cmd" run --args="generate --mock"
exit /b %ERRORLEVEL%
