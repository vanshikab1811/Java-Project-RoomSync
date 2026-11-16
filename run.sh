#!/bin/sh
# Starts RoomSync, or runs the tests with: ./run.sh test
cd "$(dirname "$0")" || exit 1
JAVA=java
[ -x jdk/bin/java ] && JAVA=jdk/bin/java
SEP=":"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";" ;; esac   # Git Bash on Windows needs ';'
if [ ! -f out/com/roomsync/Main.class ]; then
    echo "RoomSync is not built yet. Run ./build.sh first."
    exit 1
fi
if [ "$1" = "test" ]; then
    exec "$JAVA" -cp "out${SEP}lib/*" com.roomsync.TestRunner
else
    exec "$JAVA" -cp "out${SEP}lib/*" com.roomsync.Main
fi
