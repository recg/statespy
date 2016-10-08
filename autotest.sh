# !/bin/bash

num=$1
filt=$2


echo $filt
java -jar jdi_state_spill.jar -c $filt &
JarPID=$!

trap "echo Booh!" SIGINT SIGTERM

adb shell pm list packages -f | grep "/app/" | while read p; do
    printf "\n"
    echo $service_name
	service_name=$(cut -d "=" -f 2 -s <<< $p | sed 's/\r//g')
    adb shell monkey -p "$service_name" --pct-syskeys 0 $1 & wait
done

kill $JarPID
