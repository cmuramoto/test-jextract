#!/bin/bash

JAVA=/opt/java/jdk-19/bin/java

#GC="Parallel"
GC="Epsilon"

Mem=128m

$JAVA \
-XX:+UnlockExperimentalVMOptions -XX:+Use"$GC"GC -XX:+AlwaysPreTouch \
-XX:-TieredCompilation \
-Xms$Mem -Xmx$Mem -XX:MaxDirectMemorySize=2g \
--enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.foreign \
-Djava.library.path=native -cp bin test.ReaderNative
