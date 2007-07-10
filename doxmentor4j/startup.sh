#! /bin/sh
DOXMENTOR_HOME=$PWD
#cd "$DOXMENTOR_HOME"
# EXEDIR is the directory where this executable is.
# EXEDIR=${0%/*}
DIRLIBS=lib/*.jar
for i in ${DIRLIBS}
do
   if [ -z "$APP_CLASSPATH" ] ; then
    APP_CLASSPATH=$i
  else
    APP_CLASSPATH="$i":$APP_CLASSPATH
  fi
done

DIRLIBS=lib/*.zip
for i in ${DIRLIBS}
do
  if [ "$i" == "lib/*.zip" ] ; then
    continue
  fi
  if [ -z "$APP_CLASSPATH" ] ; then
    APP_CLASSPATH=$i
  else
    APP_CLASSPATH="$i":$APP_CLASSPATH
  fi
done

APP_CLASSPATH=DoxMentor4J.jar:$APP_CLASSPATH
echo $APP_CLASSPATH
java -Xms64m -Xmx512m  -Xss4m -classpath "$APP_CLASSPATH:$CLASSPATH" -Diapp.home="$DOXMENTOR_HOME" net.homeip.donaldm.doxmentor4j.DoxMentor4J "$@"

