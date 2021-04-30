#!/bin/bash
function rabbit ()
{
    INTERVAL=1
    reps=$1
    files=$2
    if [ $reps -gt 0 ];
    then
	sleep $INTERVAL
	(rabbit $(expr $reps - 1) $files) &
	sleep $INTERVAL
	if [ $reps -gt 1 ];
	then 
	    (rabbit $(expr $reps - 2) $files) &
	fi
	for _ in $(seq $(expr $reps \* $INTERVAL));
	do
	    (shuffleCommands $2) &
	    sleep 1.01
	done
    fi
}

function shuffleCommands ()
{
    targets=( $(ls $1) )
    length=${#targets[@]}
    (eval "$1/${targets[$(expr $RANDOM % $length)]}") &
}

rabbit $1 $2
