@echo off
setlocal
node "%~dp0serve-github-pages-preview.js"
exit /b %ERRORLEVEL%
