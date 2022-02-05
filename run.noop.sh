#!/bin/bash

/opt/java/jdk-19/bin/java \
-XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -XX:+AlwaysPreTouch \
-Xms64m -Xmx64m -XX:MaxDirectMemorySize=2g \
--enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.foreign \
-Djava.library.path=native -cp bin test.EstimateCallOverhead
