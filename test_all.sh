# !/bin/bash

num=$1

ls ./filters | while read f; do
    echo $f
    ./autotest.sh $num $f 2>&1 2>&1 | tee "test-results/$f.txt" & wait
done

