#!/bin/sh

java -jar /solution/app/Main.jar "$@" || echo "run error code: $?"
