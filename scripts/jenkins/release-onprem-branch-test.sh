#!/bin/bash
PROJECTS="ART|BT|CCE|CCM|CDC|CDNG|CDP|CE|CI|CV|CVNG|DEL|DOC|DX|ER|FFM|OPS|PL|SEC|SWAT|GTM|ONP"


for line in `git branch -r | grep "release/on-prem" |grep ".xx$"| tail -5`;
	do
	       echo "Onprem branch is $line"
         git log --remotes=$line* --pretty=oneline --abbrev-commit | grep -iE "\[(${PROJECTS})-[0-9]+]:" -o | sort | uniq | tr '[:lower:]' '[:upper:]' > release_onprem_temp.txt
done

cat release_onprem_temp.txt | sort | uniq >release_onprem.txt


echo "Saas branch is ${SAAS_BRANCH}"

git log --remotes=origin/${SAAS_BRANCH}* --pretty=oneline --abbrev-commit | grep -iE "\[(${PROJECTS})-[0-9]+]:" -o | sort | uniq | tr '[:lower:]' '[:upper:]' > release_saas.txt

NOT_MERGED=`comm -23 release_onprem.txt release_saas.txt | tr '\n' ' '`

cat release_onprem.txt | wc -l

cat release_saas.txt | wc -l

if [ -z "$NOT_MERGED" ]
then
      echo "Hotfix changes are reflected in onprem as well"
else
      echo "${NOT_MERGED}"
      exit 1
fi
