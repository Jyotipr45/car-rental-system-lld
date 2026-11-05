#!/usr/bin/env bash
# Builds and runs the demo with only a JDK (17+). No Maven needed.
#   ./run.sh          -> console shows ERROR only, files get INFO+
#   ./run.sh debug    -> console and files get DEBUG
#   ./run.sh trace    -> everything, including lock and repository traces
# Logs: logs/car-rental-0.log (all) and logs/audit-0.log (audit trail)
# Author: Jyoti Prakash Parida
set -euo pipefail
cd "$(dirname "$0")"
LOG_OPTS=""
case "${1:-}" in
  debug) LOG_OPTS="-Dlog.level=DEBUG -Dlog.console=DEBUG" ;;
  trace) LOG_OPTS="-Dlog.level=TRACE -Dlog.console=TRACE" ;;
esac
rm -rf out && mkdir -p out
find src/main/java -name '*.java' > out/sources.txt
javac --release 17 -encoding UTF-8 -d out/classes @out/sources.txt
java $LOG_OPTS -cp out/classes com.jyoti.carrental.CarRentalApplication
