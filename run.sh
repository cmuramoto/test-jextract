#!/bin/bash

JAVA=/opt/java/latest_18/bin/java
PS3='Select Mode: '
options=("MemorySegments" "Unsafe" "Unsafe(Alt)" "Native(Panama)" "Native(Panam-Alt)" "Native(Nalim-Alt)" "Noop" "JMH")

select opt in "${options[@]}"
do
    case $opt in
        "MemorySegments")
            main_class="test.Reader"
            break
        ;;
        "Unsafe")
            main_class="test.ReaderUnsafe"
            break
        ;;
        "Unsafe(Alt)")
            main_class="test.ReaderUnsafeAlt"
            break
        ;;
        "Native(Panama)")
            main_class="test.ReaderNative"
            break
        ;;
        "Native(Panama-Alt)")
            main_class="test.ReaderNativeAlt"
            break
        ;;
        "Native(Nalim-Alt)")
            main_class="test.ReaderNativeAltNalim"
            break
        ;;
        "Noop")
            main_class="test.EstimateCallOverhead"
            break
        ;;
        "JMH")
            main_class="test.JMHRunner"
            break
        ;;
        *)
            echo "invalid mode $REPLY"
            exit 1
        ;;
    esac
done

echo "Running $main_class"


$JAVA \
-XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:+UseSerialGC -XX:+AlwaysPreTouch \
-Xms64m -Xmx64m -XX:MaxDirectMemorySize=2g \
--enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.foreign,jdk.internal.vm.ci \
--add-opens java.base/jdk.internal.misc=ALL-UNNAMED \
--add-exports jdk.internal.vm.ci/jdk.vm.ci.code=ALL-UNNAMED      \
--add-exports jdk.internal.vm.ci/jdk.vm.ci.code.site=ALL-UNNAMED \
--add-exports jdk.internal.vm.ci/jdk.vm.ci.hotspot=ALL-UNNAMED   \
--add-exports jdk.internal.vm.ci/jdk.vm.ci.meta=ALL-UNNAMED      \
--add-exports jdk.internal.vm.ci/jdk.vm.ci.runtime=ALL-UNNAMED   \
-Djava.library.path=native \
-cp "bin:libs/*" $main_class
