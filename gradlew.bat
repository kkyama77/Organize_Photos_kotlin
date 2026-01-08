@echo off
REM --------------------------------------------------
REM Gradle startup script for Windows.
REM Requires JDK 17+.
REM --------------------------------------------------

set DIR=%~dp0
set APP_BASE_NAME=%~n0
set APP_HOME=%DIR%

set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

if not defined JAVA_HOME (
    echo Please set JAVA_HOME to a JDK 17+ installation.
    exit /b 1
)

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

"%JAVA_HOME%\bin\java.exe" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% -Dorg.gradle.appname=%APP_BASE_NAME% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
