#!/bin/bash

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

#Create yum repo file to install mongo
cat > /etc/yum.repos.d/mongodb-org-4.4.repo << EOF
[mongodb-org-4.4]
name=MongoDB Repository
baseurl=https://repo.mongodb.org/yum/redhat/\$releasever/mongodb-org/4.4/x86_64/
gpgcheck=1
enabled=1
gpgkey=https://www.mongodb.org/static/pgp/server-4.4.asc
EOF

#Install mongo shell
yum -y install mongodb-org-shell

for index in "${!array[@]}"
do
  if [ "$mongo_working" = false ] ; then
     check_mongo_status "${array[index]}"
  fi
done

#Clean up mongo shell
yum -y erase $(rpm -qa | grep mongodb-org-shell)

#If none of mongo instances are running, mark the build failed
if [ "$mongo_working" = false ] ; then
  echo "None of the mongo instances are working fine!! Build will be marked as failure"
  exit 1
fi