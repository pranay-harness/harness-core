#!/bin/bash

set -e

PROJECTS="ART|BT|CCE|CCM|CDC|CDNG|CDP|CE|CI|CV|CVNG|DEL|DOC|DX|ER|FFM|OPS|PIP|PL|SEC|SWAT|GTM|ONP"

cat <<EOF > ${HOME}/.netrc
machine ${DRONE_NETRC_MACHINE}
login ${DRONE_NETRC_USERNAME}
password ${DRONE_NETRC_PASSWORD}
EOF

git fetch --unshallow

git fetch --all

git log --remotes=origin/release/* --pretty=oneline --abbrev-commit | grep -iE "\[(${PROJECTS})-[0-9]+]:" -o | sort | uniq | tr '[:lower:]' '[:upper:]' > release.txt
git log --remotes=origin/[m]aster --pretty=oneline --abbrev-commit | grep -iE "\[(${PROJECTS})-[0-9]+]:" -o | sort | uniq | tr '[:lower:]' '[:upper:]' > master.txt
NOT_MERGED=`comm -23 release.txt master.txt | tr '\n' ' '`

if [ -z "$NOT_MERGED" ]
then
      echo "All Hotfix changes are reflected in Master as well" > envvars
else
      echo NOT_MERGED="${NOT_MERGED}" > envvars
fi
