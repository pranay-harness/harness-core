export VERSION_FILE=pom.xml

export VERSION=`cat ${VERSION_FILE} |\
    grep '<build.number>' |\
    sed -e 's: *<build.number>::g' | sed -e 's:</build.number>::g'`
export VERSION=${VERSION%??}
export NEW_VERSION=$(( ${VERSION}+1 ))

export BRANCH=`echo "${GIT_BRANCH}" | sed -e "s/origin\///g"`
export SHA=`git rev-parse HEAD`

git checkout -b release/${VERSION}
git checkout ${BRANCH}


sed -i "s:<build.number>${VERSION}00</build.number>:<build.number>${NEW_VERSION}00</build.number>:g" ${VERSION_FILE}
git add ${VERSION_FILE}
git commit -m "Branching to release/${PURPOSE}/${VERSION}. New version ${NEW_VERSION}"

echo VERSION=${VERSION} > build.properties
echo BRANCH=${BRANCH} >> build.properties
echo SHA=${SHA} >> build.properties