#!/usr/bin/env bash

ALL_MODULES=`ls -a`
MODULE=$1

if [[ ! $ALL_MODULES[*] =~ $MODULE ]]; then
    echo "Module not found in the project root."
    exit 0
fi

KRYO_DEPENDENCIES_FILE=`mktemp`
PROTO_DEPENDENCIES_FILE=`mktemp`

echo "Kryo dependencies file: " $KRYO_DEPENDENCIES_FILE
echo "Proto dependencies file: " $PROTO_DEPENDENCIES_FILE

bazel query "deps(//$MODULE:module)" | grep -i "KryoRegistrar" | rev | cut -f 1 -d "/" | rev | cut -f 1 -d "." > $KRYO_DEPENDENCIES_FILE
sh scripts/interface-hash/module-deps.sh $MODULE:module > $PROTO_DEPENDENCIES_FILE

CODEBASE_HASH_STRING=`bazel run "//001-microservice-intfc-tool:module" -- kryo-file=$KRYO_DEPENDENCIES_FILE proto-file=$PROTO_DEPENDENCIES_FILE ignore-json | grep "Codebase Hash:"`
echo "Extracted hash string: " $CODEBASE_HASH_STRING

HASH=${CODEBASE_HASH_STRING:14:64}
echo "New hash: " $HASH

EXISTING_HASH=$(head -n 1 $MODULE/module-dependency.hash)
echo "Existing hash: " $EXISTING_HASH

if [[ $HASH != $EXISTING_HASH ]];
then
  echo "You have changed files which are being used for delegate task communication. Your change should be backward compatible."
  echo "If the change is backward compatible replace the new hash $HASH in file $MODULE/module-dependency.hash"
  echo "Please contact delegate team for backward compatibility check."
  exit 1
fi

echo "Successful"

