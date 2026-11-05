#!/usr/bin/env bash
# Starts the REST API on http://localhost:8080 (Swagger UI at /swagger-ui). JDK 17+ only.
#   ./server.sh            -> port 8080, seeded demo data
#   ./server.sh 9090       -> another port
# Stop with Ctrl+C.
# Author: Jyoti Prakash Parida
set -euo pipefail
cd "$(dirname "$0")"
PORT="${1:-8080}"
rm -rf out && mkdir -p out
find src/main/java -name '*.java' > out/sources.txt
javac --release 17 -encoding UTF-8 -d out/classes @out/sources.txt
cp -r src/main/resources/. out/classes/
java -Dserver.port="$PORT" -cp out/classes com.jyoti.carrental.ApiServerApplication
