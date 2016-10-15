# !/bin/bash

num=$1
filt=$2


echo $filt

if [ -e "test-results/$filt.txt" ] ; then
	echo $filt
    echo "%%%%results file already exists%%%%"
    exit
fi

java -jar jdi_state_spill.jar -c $filt &
JarPID=$!

trap "kill $JarPID" SIGINT SIGTERM

adb shell pm list packages -f | grep "/app/" | while read p; do
    printf "\n"
	service_name=$(cut -d "=" -f 2 -s <<< $p | sed 's/\r//g')
	echo $service_name
    echo $service_name >> "test-results/$filt.txt"
    adb shell monkey -p "$service_name" --pct-syskeys 0 --throttle 500 $1 & wait
done

kill $JarPID
    
