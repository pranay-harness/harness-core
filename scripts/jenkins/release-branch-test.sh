#!/bin/bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

set -e

PROJECTS="ART|BT|CCE|CCM|CDC|CDNG|CDP|CE|CI|CV|CVNG|DEL|DOC|DX|ER|FFM|OPS|PIE|PL|SEC|SWAT|GTM|ONP"

git log --remotes=origin/release/* --pretty=oneline --abbrev-commit | grep -iE "\[(${PROJECTS})-[0-9]+]:" -o | sort | uniq | tr '[:lower:]' '[:upper:]' > release.txt
git log --remotes=origin/[m]aster --pretty=oneline --abbrev-commit | grep -iE "\[(${PROJECTS})-[0-9]+]:" -o | sort | uniq | tr '[:lower:]' '[:upper:]' > master.txt
NOT_MERGED=`comm -23 release.txt master.txt | tr '\n' ' '`

if [ -z "$NOT_MERGED" ]
then
      echo "All Hotfix changes are reflected in Master as well" > envvars
else
      echo NOT_MERGED="${NOT_MERGED}" > envvars
fi
