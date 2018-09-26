#!/bin/bash

#run all integration tests
echo 'running integration tests'
vault_token=`cat vault.log | grep "Root Token:" | awk '{print $3}'`
echo vault token: $vault_token
serviceSecret=`mongo harness --eval "db.serviceSecrets.find({ }, { serviceSecret: 1, _id: 0})"| grep serviceSecret | awk '{print $4}' | tr -d '"'`
echo learing engine secret: $serviceSecret
export LEARNING_SERVICE_SECRET=$serviceSecret
mvn -B failsafe:integration-test -DskipITs=false -P integration-coverage jacoco:report -Dvault.token=$vault_token
test_status=$?

# take dump of mongodb
mongodump

# tar.gz dump files
tar -czf dump.tar.gz dump/

if [[ $test_status -ne 0 ]] ; then
  echo 'integration tests failed';
  exit $test_status
fi
