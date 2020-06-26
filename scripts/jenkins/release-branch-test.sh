#!/bin/bash

PROJECTS="CCE|CCM|CDC|CDNG|CDP|CE|CI|CV|CVNG|DEL|DOC|DX|ER|OPS|PL|SEC|SWAT"

git log --remotes=origin/release/* --pretty=oneline --abbrev-commit | grep -iE "\[(${PROJECTS})-[0-9]+]:" -o | sort | uniq > release.txt
git log --remotes=origin/[m]aster --pretty=oneline --abbrev-commit | grep -iE "\[(${PROJECTS})-[0-9]+]:" -o | sort | uniq > master.txt

NOT_MERGED=`comm -23 release.txt master.txt | tr '\n' ' '`
echo NOT_MERGED="${NOT_MERGED}" > envvars
