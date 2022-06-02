#!/bin/bash
cp extra_bin/libscip-vm.so bin/libscip.so
tar -czvf solution.tgz solution bin/* jna-5.11.0.jar jna-platform-5.11.0.jar
