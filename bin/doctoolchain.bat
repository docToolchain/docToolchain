@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  docToolchain script for Windows
@rem
@rem ##########################################################################

set CMD_LINE_ARGS=%*

IF "%1"=="" GOTO :help
IF [%1]==[/?] GOTO :help

set SCRIPTDIR=%~dp0
set BASEDIR=%SCRIPTDIR%..\
set GRADLECMD=%BASEDIR%gradlew.bat
set WORKINGDIR=%cd%\
set PATHTODOCS=%1

@REM throw the first parameter away
set params=%*
set params=%params:* =%

@REM Execute docToolchain

@REM %GRADLECMD% --project-cache-dir %BASEDIR%.gradle -p %BASEDIR% -PdocDir=%WORKINGDIR%%1 %params%

cd /d %BASEDIR%

IF "%PATHTODOCS:~0,1%"=="." goto :relativePath

call "%GRADLECMD%" --project-cache-dir %BASEDIR%/.gradle "-PdocDir=%PATHTODOCS%" %params%

goto :end

:relativePath

call "%GRADLECMD%" --project-cache-dir %BASEDIR%/.gradle "-PdocDir=%WORKINGDIR%%PATHTODOCS%" %params%

goto :end

:help


echo Usage: doctoolchain docDir [option...] [task...]
echo.
echo docDir - Absolute directory with the documentation.
echo.
echo You can use the same options and tasks as in underlying gradle.
echo Use "doctoolchain . --help" to see available options.
echo Use "doctoolchain . tasks" to see available tasks.
echo.
echo Examples:
echo.
echo   Init new project with arc42 tempalte
echo     doctoolchain . -b init.gradle initArc42EN
echo.
echo   Generate PDF:
echo     doctoolchain . generatePDF
echo.
echo   Generate HTML:
echo     doctoolchain . generateHTML
echo.
echo   Publish HTML to Confluence:
echo     doctoolchain . publishToConfluence
goto :end

:end
rem back to WORKINGDIR
cd /d %WORKINGDIR%
