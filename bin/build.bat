@echo off
SETLOCAL
REM === CONFIG BEGIN =========================================

REM If you have Proguard installed, set the jar location below
SET PROGUARD=C:\Tools\proguard\lib\proguard.jar

REM === CONFIG END ===========================================
TITLE Building javamon...
PUSHD "%~dp0\.."

REM Get JDK path
SET JMINVER=1.4
SET JDK=%JDK_HOME%
IF EXIST "%JDK%\bin\javac.exe" GOTO JDKFOUND
SET JDK=%JAVA_HOME%
IF EXIST "%JDK%\bin\javac.exe" GOTO JDKFOUND
FOR /f "tokens=2*" %%i IN ('reg query "HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit" /s 2^>nul ^| find "JavaHome"') DO SET JDK=%%j
IF EXIST "%JDK%\bin\javac.exe" GOTO JDKFOUND
FOR /f "tokens=2*" %%i IN ('reg query "HKEY_LOCAL_MACHINE\SOFTWARE\Wow6432Node\JavaSoft\Java Development Kit" /s 2^>nul ^| find "JavaHome"') DO SET JDK=%%j
IF NOT EXIST "%JDK%\bin\javac.exe" GOTO JDKNOTFOUND
:JDKFOUND
IF NOT EXIST "%JDK%\jre\lib\rt.jar" GOTO RTNOTFOUND

REM Compile the javamon
rd /s /q classes >nul 2>nul
mkdir classes\javamon 2>nul
"%JDK%\bin\javac" -source %JMINVER% -target %JMINVER% -bootclasspath "%JDK%\jre\lib\rt.jar" -classpath src test\TestWrapper.java test\TestAPI.java
"%JDK%\bin\javac" -source %JMINVER% -target %JMINVER% -bootclasspath "%JDK%\jre\lib\rt.jar" -d classes\javamon -g:lines -classpath src src\com\agent\javamon.java
IF %ERRORLEVEL% NEQ 0 GOTO EXIT

REM Generate the Manifest
(
ECHO Manifest-Version: 1.0
ECHO Main-Class: com.agent.javamon
ECHO Copyright: 2025
ECHO Created-By: Vladimir Kamenar
ECHO.
) >classes\javamon.MF

REM Pack the JAR (optionally optimize with Proguard)
IF NOT EXIST "%PROGUARD%" GOTO NOPROGUARD
"%JDK%\bin\jar" cmf classes\javamon.MF javamon_in.jar -C classes\javamon .
IF %ERRORLEVEL% NEQ 0 GOTO EXIT
for %%x in ("%JDK%\") do set SH_JDK=%%~dpsx
"%JDK%\bin\java" -jar %PROGUARD% -injars javamon_in.jar -outjars javamon.jar -libraryjars "%SH_JDK%\jre\lib\rt.jar" @src\javamon.pro
del javamon_in.jar /q >nul 2>nul
GOTO EXIT
:NOPROGUARD
"%JDK%\bin\jar" cmf classes\javamon.MF javamon.jar -C classes\javamon .
GOTO EXIT

:JDKNOTFOUND
ECHO JDK not found. If you have JDK %JMINVER% or later installed, set the JDK_HOME environment variable to point to where the JDK is located.
GOTO EXIT
:RTNOTFOUND
ECHO %JDK%\jre\lib\rt.jar not found
:EXIT
pause
POPD
ENDLOCAL
@echo on
