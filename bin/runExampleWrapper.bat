@echo off
SETLOCAL
REM === CONFIG END ===============================

REM JRE directory
SET JRE_HOME=

REM Javamon host (localhost, if empty)
SET JAVAMON_HOST=

REM Javamon port (9091, if empty)
SET JAVAMON_PORT=

REM The main class to load
SET CMDMAINCLASS=TestWrapper

REM The command-line parameters for the main class
SET CMDPARAMS=param1 param2

REM === CONFIG END ===============================
TITLE Javamon example: wrapper
PUSHD "%~dp0\.."
SET JRE=%JRE_HOME%
IF EXIST "%JRE%\bin\java.exe" GOTO JREFOUND
SET JRE=%JAVA_HOME%
IF EXIST "%JRE%\bin\java.exe" GOTO JREFOUND
FOR /f "tokens=2*" %%i IN ('reg query "HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Runtime Environment" /s 2^>nul ^| find "JavaHome"') DO SET JRE=%%j
IF EXIST "%JRE%\bin\java.exe" GOTO JREFOUND
FOR /f "tokens=2*" %%i IN ('reg query "HKEY_LOCAL_MACHINE\SOFTWARE\Wow6432Node\JavaSoft\Java Runtime Environment" /s 2^>nul ^| find "JavaHome"') DO SET JRE=%%j
IF EXIST "%JRE%\bin\java.exe" GOTO JREFOUND
FOR /f "tokens=2*" %%i IN ('reg query "HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit" /s 2^>nul ^| find "JavaHome"') DO SET JRE=%%j
IF EXIST "%JRE%\bin\java.exe" GOTO JREFOUND
FOR /f "tokens=2*" %%i IN ('reg query "HKEY_LOCAL_MACHINE\SOFTWARE\Wow6432Node\JavaSoft\Java Development Kit" /s 2^>nul ^| find "JavaHome"') DO SET JRE=%%j
IF NOT EXIST "%JRE%\bin\java.exe" GOTO JRENOTFOUND
:JREFOUND
SET CNFG=
IF NOT [%JAVAMON_HOST%]==[] SET CNFG=-Djm.host=%JAVAMON_HOST%
IF NOT [%JAVAMON_PORT%]==[] SET CNFG=%CNFG% -Djm.port=%JAVAMON_PORT%
IF NOT [%CMDMAINCLASS%]==[] SET CNFG=%CNFG% -Djm.main=%CMDMAINCLASS%
ECHO Monitoring endpoint: http://[jm.host]:[jm.port]/metrics
"%JRE%\bin\java" %CNFG% -classpath test;javamon.jar com.agent.javamon %CMDPARAMS%
GOTO EXIT
:JRENOTFOUND
ECHO Java not found. If you have Java 1.4 or later installed, set the JRE_HOME environment variable to point to where the JRE or JDK is located.
:EXIT
pause
POPD
ENDLOCAL
@echo on