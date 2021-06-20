#!/bin/sh
#
# MoasdaWiki Server
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

MOASDAWIKI_USER=masdawiki
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
COMMONS_DAEMON_JAR=/usr/share/java/commons-daemon.jar
MOASDAWIKI_JAR=/usr/lib/moasdawiki/moasdawiki-server-${version}.jar
PID_FILE=/var/run/moasdawiki.pid
MOASDAWIKI_REPOSITORY=/srv/moasdawiki/repository

export LANG=de_DE.UTF-8
export LC_ALL=de_DE.UTF-8

case "$1" in
  start)
    echo Starting MoasdaWiki server ...

    /usr/bin/jsvc \
    -user $MOASDAWIKI_USER \
    -home $JAVA_HOME \
    -cp $COMMONS_DAEMON_JAR:$MOASDAWIKI_JAR \
    -pidfile $PID_FILE \
    -outfile /var/log/moasdawiki/outfile.log \
    -errfile /var/log/moasdawiki/errfile.log \
    -Duser.country=DE -Duser.language=de -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 \
    net.moasdawiki.MainService $MOASDAWIKI_REPOSITORY

    exit $?
    ;;

  stop)
    echo Shutdown MoasdaWiki server ...

    /usr/bin/jsvc \
    -stop \
    -pidfile $PID_FILE \
    net.moasdawiki.MainService

    exit $?
    ;;

  *)
    echo "Syntax: $0 {start|stop}"
    exit 1;;
esac
