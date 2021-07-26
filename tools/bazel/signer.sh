#!/usr/bin/env bash

set -e

STABLE_PROPERTIES=$1
shift 1

function getProperty () {
   cat "${STABLE_PROPERTIES}" | grep "^STABLE_$1 " | cut -d" " -f2
}

SIGNER_KEY_STORE=$(getProperty "SIGNER_KEY_STORE")
SIGNER_KEY_STORE_PASSWORD=$(getProperty "SIGNER_KEY_STORE_PASSWORD")

set +e

output=`eval $@`
errorCode=$?

if [ $errorCode != 0 ]; then echo $output; fi
exit $errorCode
