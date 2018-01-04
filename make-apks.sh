#!/bin/bash

apps="treebolic"

for a in $apps; do
	echo "*** $a"
	./gradlew :${a}:assembleRelease
done
