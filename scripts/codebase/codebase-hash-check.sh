#!/usr/bin/env bash

bold=$(tput bold)
normal=$(tput sgr0)

errors=()
ALL_MODULES=`ls -a`
for MODULE in $@
do
      echo -e "${bold}\n##################################################\nChecking dependency hash for module: $MODULE\n${normal}"

      if [[ ! $ALL_MODULES[*] =~ $MODULE ]]; then
          echo "Module not found in the project root."
          exit 1
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

      EXISTING_HASH=$(cat $MODULE/module-dependency.hash)
      echo "Existing hash: " $EXISTING_HASH

      if [[ $HASH != $EXISTING_HASH ]];
      then
        message="$HASH in file $MODULE/module-dependency.hash"
        echo -e "\n${bold}$MODULE check failed.\n${normal}"
        errors+=("$message")
      else
        echo "There are no changes in dependencies for module: $MODULE"
        echo -e "\n${bold}$MODULE check is successful${normal}"
      fi
done

if [ ${#errors[@]} -eq 0 ]; then
   echo -e "\n${bold}All checks are successful.${normal}"
else
    echo "${bold}You have changed files which are being used for delegate task communication. Your change should be backward compatible."
    echo "If you want to generate hash locally run this script: ./scripts/codebase/codebase-hash-check.sh $@"
    echo "Please contact delegate team for backward compatibility check."

    echo -e "${bold}\n----------------------------------------------------------------------------\n"
    echo "If the change is backward compatible replace the new hash:"

    for i in "${!errors[@]}"
    do
        echo $(($i +1))")" ${errors[$i]}
    done
    echo -e "\n----------------------------------------------------------------------------\n"
    echo -e "\nFailure.${normal}"
    exit 1
fi


