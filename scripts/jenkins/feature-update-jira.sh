# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

PROJECTS="BT|CCE|CCM|CDC|CDNG|CDP|CE|CI|CV|CVNG|DEL|DOC|DX|ER|FFM|OPA|OPS|PIE|PL|SEC|SWAT|GTM|ONP"

KEY=`git log --pretty=oneline --abbrev-commit -1 |\
  grep -o -iE "\[(${PROJECTS})-[0-9]+]:" | grep -o -iE "(${PROJECTS})-[0-9]+"`

if [ -z "$KEY" ]; then
  KEY=`git rev-parse --abbrev-ref HEAD | grep -o -iE "(${PROJECTS})-[0-9]+"`
fi

FIELD_ID="customfield_10769"
if [ ! -z "$KEY" ]
then
  echo $KEY
  curl \
    -X PUT \
    --data "{ \"fields\" : { \"${FIELD_ID}\" : \"${FEATURE_BUILD_VALUE}\" }}" \
    -H "Content-Type: application/json" \
    https://harness.atlassian.net/rest/api/2/issue/${KEY} \
    --user $JIRA_USERNAME:$JIRA_PASSWORD
fi
