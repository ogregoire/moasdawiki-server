#!/bin/sh
#
#  MoasdaWiki Server
#
#  Script to run MoasdaWiki server as Linux daemon.
#  See README for installation tutorial. 
#

JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/jre        # binary folder of JRE
JSVC_HOME=/usr/bin                                     # binary folder of jsvc
COMMONS_DAEMON_JAR=/usr/share/java/commons-daemon.jar  # path to commons-daemon.jar
MOASDAWIKI_HOME=/home/username/moasdawiki              # folder containing MoasdaWiki.jar
MOASDAWIKI_REPOSITORY=$MOASDAWIKI_HOME/repository-en   # repository folder, change language on demand
MOASDAWIKI_USER=username
CLASSPATH=$COMMONS_DAEMON_JAR:$MOASDAWIKI_HOME/MoasdaWiki-version.jar  # JAR file to run, replace version string
PID_FILE=/var/run/moasdawiki.pid

export LANG=de_DE.UTF-8
export LC_ALL=de_DE.UTF-8

case "$1" in
  start)
    echo Starting MoasdaWiki server ...

    $JSVC_HOME/jsvc \
    -user $MOASDAWIKI_USER \
    -home $JAVA_HOME \
    -cp $CLASSPATH \
    -pidfile $PID_FILE \
    -outfile $MOASDAWIKI_HOME/outfile \
    -errfile $MOASDAWIKI_HOME/errfile \
    -Duser.country=DE -Duser.language=de -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 \
    net.moasdawiki.MainService $MOASDAWIKI_REPOSITORY

    exit $?
    ;;

  stop)
    echo Shutdown MoasdaWiki server ...

    $JSVC_HOME/jsvc \
    -stop \
    -pidfile $PID_FILE \
    net.moasdawiki.MainService

    exit $?
    ;;

  *)
    echo "Syntax: $0 {start|stop}"
    exit 1;;
esac
