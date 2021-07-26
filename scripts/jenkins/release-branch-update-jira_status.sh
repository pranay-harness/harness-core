#!/bin/bash
PROJECTS="BT|CCE|CCM|CDC|CDNG|CDP|CE|CI|CV|CVNG|DEL|DOC|DX|ER|FFM|OPS|PL|SEC|SWAT|GTM|ONP"
KEYS=$(git log --pretty=oneline --abbrev-commit |\
      awk "/${PREVIOUS_CUT_COMMIT_MESSAGE}/ {exit} {print}" |\
      grep -o -iE '('$PROJECTS')-[0-9]+' | sort | uniq)

QA_TEST_STATUS_ID=201

for KEY in ${KEYS}
do
    echo $KEY

    status=`curl -X GET -H "Content-Type: application/json" https://harness.atlassian.net/rest/api/2/issue/${KEY}?fields=status --user $JIRA_USERNAME:$JIRA_PASSWORD | jq ".fields.status.name" | tr -d '"'`

    echo $status

    if [[ "${status}" = "QA Test" || "${status}" = "Done" ]]
            then
               echo " ${KEY}  is in Done or QA-Test status, Hence no update"
            else
               echo " ${KEY}  is in  ${status} , Hence moving to QA-Test status"
               curl \
                 -X POST \
                --data "{\"transition\":{\"id\":\"${QA_TEST_STATUS_ID}\"}}" \
                -H "Content-Type: application/json" \
                https://harness.atlassian.net/rest/api/2/issue/${KEY}/transitions \
                --user $JIRA_USERNAME:$JIRA_PASSWORD
    fi

done

