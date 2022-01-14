#!/usr/bin/env bash

set -e

MODULES="$1"

echo "def get_sonarqube_targets_seperated():"
echo "    return {"

if [[ "$MODULES" == "below400" ]]; then
  bazel query 'attr(tags, "sonarqube", //...:*)' | cut -c 3- | rev | cut -c 11- | rev |\
    awk ' { print "        \"//"$1":sonarqube\": \""$1"\"," } ' | sed '1,/400-rest/d'
elif [[ "$MODULES" == "only400" ]]; then
  bazel query 'attr(tags, "sonarqube", //...:*)' | cut -c 3- | rev | cut -c 11- | rev |\
    awk ' { print "        \"//"$1":sonarqube\": \""$1"\"," } ' | grep 400-rest
elif [[ "$MODULES" == "above400" ]]; then
  bazel query 'attr(tags, "sonarqube", //...:*)' | cut -c 3- | rev | cut -c 11- | rev |\
    awk ' { print "        \"//"$1":sonarqube\": \""$1"\"," } ' | sed -n '/400-rest/q;p'
else
  echo "No Module Range Provided...Exiting"; exit 1
fi

echo "    }"