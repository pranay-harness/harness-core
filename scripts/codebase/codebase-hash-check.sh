#!/usr/bin/env bash

bold=$(tput bold)
normal=$(tput sgr0)

ALL_MODULES=`ls -a`
for MODULE in $@
do
      echo -e "${bold}\n##################################################\nChecking dependency hash for module: $MODULE\n${normal}"

      if [[ ! $ALL_MODULES[*] =~ $MODULE ]]; then
          echo "Module not found in the project root."
          exit 0
      fi


      TEMP_DIR=`mktemp -d`
      touch $TEMP_DIR/KryoDeps-$MODULE.text
      touch $TEMP_DIR/ProtoDeps-$MODULE.text

      KRYO_DEPENDENCIES_FILE=$TEMP_DIR/KryoDeps-$MODULE.text
      PROTO_DEPENDENCIES_FILE=$TEMP_DIR/ProtoDeps-$MODULE.text

      echo "Kryo dependencies file: " $KRYO_DEPENDENCIES_FILE
      echo "Proto dependencies file: " $PROTO_DEPENDENCIES_FILE

      bazel query "deps(//$MODULE:module)" | grep -i "KryoRegistrar" | rev | cut -f 1 -d "/" | rev | cut -f 1 -d "." > $KRYO_DEPENDENCIES_FILE
      sh scripts/interface-hash/module-deps.sh $MODULE:module > $PROTO_DEPENDENCIES_FILE

      CODEBASE_HASH_STRING=`bazel run "//001-microservice-intfc-tool:module" -- kryo-file=$KRYO_DEPENDENCIES_FILE proto-file=$PROTO_DEPENDENCIES_FILE ignore-json | grep "Codebase Hash:"`

      HASH=${CODEBASE_HASH_STRING:14:64}
      echo "New hash: " $HASH

      EXISTING_HASH=$(head -n 1 $MODULE/module-dependency.hash)
      echo "Existing hash: " $EXISTING_HASH

      if [[ $HASH != $EXISTING_HASH ]];
      then
        echo "${bold}You have changed files which are being used for delegate task communication. Your change should be backward compatible."
        echo "If the change is backward compatible replace the new hash $HASH in file $MODULE/module-dependency.hash"
        echo "Please start script from Portal project in your local environment for modules $@. e.g. ./scripts/codebase/codebase-hash-check.sh $@"
        echo "Please contact delegate team for backward compatibility check."
        echo -e "\nFailure.${normal}"
        exit 1
      fi
      echo "There are no changes in dependencies for module: $MODULE"
      echo -e "\n${bold}Successful${normal}"
done

echo -e "\n${bold}All checks are successful.${normal}"

