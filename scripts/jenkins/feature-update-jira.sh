PROJECTS="CCE|CCM|CDC|CDNG|CDP|CE|CI|CV|CVNG|DEL|DOC|DX|ER|OPS|PL|SEC|SWAT"

KEY=`git log --pretty=oneline --abbrev-commit -1 |\
  grep -o -iE "\[(${PROJECTS})-[0-9]+]:" | grep -o -iE "(${PROJECTS})-[0-9]+"`
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
