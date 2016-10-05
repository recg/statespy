# !/bin/bash

num=$1

ls ./filters | while read f; do
    ./autotest.sh $num $f & wait
done

