PROJECTS="BT|CCE|CCM|CDC|CDNG|CDP|CE|CI|CV|CVNG|DEL|DOC|DX|ER|FFM|OPS|PIP|PL|SEC|SWAT|GTM|ONP|ART"

KEY=`git log --pretty=oneline --abbrev-commit -1 |\
  grep -o -iE "\[(${PROJECTS})-[0-9]+]:" | grep -o -iE "(${PROJECTS})-[0-9]+"`

if [ -z "$KEY" ]; then
  KEY=`git rev-parse --abbrev-ref HEAD | grep -o -iE "(${PROJECTS})-[0-9]+"`
fi

echo $KEY

status=`curl -X GET -H "Content-Type: application/json" https://harness.atlassian.net/rest/api/2/issue/${KEY}?fields=status --user $JIRA_USERNAME:$JIRA_PASSWORD | jq ".fields.status.name" | tr -d '"'`

echo $status

if [[ "${status}" = "QA Test" || "${status}" = "Done" ]]
        then
           echo "status is in done or qa test status"
        else
           echo "jira not in done or qa test status, Hence failing, current status is : ${status} "
	         exit 1
fi

