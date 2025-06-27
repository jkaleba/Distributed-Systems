@echo off
setlocal

cd /d %~dp0

echo ARGUMENTY: %*
echo Current dir: %CD%

java -jar build\libs\server-1.0-SNAPSHOT-all.jar %*
