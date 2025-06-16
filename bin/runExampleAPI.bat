@echo off
SETLOCAL
REM === CONFIG END ===============================

REM JRE directory
SET JRE_HOME=

REM === CONFIG END ===============================
TITLE Javamon example: API
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
"%JRE%\bin\java" -classpath test;javamon.jar TestAPI
GOTO EXIT
:JRENOTFOUND
ECHO Java not found. If you have Java 1.4 or later installed, set the JRE_HOME environment variable to point to where the JRE or JDK is located.
:EXIT
pause
POPD
ENDLOCAL
@echo on