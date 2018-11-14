# Check for not merged hot fixes
git log --remotes=origin/release/* --pretty=oneline --abbrev-commit | grep -e "\[HAR-[0-9]\+]:" -o | sort | uniq > release.txt
git log --remotes=origin/[m]aster --pretty=oneline --abbrev-commit | grep -e "\[HAR-[0-9]\+]:" -o | sort | uniq > master.txt

NOT_MERGED=`comm -23 release.txt master.txt`

if [ ! -z "$NOT_MERGED" ]
then
    echo "There are jira issues in release branches that are not reflected in master."
    exit 1
fi


# initialize variables
export VERSION_FILE=pom.xml

export VERSION=`cat ${VERSION_FILE} |\
    grep '<build.number>' |\
    sed -e 's: *<build.number>::g' | sed -e 's:</build.number>::g'`
export VERSION=${VERSION%??}
export NEW_VERSION=$(( ${VERSION}+1 ))

export BRANCH=`echo "${GIT_BRANCH}" | sed -e "s/origin\///g"`
export SHA=`git rev-parse HEAD`


# Update jira issues
KEYS=`git log --pretty=oneline --abbrev-commit | awk "/Branching to release\/${PURPOSE}/ {exit} {print}" | grep -o -e "HAR-[0-9]\+" | sort | uniq`

if [ "${PURPOSE}" = "saas" ]
then
    FIELD_ID="customfield_10634"
elif [ "${PURPOSE}" = "on-prem" ]
then
    FIELD_ID="customfield_10636"
else
   echo "Unknown purpose ${PURPOSE}"
   exit 1
fi

for KEY in ${KEYS}
do
    echo $KEY
    curl \
       -X PUT \
       --data "{ \"fields\" : { \"${FIELD_ID}\" : \"${VERSION}xx\" }}" \
       -H "Content-Type: application/json" \
       https://harness.atlassian.net/rest/api/2/issue/${KEY} \
       --user $JIRA_USERNAME:$JIRA_PASSWORD

done


# Prepare new release commit
git checkout -b release/${VERSION}
git checkout ${BRANCH}

sed -i "s:<build.number>${VERSION}00</build.number>:<build.number>${NEW_VERSION}00</build.number>:g" ${VERSION_FILE}
git add ${VERSION_FILE}
git commit -m "Branching to release/${PURPOSE}/${VERSION}xx. New version ${NEW_VERSION}xx"


# Export variables
echo VERSION=${VERSION} > build.properties
echo BRANCH=${BRANCH} >> build.properties
echo SHA=${SHA} >> build.properties