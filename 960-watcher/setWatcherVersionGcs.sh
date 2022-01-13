# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

if [ $# -ne 3 ]
then
  echo "This script is used to set version on watchers."
  echo "Usage: $0 <version> <environment>"
  echo "version: portal build number"
  echo "environment: ci, qa, prod"
  exit 1
fi

echo "System-Properties: version=1.0.${1}" >> app.mf
echo "Application-Version: version=1.0.${1}" >> app.mf
jar ufm watcher-capsule.jar app.mf
rm -rf app.mf
echo "1.0.${1} latest/watcher.jar" >> watcher${2}.txt
mv watcher-capsule.jar watcher.jar
