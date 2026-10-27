#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")"
bash build.sh
java -cp build restuarant.Review1Tests "$@"
python3 tests/console_smoke.py
