@echo off
%windir%\System32\more +8 "%~f0" > "%temp%\%~n0.ps1"
powershell -NoProfile -ExecutionPolicy Bypass -File "%temp%\%~n0.ps1" %*
del %temp%\%~n0.ps1
REM pause
exit /b

*** PowerShell from here on ***

