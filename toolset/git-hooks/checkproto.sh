#!/usr/bin/env bash
#

if git rev-parse --verify HEAD >/dev/null 2>&1
then
    against=HEAD
else
    #Initial commit : diff against an empty tree object
    against=4b825dc642cb6eb9a060e54bf8d69288fbee4904
fi

#do the formatting
for file in `git diff-index --cached --name-only $against | grep -E '\.proto$'`
do
    if [ -e "${file}" ]
    then
      if ! grep 'syntax = "proto3";' ${file} > /dev/null
      then
        echo ${file} needs to use: syntax = \"proto3\";
        exit 1;
      fi

      if ! grep "option java_multiple_files = true;" ${file} > /dev/null
      then
        echo ${file} needs to use "option java_multiple_files = true;"
        exit 1;
      fi
    fi
done

ISSUES=`buf check lint`

if [ ! -z "${ISSUES}" ]
then
  echo $ISSUES
  exit 1
fi