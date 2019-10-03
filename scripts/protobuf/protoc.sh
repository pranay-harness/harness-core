#/bin/env bash

EXPECTED_VERSION="libprotoc 3.7.1"

VERSION=`protoc --version`
if [ "${EXPECTED_VERSION}" != "${VERSION}" ]
then
  echo "Your protoc version $VERSION does not match the expected ${EXPECTED_VERSION}"
  exit 1
fi

protoc --java_opt=annotate_code $@