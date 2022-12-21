#!/bin/bash

JAVA=/opt/java/latest_19/bin/java

main_class="test.ReaderUnsafeAlt"

$JAVA \
--enable-preview \
-XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:+UseSerialGC -XX:+AlwaysPreTouch \
-Xms64m -Xmx64m -XX:LoopUnrollLimit=0 -XX:MaxDirectMemorySize=2g \
--enable-native-access=ALL-UNNAMED --add-modules jdk.internal.vm.ci \
--add-opens java.base/jdk.internal.misc=ALL-UNNAMED \
--add-exports jdk.internal.vm.ci/jdk.vm.ci.code=ALL-UNNAMED      \
--add-exports jdk.internal.vm.ci/jdk.vm.ci.code.site=ALL-UNNAMED \
--add-exports jdk.internal.vm.ci/jdk.vm.ci.hotspot=ALL-UNNAMED   \
--add-exports jdk.internal.vm.ci/jdk.vm.ci.meta=ALL-UNNAMED      \
--add-exports jdk.internal.vm.ci/jdk.vm.ci.runtime=ALL-UNNAMED   \
-Djava.library.path=native \
-cp "bin:libs/*" $main_class
