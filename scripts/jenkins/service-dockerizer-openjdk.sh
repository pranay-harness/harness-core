set -x

export VERSION=`cat destination/dist/${SERVICE}/version.txt`
export JDK=`cat destination/dist/jdk.txt 2>/dev/null`


export PURPOSE=`cat destination/dist/${SERVICE}/purpose.txt 2>/dev/null`
if [ ! -z "${PURPOSE}" ]
then
    export PURPOSE=/${PURPOSE}-${JDK}
    export SERVICEDIR=${SERVICE}
else
    export SERVICEDIR=${SERVICE}-${JDK}
fi

export IMAGE_TAG=`cat destination/dist/${SERVICE}/image_tag.txt 2>/dev/null`
if [ -z "${IMAGE_TAG}" ]
then
    export IMAGE_TAG=${VERSION}
fi

docker login -u _json_key --password-stdin https://us.gcr.io < $GCR_CREDENTIALS

export IMAGE_REPO="us.gcr.io/platform-205701/harness${PURPOSE}/${SNAPSHOT_PREFIX}${SERVICEDIR}:${IMAGE_TAG}"
docker build -t ${IMAGE_REPO} destination/dist/${SERVICE} -f destination/dist/${SERVICE}/Dockerfile-gcr
docker push ${IMAGE_REPO}

if [ "${PURPOSE}" = "/on-prem-${JDK}" ]
then
    docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD

    docker build -t harness/${SERVICE}:${IMAGE_TAG} destination/dist/${SERVICE}
    docker push harness/${SERVICE}:${IMAGE_TAG}
fi
