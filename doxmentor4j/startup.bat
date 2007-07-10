@echo off
set APP_CLASSPATH=DoxMentor4J.jar;
for %%i in ("lib\*.jar") do call "cpappend.bat" %%i
for %%i in ("lib\*.zip") do call "cpappend.bat" %%i
set APP_CLASSPATH=%APP_CLASSPATH%;%CLASSPATH%
java -cp "%APP_CLASSPATH%" -Xms64m -Xmx512m -Xss4m net.homeip.donaldm.doxmentor4j.DoxMentor4J  %*

