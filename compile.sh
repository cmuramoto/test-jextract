#!/bin/bash



rm -rf bin
/opt/java/latest_20/bin/javac -cp "libs/*" \
-source 20 --enable-preview \
--add-exports java.base/jdk.internal.misc=ALL-UNNAMED \
-sourcepath src src/trie/asm/*.java src/trie/nalim/*.java src/test/*.java \
-d bin
