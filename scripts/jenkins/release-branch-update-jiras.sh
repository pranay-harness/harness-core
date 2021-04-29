#!/bin/bash
PROJECTS="BT|CCE|CCM|CDC|CDNG|CDP|CE|CI|CV|CVNG|DEL|DOC|DX|ER|FFM|OPS|PL|SEC|SWAT|GTM|ONP"
KEYS=`git log --pretty=oneline --abbrev-commit |\
      awk "/${PREVIOUS_CUT_COMMIT_MESSAGE}/ {exit} {print}" |\
      grep -o -iE '(BT|CCE|CCM|CDC|CDNG|CDP|CE|CI|CV|CVNG|DEL|DOC|DX|ER|FFM|OPS|PL|SEC|SWAT|GTM|ONP)-[0-9]+' | sort | uniq`
#Getting the dummy commits that are made to the branch and creating a ticket out of it
git log --pretty=oneline --abbrev-commit |\
      awk "/${PREVIOUS_CUT_COMMIT_MESSAGE}/ {exit} {print}" |\
      grep -iE "\[(${PROJECTS})-0]:.*" -o | sort | uniq  | tr '\n' ',' > dummyJiraList.txt
dummyJiraList=$(sed 's/,/\\n/g' dummyJiraList.txt)

#Creating a ticket for such tickets
if [ -s dummyJiraList.txt ]
then
  response=$(curl -X POST \
  https://harness.atlassian.net/rest/api/2/issue/ \
  -H 'authorization: Basic Ym9vcGVzaC5zaGFubXVnYW1AaGFybmVzcy5pbzpYVWVFTTFJbzBzWUttd1FINWJLZDA3MEU=' \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -H 'postman-token: 53e72059-f543-f0c7-f60f-c14bc2e5d502' \
  -d '{
    "fields": {
       "project":
       {
          "key": "ART"
       },
       "summary": "Contains the details of untracked changes going all the release build:'$VERSION'00",
       "description": "'{code}"$dummyJiraList"{code}'",
       "issuetype": {
          "name": "Story"
       }
   }
}')
  ticketId=$(echo $response | grep -o -iE '(ART)-[0-9]+')
else
  echo "No Dummy Commits"
fi


#Assigning the Release BE Number to this ticket
if [ -z "$ticketId" ]
then
  echo "No Ticket is Created as there are no dummy commits"
else
  KEYS="${KEYS} ${ticketId}"
fi

#Updating all the Jira tickets with the Release Build Number
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
