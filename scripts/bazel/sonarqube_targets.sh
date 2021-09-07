#!/usr/bin/env bash

set -e

echo "def get_sonarqube_targets():"
echo "    return {"
bazel query 'attr(tags, "sonarqube", //...:*)' | cut -c 3- | rev | cut -c 11- | rev |\
    awk ' { print "        \"//"$1":sonarqube\": \""$1"\"," } '
echo "    }"