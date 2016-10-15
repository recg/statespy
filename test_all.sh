#!/bin/bash

num=$1

trap "pgrep -P $$" SIGINT SIGTERM
for path in ./filters/*; do
    filter="$(basename $path)"
    
    if [ -e "$path/onTransact.txt" ] ; then
        mv $path/onTransact.txt $path/onTransact
    fi

    if [ -e "$path/onTransact" ] ; then
        ./autotest.sh $num $filter
    else
        echo "no filter for $filter"
    fi

done

