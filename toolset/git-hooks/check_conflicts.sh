#!/usr/bin/env bash
# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

#

if git rev-parse --verify HEAD >/dev/null 2>&1
then
    against=HEAD
else
    #Initial commit : diff against an empty tree object
    against=4b825dc642cb6eb9a060e54bf8d69288fbee4904
fi

FILES=`git diff-index --cached --name-only $against`

CHECKING=`
for file in $FILES
do
    if [ -e "${file}" ]
    then
        grep -nr "^<\{7\} \|^=\{7\}$\|^>\{7\} " "${file}"
    fi
done`

if [ ! -z "$CHECKING" ]
then
    echo "$CHECKING"
    exit 1
fi
