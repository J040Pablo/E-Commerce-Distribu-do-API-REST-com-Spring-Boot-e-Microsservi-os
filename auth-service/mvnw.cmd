@REM Maven Wrapper script for Windows
@REM

@if "%DEBUG%" == "" @echo off
@setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.

@REM Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >nul 2>&1
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

goto fail

:init
@REM Find the project base dir, i.e. the directory that contains the .mvn folder.
@REM Fallback to current directory if .mvn folder not found.

if not "%MAVEN_PROJECTBASEDIR%" == "" goto endDetectBaseDir

set EXEC_DIR=%CD%
set WDIR=%EXEC_DIR%
:findBaseDir
if exist "%WDIR%"\.mvn goto baseDirFound
cd ..
if "%WDIR%" == "%CD%" goto baseDirNotFound
set WDIR=%CD%
goto findBaseDir

:baseDirFound
set MAVEN_PROJECTBASEDIR=%WDIR%
cd "%EXEC_DIR%"
goto endDetectBaseDir

:baseDirNotFound
set MAVEN_PROJECTBASEDIR=%EXEC_DIR%
cd "%EXEC_DIR%"

:endDetectBaseDir

@REM Setup the command line

set CLASSPATH=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar;%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\MavenWrapperDownloader.class

if "%MAVEN_PROJECTBASEDIR%"=="" (
	set MAVEN_PROJECTBASEDIR=%CD%
)

@REM Extension to allow automatically downloading the maven-wrapper.jar and .settings.xml from Maven-central
@REM This allows using the maven wrapper in projects that prohibit checking in binary data.
if exist "%USERPROFILE%\.m2\repository\org\apache\maven\wrapper\maven-wrapper\3.1.0\maven-wrapper-3.1.0.jar" (
    set WRAPPER_JAR="%USERPROFILE%\.m2\repository\org\apache\maven\wrapper\maven-wrapper\3.1.0\maven-wrapper-3.1.0.jar"
)
if not "%WRAPPER_JAR%"=="" (
    java -Dorg.slf4j.simpleLogger.defaultLogLevel=info -cp %WRAPPER_JAR% org.apache.maven.wrapper.MavenWrapperMain %MAVEN_ARGS% %*
) else (
    powershell -Command "&{ $webclient = new-object System.Net.WebClient; if (-not ([string]::IsNullOrEmpty('%MAVEN_USERNAME%') -and [string]::IsNullOrEmpty('%MAVEN_PASSWORD%'))) { $webclient.Credentials = new-object System.Net.NetworkCredential('%MAVEN_USERNAME%', '%MAVEN_PASSWORD%') } [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12; $webclient.DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%') }"
    if "%ERRORLEVEL%" == "0" (
        java -Dorg.slf4j.simpleLogger.defaultLogLevel=info -cp %WRAPPER_JAR% org.apache.maven.wrapper.MavenWrapperMain %MAVEN_ARGS% %*
    ) else (
        echo Couldn't download %WRAPPER_URL%
        exit /b 1
    )
)
@endlocal
@goto end

:fail
@endlocal
@goto error_exit

:error_exit
@echo off
set ERROR_CODE=%ERRORLEVEL%

if not "%MAVEN_SKIP_RC%"=="" goto skipRcPost
@REM check for post script, ".bat" ending in "_post" will only run if you set "POST_ID"
if exist "%EXITDIR%\maven_post.bat" call "%EXITDIR%\maven_post.bat"
:skipRcPost

@endlocal & set ERROR_CODE=%ERROR_CODE%

if not "%ERRORLEVEL%"=="" (
    exit /b %ERRORLEVEL%
) else (
    exit /b 0
)
:end
