#!/bin/bash

JAVAC=/opt/java/panama/bin/javac

rm -rf bin
$JAVAC -cp "libs/*" --add-modules jdk.incubator.foreign --add-exports java.base/jdk.internal.misc=ALL-UNNAMED -sourcepath src src/trie/asm/*.java src/test/*.java -d bin
