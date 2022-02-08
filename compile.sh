#!/bin/bash



rm -rf bin
/opt/java/jdk-19/bin/javac -cp "libs/*" --add-modules jdk.incubator.foreign --add-exports java.base/jdk.internal.misc=ALL-UNNAMED -sourcepath src src/trie/asm/*.java src/test/*.java -d bin
