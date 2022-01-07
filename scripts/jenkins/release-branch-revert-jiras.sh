# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

KEYS=`git log --pretty=oneline --abbrev-commit |\
      awk "/${PREVIOUS_CUT_COMMIT_MESSAGE}/ {exit} {print}" | grep -iE "revert: \["  |\
      grep -o -iE '(ART|BT|CCE|CCM|CDC|CDNG|CDP|CE|CI|CV|CVNG|DEL|DOC|DX|ER|FFM|OPA|OPS|PIE|PL|SEC|SWAT|GTM|ONP)-[0-9]+' | sort | uniq`

LABLE="HOTFIX"

if [ "${PURPOSE}" = "saas" ]
then
    LABLE="SAAS_HOTFIX"
    FIELD_ID="customfield_10644"
elif [ "${PURPOSE}" = "on-prem" ]
then
    LABLE="ONPREM_HOTFIX"
    FIELD_ID="customfield_10646"
else
   echo "Unknown purpose ${PURPOSE}"
   exit 1
fi

echo $KEYS

echo $LABLE


for KEY in ${KEYS}
do
    echo $KEY
    curl \
       -X PUT \
       --data "{ \"fields\" : { \"${FIELD_ID}\" : \"\" }}" \
       -H "Content-Type: application/json" \
       https://harness.atlassian.net/rest/api/2/issue/${KEY} \
       --user $JIRA_USERNAME:$JIRA_PASSWORD
done
