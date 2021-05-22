#!/bin/sh
#
# MoasdaWiki Server
#
# Script to run MoasdaWiki server as Linux daemon.
# See README.md for installation tutorial.
#
# Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
#
# This program is free software: you can redistribute it and/or modify it
# under the terms of the GNU Affero General Public License version 3 as
# published by the Free Software Foundation (AGPL-3.0-only).
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program. If not, see
# <https://www.gnu.org/licenses/agpl-3.0.html>.
#

MOASDAWIKI_USER=youruser
MOASDAWIKI_HOME=/home/$MOASDAWIKI_USER/moasdawiki            # folder containing MoasdaWiki.jar
MOASDAWIKI_REPOSITORY=$MOASDAWIKI_HOME/repository-en         # repository folder, change language on demand
MOASDAWIKI_JAR=$MOASDAWIKI_HOME/moasdawiki-server-2.x.y.jar  # JAR file name, replace version string
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/jre              # binary folder of JRE
JSVC_HOME=/usr/bin                                           # binary folder of jsvc
COMMONS_DAEMON_JAR=/usr/share/java/commons-daemon.jar        # path to commons-daemon.jar
CLASSPATH=$COMMONS_DAEMON_JAR:$MOASDAWIKI_JAR
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
