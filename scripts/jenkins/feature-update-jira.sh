# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

PROJECTS="BT|CCE|CCM|CDC|CDNG|CDP|CE|CI|CV|CVNG|DEL|DOC|DX|ER|FFM|OPS|PIE|PL|SEC|SWAT|GTM|ONP"

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
