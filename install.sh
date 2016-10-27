#!/bin/bash

KERNEL_SPEC="./resources/clj_jupyter"
KERNEL=clojure
BUILD_DIR=./build

# Provide setup according to kernel-spec
# https://jupyter-client.readthedocs.io/en/latest/kernels.html#kernel-specs
mkdir -p $BUILD_DIR
cp -r $KERNEL_SPEC/* $BUILD_DIR
cp -f target/IClojure.jar $BUILD_DIR/IClojure.jar
sed 's|HOME|'${HOME}'|' $KERNEL_SPEC/kernel.json > $BUILD_DIR/kernel.json
# do a sed later here to parse to install location and feed it in to 
# the kernelspec, see sed about. Better yet, jupyter should provide an install hook
jupyter kernelspec install --user --replace --name=$KERNEL $BUILD_DIR

