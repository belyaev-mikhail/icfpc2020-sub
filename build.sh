#!/bin/sh

cd app
kotlinc *.kt -include-runtime -d Main.jar
