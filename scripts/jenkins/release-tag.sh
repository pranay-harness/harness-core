# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

if [ -z "$RELEASE_COMMIT" ]
then
  echo "Please Provide Valid git commit sha to RELEASE_COMMIT variable"
  exit 1
fi

UNIT_TEST_SIGN_OFF=`git tag -l --contains $RELEASE_COMMIT | grep signoff-unittest`

if [ -z "$UNIT_TEST_SIGN_OFF" ]
then
  echo "The specified sha $RELEASE_COMMIT was not signed off from unit tests"
  exit 1
else
  echo "The specified sha $RELEASE_COMMIT was  signed off from unit tests"
fi

FUNCTIONAL_TEST_SIGN_OFF=`git tag -l --contains $RELEASE_COMMIT | grep signoff-functional-test`

if [ -z "$FUNCTIONAL_TEST_SIGN_OFF" ]
then
  echo "The specified sha $RELEASE_COMMIT was not signed off from functional tests"
  exit 1
else
  echo "The specified sha $RELEASE_COMMIT was signed off from functional tests"
fi


git checkout "$RELEASE_COMMIT"

# initialize variables
export VERSION_FILE=build.properties

export VERSION=`cat ${VERSION_FILE} |\
    grep 'build.number=' |\
    sed -e 's: *build.number=::g'`
export VERSION=${VERSION%??}

# Export variables
echo VERSION=${VERSION} > jenkins.properties
