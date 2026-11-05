#!/usr/bin/env bash
# Compiles main + test code and runs the test suite with only a JDK (17+).
# Author: Jyoti Prakash Parida
set -euo pipefail
cd "$(dirname "$0")"
rm -rf out && mkdir -p out
find src -name '*.java' > out/sources.txt
javac --release 17 -encoding UTF-8 -d out/classes @out/sources.txt
java -cp out/classes com.jyoti.carrental.CarRentalSystemTest
