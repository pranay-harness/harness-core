KEYS=`git log --pretty=oneline --abbrev-commit |\
      awk "/${PREVIOUS_CUT_COMMIT_MESSAGE}/ {exit} {print}" |\
      grep -o -iE '(BT|CCE|CCM|CDC|CDNG|CDP|CE|CI|CV|CVNG|DEL|DOC|DX|ER|FFM|OPS|PL|SEC|SWAT|GTM|ONP)-[0-9]+' | sort | uniq`

LABLE="HOTFIX"

if [ "${PURPOSE}" = "saas" ]
then
    LABLE="SAAS_HOTFIX"
elif [ "${PURPOSE}" = "on-prem" ]
then
    LABLE="SAAS_HOTFIX"
else
   echo "Unknown purpose ${PURPOSE}"
fi

for KEY in ${KEYS}
do
    echo $KEY
    curl \
       -X PUT \
       --data "{ \"fields\" : { \"labels\" : [\"${LABLE}\"] }}" \
       -H "Content-Type: application/json" \
       https://harness.atlassian.net/rest/api/2/issue/${KEY} \
       --user $JIRA_USERNAME:$JIRA_PASSWORD
done
