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

@REM Execute docToolchain

@REM %GRADLECMD% --project-cache-dir %BASEDIR%.gradle -p %BASEDIR% -PdocDir=%WORKINGDIR%%1 %2 %3 %4 %5 %6 %7 %8 %9

cd %BASEDIR%

IF "%PATHTODOCS:~0,1%"=="." goto :relativePath

./gradlew --project-cache-dir %BASEDIR%/.gradle -PdocDir=%PATHTODOCS% %2 %3 %4 %5 %6

goto :end

:relativePath

./gradlew --project-cache-dir %BASEDIR%/.gradle -PdocDir=%WORKINGDIR%%PATHTODOCS% %2 %3 %4 %5 %6

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
echo     doctoolchain . publishToConfluence -PconfluenceConfigFile=ConfluenceConfig.groovy --no-daemon -q
goto :end

:end
