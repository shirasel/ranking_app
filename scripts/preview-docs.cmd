@echo off
setlocal
node "%~dp0preview-docs.js"
exit /b %ERRORLEVEL%
