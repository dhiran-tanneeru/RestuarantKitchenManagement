#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")"
mkdir -p build
find src tests -name '*.java' -print > build/sources.txt
if command -v javac >/dev/null 2>&1; then
  if javac -version 2>&1 | grep -q 'javac 1.8'; then
    javac -encoding UTF-8 -source 8 -target 8 -Xlint:all -Werror -d build @build/sources.txt
  else
    javac -encoding UTF-8 --release 8 -Xlint:all -Werror -d build @build/sources.txt
  fi
else
  java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 8 -Xlint:all -Werror -d build @build/sources.txt
fi
