#!/bin/bash

JAVA=/opt/java/jdk-19/bin/java
PS3='Select Mode: '
options=("MemorySegments" "Unsafe" "Native" "JMH")

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
        "Native")
            main_class="test.ReaderNative"
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
-XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -XX:+AlwaysPreTouch \
-Xms64m -Xmx64m -XX:MaxDirectMemorySize=2g \
--enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.foreign \
--add-opens java.base/jdk.internal.misc=ALL-UNNAMED \
-Djava.library.path=native \
-cp "bin:libs/*" $main_class
