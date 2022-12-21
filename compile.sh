#!/bin/bash



rm -rf bin
/opt/java/latest_19/bin/javac -cp "libs/*" \
-source 19 --enable-preview \
--add-exports java.base/jdk.internal.misc=ALL-UNNAMED \
-sourcepath src src/trie/asm/*.java src/trie/nalim/*.java src/test/*.java \
-d bin
