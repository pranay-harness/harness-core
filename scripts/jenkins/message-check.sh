#!/bin/bash

set +e

# Check commit message if there's a single commit
if [ $(git rev-list --count $ghprbActualCommit ^origin/master)  -eq 1 ]; then
    ghprbPullTitle=$(git log -1 --format="%s" $ghprbActualCommit)
fi

PR_MESSAGE=`echo "${ghprbPullTitle}" | grep -iE '\[(CCM|CCE|CD|CDP|CDC|CE|CI|CV|DOC|ER|HAR|LE|PL|SEC|SWAT|DX)-[0-9]+]:'`

if [ -z "$PR_MESSAGE" ]
then
    echo The PR title \"${ghprbPullTitle}\"
    echo "does not match the expectations"
    echo 'Make sure that your message starts with [CCM|CCE|CD|CDP|CDC|CE|CI|CV|DOC|ER|HAR|LE|PL|SEC|SWAT|DX-<number>]: <description>'
    exit 1
fi

KEY=`echo "${ghprbPullTitle}" | grep -o -iqE '(CCM|CCE|CD|CDP|CDC|CE|CI|CV|DOC|ER|HAR|LE|PL|SEC|SWAT|DX)-[0-9]+'`


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
