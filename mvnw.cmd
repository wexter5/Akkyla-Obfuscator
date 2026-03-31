@REM Maven Wrapper script for Windows
@echo off
setlocal

SET MAVEN_VERSION=3.9.6
SET USER_HOME=%USERPROFILE%
SET MAVEN_HOME=%USER_HOME%\.m2\wrapper\maven-%MAVEN_VERSION%
SET MAVEN_URL=https://dlcdn.apache.org/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip
SET WRAPPER_JAR=%USER_HOME%\.m2\wrapper\maven-wrapper.jar

IF NOT EXIST "%MAVEN_HOME%" (
    echo Downloading Maven %MAVEN_VERSION%...
    mkdir "%USER_HOME%\.m2\wrapper" 2>nul
    powershell -Command "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%USER_HOME%\.m2\wrapper\maven.zip'"
    powershell -Command "Expand-Archive -Path '%USER_HOME%\.m2\wrapper\maven.zip' -DestinationPath '%USER_HOME%\.m2\wrapper' -Force"
    del "%USER_HOME%\.m2\wrapper\maven.zip"
)

SET PATH=%MAVEN_HOME%\bin;%PATH%
mvn %*

endlocal
