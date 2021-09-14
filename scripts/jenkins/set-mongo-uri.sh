#!/bin/bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


# JIRA: https://harness.atlassian.net/browse/BT-195
# Description: Support multiple mongo deployments
# Author: Diptiman Adak
# Date: 7-Jan-2021

mongo_working=false

# Check whether remote server is running or not
check_mongo_status () {
  mongo "$1" --eval "db.stats()"
  RESULT=$?
  if [ $RESULT -ne 0 ]; then
    echo "mongodb not running"
  else
    echo "mongodb running!"
    export TEST_MONGO_URI=$1
    mongo_working=true
  fi
}

set -x

#Parse the comma separated list of mongo URIs and store into the array
echo $TEST_MONGO_URIS
IFS=', ' read -r -a array <<< "$TEST_MONGO_URIS"

for index in "${!array[@]}"
do
  if [ "$mongo_working" = false ] ; then
     check_mongo_status "${array[index]}"
  fi
done

#If none of mongo instances are running, mark the build failed
if [ "$mongo_working" = false ] ; then
  echo "None of the mongo instances are working fine!! Build will be marked as failure"
  exit 1
fi
