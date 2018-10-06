export VERSION=`cat destination/dist/manager/version.txt`

docker login -u _json_key --password-stdin https://us.gcr.io < $GCR_CREDENTIALS

docker build -t us.gcr.io/platform-205701/harness/verification-service:${VERSION} destination/dist/verification -f destination/dist/verification/Dockerfile-gcr
docker push us.gcr.io/platform-205701/harness/verification-service:${VERSION}

docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD

docker build -t harness/verification-service:${VERSION} destination/dist/verification
docker push harness/verification-service:${VERSION}
