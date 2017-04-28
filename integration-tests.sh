#!/bin/bash

#run all integration tests
mvn failsafe:integration-test failsafe:verify
test_status=$?

#take dump of mongodb
mongodump

#tar.gz dump files
tar -czvf dump.tar.gz dump/

if [[ $test_status -ne 0 ]] ; then
  echo 'integration tests failed';
  exit $test_status
fi
