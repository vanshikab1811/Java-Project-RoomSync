#!/bin/sh
# Compiles RoomSync into out/. Needs JDK 17+ (uses ./jdk if present, otherwise javac on PATH).
cd "$(dirname "$0")" || exit 1
JAVAC=javac
[ -x jdk/bin/javac ] && JAVAC=jdk/bin/javac
rm -rf out && mkdir out
"$JAVAC" -encoding UTF-8 -d out -cp "lib/*" $(find src -name "*.java") || { echo "Build failed."; exit 1; }
echo "Build succeeded. Start the app with ./run.sh, or run the tests with: ./run.sh test"
