@echo off
setlocal
set "MAVEN_VERSION=3.9.11"
set "MAVEN_DIR=%~dp0.mvn\apache-maven-%MAVEN_VERSION%"
if not exist "%MAVEN_DIR%\bin\mvn.cmd" (
  echo Downloading Apache Maven %MAVEN_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $root='%~dp0.mvn'; $zip=Join-Path $root 'maven.zip'; $null=New-Item -ItemType Directory -Force -Path $root; Invoke-WebRequest -UseBasicParsing 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile $zip; Expand-Archive -Force $zip $root; Remove-Item $zip"
  if errorlevel 1 exit /b 1
)
call "%MAVEN_DIR%\bin\mvn.cmd" %*
set "MAVEN_EXIT=%ERRORLEVEL%"
endlocal & exit /b %MAVEN_EXIT%
