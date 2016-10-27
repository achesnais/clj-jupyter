#!/bin/bash

UNAMESTR=$(UNAME)
KERNEL_DIR=""
KERNEL=clojure

# Provide setup according to kernel-spec
# https://jupyter-client.readthedocs.io/en/latest/kernels.html#kernel-specs
if [[ "$UNAMESTR" == 'linux' ]]; then
	KERNEL_DIR=~/.local/share/jupyter/kernels
elif [[ "$UNAMESTR" == 'Darwin' ]]; then
	KERNEL_DIR=~/Library/Jupyter/kernels
fi

TARGET_DIR=$KERNEL_DIR/$KERNEL

mkdir -p $TARGET_DIR
cp -f target/IClojure.jar $TARGET_DIR/IClojure.jar
sed 's|HOME|'${HOME}'|' resources/clj_jupyter/kernel.json > $TARGET_DIR/kernel.json

