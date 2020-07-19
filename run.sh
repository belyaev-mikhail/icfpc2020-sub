#!/bin/sh

java -Xss100M -jar /solution/app/icfpc2020/target/app.jar "$@" || echo "run error code: $?"
