#!/bin/bash

git log --remotes=origin/release/* --pretty=oneline --abbrev-commit | grep -iE '\[(CCM|CD|CE|DOC|ER|HAR|LE|PL|SEC|SWAT)-[0-9]+]:' -o | sort | uniq > release.txt
git log --remotes=origin/[m]aster --pretty=oneline --abbrev-commit | grep -iE '\[(CCM|CD|CE|DOC|ER|HAR|LE|PL|SEC|SWAT)-[0-9]+]:' -o | sort | uniq > master.txt

NOT_MERGED=`comm -23 release.txt master.txt | tr '\n' ' '`
echo NOT_MERGED="${NOT_MERGED}" > envvars