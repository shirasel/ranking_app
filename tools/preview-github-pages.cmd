@echo off
setlocal
node "%~dp0preview\serve-github-pages-preview.js"
exit /b %ERRORLEVEL%
