#!/bin/sh
#

BUILD=`pushd tools; mvn install; popd`
if [ $? -ne 0 ]
then
    echo "$BUILD"
    exit 1
fi

CHECKING=`mvn checkstyle:checkstyle`
if [ $? -ne 0 ]
then
    echo "$CHECKING"
    exit 1
fi

WARNINGS=`echo "$CHECKING" | grep "\[WARN\]"`
if [ ! -z "$WARNINGS" ]
then
    echo "$WARNINGS"
    exit 1
fi