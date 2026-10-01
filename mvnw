#!/bin/sh
set -eu
MAVEN_VERSION=3.9.11
BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MAVEN_DIR="$BASE_DIR/.mvn/apache-maven-$MAVEN_VERSION"
if [ ! -x "$MAVEN_DIR/bin/mvn" ]; then
  echo "Downloading Apache Maven $MAVEN_VERSION..."
  mkdir -p "$BASE_DIR/.mvn"
  curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.tar.gz" -o "$BASE_DIR/.mvn/maven.tar.gz"
  tar -xzf "$BASE_DIR/.mvn/maven.tar.gz" -C "$BASE_DIR/.mvn"
  rm "$BASE_DIR/.mvn/maven.tar.gz"
fi
exec "$MAVEN_DIR/bin/mvn" "$@"
