@echo off
setlocal
call "%~dp0run-gradle-local.cmd" run --args="validate"
