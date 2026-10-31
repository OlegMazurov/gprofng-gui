#!/bin/bash
DST=/opt/javascope
cp javascope $DST/bin/
cp etc/gprofng.rc $DST/etc/
cp ./build/libs/gprofng-gui-3.0-SNAPSHOT.jar $DST/lib/gprofng-analyzer.jar
cp ./jdbe/build/libs/jdbe-3.0-SNAPSHOT.jar $DST/lib/jdbe.jar
