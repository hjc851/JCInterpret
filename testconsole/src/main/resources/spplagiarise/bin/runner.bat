@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  runner startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Add default JVM options here. You can also use JAVA_OPTS and RUNNER_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\runner-1.0-SNAPSHOT.jar;%APP_HOME%\lib\core-1.0-SNAPSHOT.jar;%APP_HOME%\lib\kotlin-stdlib-jdk8-1.3.11.jar;%APP_HOME%\lib\weld-se-core-3.0.6.Final.jar;%APP_HOME%\lib\weld-probe-core-3.0.6.Final.jar;%APP_HOME%\lib\weld-environment-common-3.0.6.Final.jar;%APP_HOME%\lib\weld-core-impl-3.0.6.Final.jar;%APP_HOME%\lib\weld-spi-3.0.SP4.jar;%APP_HOME%\lib\weld-api-3.0.SP4.jar;%APP_HOME%\lib\cdi-api-2.0.jar;%APP_HOME%\lib\kotlin-stdlib-jdk7-1.3.11.jar;%APP_HOME%\lib\mapdb-3.0.7.jar;%APP_HOME%\lib\jackson-module-kotlin-2.9.6.jar;%APP_HOME%\lib\kotlin-reflect-1.2.21.jar;%APP_HOME%\lib\kotlin-stdlib-1.3.11.jar;%APP_HOME%\lib\jackson-databind-2.9.6.jar;%APP_HOME%\lib\jackson-core-2.9.6.jar;%APP_HOME%\lib\org.eclipse.jdt.core-3.22.0.jar;%APP_HOME%\lib\javax.ws.rs-api-2.1.1.jar;%APP_HOME%\lib\cxf-rt-rs-client-3.3.1.jar;%APP_HOME%\lib\commons-text-1.6.jar;%APP_HOME%\lib\commons-lang3-3.9.jar;%APP_HOME%\lib\commons-io-2.6.jar;%APP_HOME%\lib\google-java-format-1.7.jar;%APP_HOME%\lib\javax.el-api-3.0.0.jar;%APP_HOME%\lib\javax.interceptor-api-1.2.jar;%APP_HOME%\lib\javax.inject-1.jar;%APP_HOME%\lib\jboss-classfilewriter-1.2.3.Final.jar;%APP_HOME%\lib\kotlin-stdlib-common-1.3.11.jar;%APP_HOME%\lib\annotations-13.0.jar;%APP_HOME%\lib\jackson-annotations-2.9.0.jar;%APP_HOME%\lib\org.eclipse.core.resources-3.13.700.jar;%APP_HOME%\lib\org.eclipse.text-3.10.200.jar;%APP_HOME%\lib\org.eclipse.core.expressions-3.6.800.jar;%APP_HOME%\lib\org.eclipse.core.runtime-3.18.0.jar;%APP_HOME%\lib\org.eclipse.core.filesystem-1.7.700.jar;%APP_HOME%\lib\eclipse-collections-forkjoin-10.3.0.M4.jar;%APP_HOME%\lib\eclipse-collections-10.3.0.M4.jar;%APP_HOME%\lib\eclipse-collections-api-10.3.0.M4.jar;%APP_HOME%\lib\guava-27.0.1-jre.jar;%APP_HOME%\lib\lz4-1.3.0.jar;%APP_HOME%\lib\elsa-3.0.0-M5.jar;%APP_HOME%\lib\cxf-rt-frontend-jaxrs-3.3.1.jar;%APP_HOME%\lib\cxf-rt-transports-http-3.3.1.jar;%APP_HOME%\lib\cxf-rt-security-3.3.1.jar;%APP_HOME%\lib\cxf-core-3.3.1.jar;%APP_HOME%\lib\javac-shaded-9+181-r4173-1.jar;%APP_HOME%\lib\org.eclipse.osgi-3.15.300.jar;%APP_HOME%\lib\org.eclipse.core.jobs-3.10.800.jar;%APP_HOME%\lib\org.eclipse.core.contenttype-3.7.700.jar;%APP_HOME%\lib\org.eclipse.equinox.app-1.4.500.jar;%APP_HOME%\lib\org.eclipse.equinox.registry-3.8.800.jar;%APP_HOME%\lib\org.eclipse.equinox.preferences-3.8.0.jar;%APP_HOME%\lib\org.eclipse.core.commands-3.9.700.jar;%APP_HOME%\lib\org.eclipse.equinox.common-3.12.0.jar;%APP_HOME%\lib\jaxb-xjc-2.3.2.jar;%APP_HOME%\lib\jaxb-runtime-2.3.2.jar;%APP_HOME%\lib\woodstox-core-5.0.3.jar;%APP_HOME%\lib\xmlschema-core-2.2.4.jar;%APP_HOME%\lib\stax-ex-1.8.1.jar;%APP_HOME%\lib\jakarta.xml.bind-api-2.3.2.jar;%APP_HOME%\lib\jakarta.ws.rs-api-2.1.5.jar;%APP_HOME%\lib\javax.annotation-api-1.3.1.jar;%APP_HOME%\lib\failureaccess-1.0.1.jar;%APP_HOME%\lib\listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar;%APP_HOME%\lib\jsr305-3.0.2.jar;%APP_HOME%\lib\checker-qual-2.5.2.jar;%APP_HOME%\lib\error_prone_annotations-2.2.0.jar;%APP_HOME%\lib\j2objc-annotations-1.1.jar;%APP_HOME%\lib\animal-sniffer-annotations-1.17.jar;%APP_HOME%\lib\jboss-annotations-api_1.3_spec-1.0.0.Final.jar;%APP_HOME%\lib\jboss-el-api_3.0_spec-1.0.7.Final.jar;%APP_HOME%\lib\jboss-interceptors-api_1.2_spec-1.0.0.Final.jar;%APP_HOME%\lib\jboss-logging-3.2.1.Final.jar;%APP_HOME%\lib\txw2-2.3.2.jar;%APP_HOME%\lib\istack-commons-runtime-3.0.8.jar;%APP_HOME%\lib\FastInfoset-1.2.16.jar;%APP_HOME%\lib\jakarta.activation-api-1.2.1.jar;%APP_HOME%\lib\stax2-api-3.1.4.jar;%APP_HOME%\lib\xsom-2.3.2.jar;%APP_HOME%\lib\codemodel-2.3.2.jar;%APP_HOME%\lib\rngom-2.3.2.jar;%APP_HOME%\lib\dtd-parser-1.4.1.jar;%APP_HOME%\lib\istack-commons-tools-3.0.8.jar;%APP_HOME%\lib\relaxng-datatype-2.3.2.jar;%APP_HOME%\lib\ant-1.10.5.jar;%APP_HOME%\lib\ant-launcher-1.10.5.jar

@rem Execute runner
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %RUNNER_OPTS%  -classpath "%CLASSPATH%" spplagiarise.MainKt %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable RUNNER_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%RUNNER_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
