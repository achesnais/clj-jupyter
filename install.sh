#!/bin/bash

UNAMESTR=$(UNAME)
TARGET_DIR=""
if [[ "$UNAMESTR" == 'linux' ]]; then
	TARGET_DIR=~/.local/share/jupyter/kernels/clojure/
elif [[ "$UNAMESTR" == 'Darwin' ]]; then
	TARGET_DIR=~/Library/Jupyter/kernels/clojure
fi

mkdir -p $TARGET_DIR
cp -f target/IClojure.jar $TARGET_DIR/IClojure.jar
sed 's|HOME|'${HOME}'|' resources/clj_jupyter/kernel.json > $TARGET_DIR/kernel.json

