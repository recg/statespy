# !/bin/bash

num=$1

for path in ./filters/*; do
    filter="$(basename $path)"
    ./autotest.sh $num $filter 2>&1 | tee "test-results/$f.txt"
done

