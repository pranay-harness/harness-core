#!/bin/bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

set +e

PROJECTS="BT|CCE|CCM|CDC|CDNG|CDP|CE|CI|CV|CVNG|DEL|DOC|DX|ER|OPS|PIE|PL|SEC|SWAT|GTM|FFM|OPA|ONP|LWG|ART"

# Check commit message if there's a single commit
if [ $(git rev-list --count $ghprbActualCommit ^origin/master)  -eq 1 ]; then
    ghprbPullTitle=$(git log -1 --format="%s" $ghprbActualCommit)
fi
KEY=`echo "${ghprbPullTitle}" | grep -o -iE "\[(${PROJECTS})-[0-9]+]:" | grep -o -iE "(${PROJECTS})-[0-9]+"`

echo "JIRA Key is : $KEY "

jira_response=`curl -X GET -H "Content-Type: application/json" https://harness.atlassian.net/rest/api/2/issue/${KEY}?fields=issuetype,customfield_10687,customfield_10709,customfield_10748,customfield_10763,customfield_10785 --user $JIRA_USERNAME:$JIRA_PASSWORD`

issuetype=`echo "${jira_response}" | jq ".fields.issuetype.name" | tr -d '"'`
if [[ $KEY == BT-* ]]
then
  bug_resolution="n/a"
  what_changed="n/a"
  ff_added="n/a"
  jira_resolved_as="n/a"
  phase_injected="n/a"
else
  bug_resolution=`echo "${jira_response}" | jq ".fields.customfield_10687" | tr -d '"'`
  what_changed=`echo "${jira_response}" | jq ".fields.customfield_10763" | tr -d '"'`
  ff_added=`echo "${jira_response}" | jq ".fields.customfield_10785.value" | tr -d '"'`
  jira_resolved_as=`echo "${jira_response}" | jq ".fields.customfield_10709" | tr -d '"'`
  phase_injected=`echo "${jira_response}" | jq ".fields.customfield_10748" | tr -d '"'`
fi

echo "issueType is ${issuetype}"
echo "INFO: Checking JIRA STATUS OF issueType ${issuetype}"

if [[ "${issuetype}" = "Bug" && ( "${bug_resolution}" = "" || "${jira_resolved_as}" = "null" || "${phase_injected}" = "null" || "${what_changed}" = "null" ) ]]
then
      if [[ -z ${bug_resolution} ]]
      then
        echo "ERROR: JIRA FIELD: bug resolution is empty"
      fi

      if [[ "${jira_resolved_as}" = "null" ]]
      then
        echo "ERROR: JIRA FIELD: jira_resolved_as is not selected"
      fi

      if [[ "${phase_injected}" = "null" ]]
      then
        echo "ERROR: JIRA FIELD: phase_injected is not selected"
      fi

      if [[ "${what_changed}" = "null" ]]
      then
        echo "ERROR: JIRA FIELD: what_changed is not updated"
      fi
      exit 1
fi

if [[ "${issuetype}" = "Story" && ( "${ff_added}" = "null" || "${what_changed}" = "null" ) ]]
then
      if [[ "${ff_added}" = "null" ]]
      then
        echo "ERROR: JIRA FIELD: FF added is not updated, Please update FF added to proceed"
      fi
      if [[ "${what_changed}" = "null" ]]
      then
        echo "ERROR: JIRA FIELD: what_changed is not updated"
      fi
      exit 1
fi

echo "JIRA Key is : $KEY is having all the mandatory details"
