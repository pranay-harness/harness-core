KEYS=`git log --pretty=oneline --abbrev-commit | awk "/${PREVIOUS_CUT_COMMIT_MESSAGE}/ {exit} {print}" | grep -o -iE '(CCM|CCE|CD|CE|DOC|ER|HAR|LE|PL|SEC|SWAT)-[0-9]+' | sort | uniq`

if [ "${PURPOSE}" = "saas" ]
then
    FIELD_ID="customfield_10644"
elif [ "${PURPOSE}" = "on-prem" ]
then
    FIELD_ID="customfield_10646"
else
   echo "Unknown purpose ${PURPOSE}"
   exit 1
fi

for KEY in ${KEYS}
do
    echo $KEY
    curl \
       -X PUT \
       --data "{ \"fields\" : { \"${FIELD_ID}\" : \"${VERSION}00\" }}" \
       -H "Content-Type: application/json" \
       https://harness.atlassian.net/rest/api/2/issue/${KEY} \
       --user $JIRA_USERNAME:$JIRA_PASSWORD
done
