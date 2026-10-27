#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")"
bash build.sh
exec java -cp build restuarant.Main
