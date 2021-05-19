#!/bin/bash

set +e

PROJECTS="BT|CCE|CCM|CDC|CDNG|CDP|CE|CI|CV|CVNG|DEL|DOC|DX|ER|OPS|PL|SEC|SWAT|GTM|FFM|ONP"

# Check commit message if there's a single commit
if [ $(git rev-list --count $ghprbActualCommit ^origin/master)  -eq 1 ]; then
    ghprbPullTitle=$(git log -1 --format="%s" $ghprbActualCommit)
fi

PR_MESSAGE=`echo "${ghprbPullTitle}" | grep -iE "\[(${PROJECTS})-[0-9]+]:"`

if [ -z "$PR_MESSAGE" ]
then
    echo The PR title \"${ghprbPullTitle}\"
    echo "does not match the expectations"
    echo "Make sure that your message starts with [${PROJECTS}-<number>]: <description>"
    exit 1
fi

KEY=`echo "${ghprbPullTitle}" | grep -o -iE "(${PROJECTS})-[0-9]+"`

echo $KEY

echo "Calling Jira API"
echo "https://harness.atlassian.net/rest/api/3/issue/${KEY}?fields=id"

curl -v https://harness.atlassian.net/rest/api/3/issue/${KEY}?fields=id  -H "Authorization: Basic ${JIRA_TOKEN}" -o result.txt > /dev/null 2>&1

echo "Result Received"

cat result.txt

if  grep -q "Issue does not exist" result.txt; then
    echo "${KEY}" is not valid JIRA Key.
    echo Please create a Jira if it does not exist
    exit 1
fi

#TODO: enable priorities check

#AUTHOR=`echo ${ghprbPullAuthorEmail} | sed 's/\(.*\)@harness.io$/\1/g'`

#curl -v https://harness.atlassian.net/rest/api/3/search?jql=filter=12111%20AND%20assignee=$AUTHOR\&fields=key --user $JIRA_USERNAME:$JIRA_PASSWORD -o result.txt > /dev/null 2>&1

#PRIORITY_ISSUES=`cat result.txt | tr "," "\n" | grep -o -e "HAR-[0-9]+"`

#if [ ! -z "$PRIORITY_ISSUES" ]
#then
#    if ! echo "${PRIORITY_ISSUES}" | grep ${KEY}
#    then
#        echo The issue ${KEY} is not your imidiate priority.
#        echo Please first address the folowing issues: ${PRIORITY_ISSUES}
#        #exit 1
#    fi
#fi
