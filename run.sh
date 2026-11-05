#!/usr/bin/env bash
# Builds and runs the demo with only a JDK (17+). No Maven needed.
# Author: Jyoti Prakash Parida
set -euo pipefail
cd "$(dirname "$0")"
rm -rf out && mkdir -p out
find src/main/java -name '*.java' > out/sources.txt
javac --release 17 -encoding UTF-8 -d out/classes @out/sources.txt
java -cp out/classes com.jyoti.carrental.CarRentalApplication
