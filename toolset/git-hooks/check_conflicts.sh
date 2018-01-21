#!/bin/sh
#

CHECKING=`grep -nr "^<\{7\} \|^=\{7\}\|^>\{7\} " *`
if [ ! -z "$CHECKING" ]
then
    echo "$CHECKING"
    exit 1
fi