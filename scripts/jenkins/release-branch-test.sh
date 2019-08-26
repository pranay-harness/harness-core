#!/bin/bash

git log --remotes=origin/release/* --pretty=oneline --abbrev-commit | grep -iE '\[(HAR|CD|PL|LE|CCM|CE|SWAT|ER|DOC)-[0-9]+]:' -o | sort | uniq > release.txt
git log --remotes=origin/[m]aster --pretty=oneline --abbrev-commit | grep -iE '\[(HAR|CD|PL|LE|CCM|CE|SWAT|ER|DOC)-[0-9]+]:' -o | sort | uniq > master.txt

NOT_MERGED=`comm -23 release.txt master.txt`

if [ ! -z "$NOT_MERGED" ]
then
    echo
    echo
    echo "Jira issues in release branches that are not reflected in master:"
    echo ${NOT_MERGED}
    echo
    echo

    exit 1
fi
