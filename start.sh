#!/usr/bin/env bash
# Start the Recruitment CRM web app on http://localhost:8080
set -euo pipefail
cd "$(dirname "$0")"

if command -v mvn >/dev/null 2>&1; then
  echo "Building with Maven..."
  mvn -q package -DskipTests
  echo "Starting server at http://localhost:8080"
  exec java -jar target/recruitment-crm.jar
fi

# Fallback: compile with javac + bundled SQLite JDBC
if [ -z "${JAVA_HOME:-}" ] && command -v /usr/libexec/java_home >/dev/null 2>&1; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 21+ 2>/dev/null || /usr/libexec/java_home 2>/dev/null || true)"
fi
if [ -z "${JAVA_HOME:-}" ] && [ -d "/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home" ]; then
  export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home"
fi
JAVA="${JAVA_HOME:-}/bin/java"
JAVAC="${JAVA_HOME:-}/bin/jar"
JAVAC_BIN="${JAVA_HOME:-}/bin/javac"
JAR="${JAVA_HOME:-}/bin/jar"

if [ ! -x "$JAVA" ]; then
  echo "Java not found. Install JDK 21+ (brew install --cask temurin) and try again."
  exit 1
fi
if [ ! -f lib/sqlite-jdbc.jar ]; then
  echo "Missing lib/sqlite-jdbc.jar — run: curl -L -o lib/sqlite-jdbc.jar https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.49.1.0/sqlite-jdbc-3.49.1.0.jar"
  exit 1
fi

mkdir -p out
echo "Compiling..."
"$JAVAC_BIN" -cp "lib/sqlite-jdbc.jar" -d out $(find src/main/java -name "*.java")

echo "Packaging..."
rm -rf build-jar && mkdir -p build-jar
cp -r out/* build-jar/
cd build-jar && "$JAR" xf ../lib/sqlite-jdbc.jar && cd ..
"$JAR" cfe target/recruitment-crm.jar com.recruitcrm.web.WebMain -C build-jar .

echo "Starting server at http://localhost:8080"
exec "$JAVA" -jar target/recruitment-crm.jar
