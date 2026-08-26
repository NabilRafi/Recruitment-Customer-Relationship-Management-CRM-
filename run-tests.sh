#!/bin/bash
# Runs the JUnit 5 test suite.
#
# One-time setup: download the JUnit standalone console launcher into lib/
#   curl -L -o lib/junit-platform-console-standalone.jar \
#     https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar

set -e

JUNIT="lib/junit-platform-console-standalone.jar"

if [ ! -f "$JUNIT" ]; then
  echo "ERROR: $JUNIT not found."
  echo "Download it first:"
  echo "  curl -L -o $JUNIT https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar"
  exit 1
fi

echo "==> Compiling main sources"
mkdir -p out
javac -cp lib/sqlite-jdbc.jar -d out $(find src/main/java -name "*.java")

echo "==> Compiling tests"
mkdir -p out-test
javac -cp "out:lib/sqlite-jdbc.jar:$JUNIT" -d out-test $(find src/test/java -name "*.java")

echo "==> Running tests"
java -jar "$JUNIT" execute \
  --class-path "out:out-test:lib/sqlite-jdbc.jar" \
  --scan-class-path out-test \
  --details=tree
