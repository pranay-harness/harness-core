set -x

export VERSION=`cat destination/dist/${SERVICE}/version.txt`
export PURPOSE=`cat destination/dist/${SERVICE}/purpose.txt 2>/dev/null`
export JDK=`cat destination/dist/jdk.txt 2>/dev/null`

if [ ! -z "${PURPOSE}" ]
then
    export PURPOSE=/${PURPOSE}-${JDK}
    export SERVICEDIR=${SERVICE}
else
    export SERVICEDIR=${SERVICE}-${JDK}
fi


docker login -u _json_key --password-stdin https://us.gcr.io < $GCR_CREDENTIALS

export IMAGE_TAG="us.gcr.io/platform-205701/harness${PURPOSE}/${SNAPSHOT_PREFIX}${SERVICEDIR}:${VERSION}"
docker build -t ${IMAGE_TAG} destination/dist/${SERVICE} -f destination/dist/${SERVICE}/Dockerfile-gcr
docker push ${IMAGE_TAG}

if [ "${PURPOSE}" = /on-prem ]
then
    docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD

    docker build -t harness/${SERVICE}:${VERSION} destination/dist/${SERVICE}
    docker push harness/${SERVICE}:${VERSION}
fi
