# !/bin/bash
num=$1
adb shell pm list packages -f | grep "/app/" | while read p; do
	echo $service_name
	service_name=$(cut -d "=" -f 2 -s <<< $p | sed 's/\r//g')
	adb shell monkey -p "$service_name" $1 & wait
done
