#!/bin/bash

/opt/java/jdk-19/bin/java \
--enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.foreign \
-XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -XX:+TieredCompilation -XX:MaxDirectMemorySize=4G \
-XX:+AlwaysPreTouch \
-Djava.library.path=native -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true \
-cp bin test.ReaderNative
