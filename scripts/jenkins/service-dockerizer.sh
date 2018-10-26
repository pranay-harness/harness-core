export VERSION=`cat destination/dist/${SERVICE}/version.txt`
export PURPOSE=`cat destination/dist/${SERVICE}/purpose.txt 2>/dev/null`

if [ ! -z "${PURPOSE}" ]
then
    export PURPOSE=/${PURPOSE}
fi


docker login -u _json_key --password-stdin https://us.gcr.io < $GCR_CREDENTIALS

export IMAGE_TAG="us.gcr.io/platform-205701/harness${PURPOSE}/${SNAPSHOT_PREFIX}${SERVICE}:${VERSION}"
docker build -t ${IMAGE_TAG} destination/dist/${SERVICE} -f destination/dist/${SERVICE}/Dockerfile-gcr
docker push ${IMAGE_TAG}

if [ -z "${SNAPSHOT_PREFIX}" ]
then
    docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD

    docker build -t harness/${SERVICE}:${VERSION} destination/dist/${SERVICE}
    docker push harness/${SERVICE}:${VERSION}
fi