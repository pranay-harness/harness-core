export VERSION=`cat destination/dist/manager/version.txt`

docker login -u _json_key --password-stdin https://us.gcr.io < $GCR_CREDENTIALS

docker build -t us.gcr.io/platform-205701/harness/manager:${VERSION} destination/dist/manager -f destination/dist/manager/Dockerfile-gcr
docker push us.gcr.io/platform-205701/harness/manager:${VERSION}


docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD

docker build -t harness/manager:${VERSION} destination/dist/manager
docker push harness/manager:${VERSION}
