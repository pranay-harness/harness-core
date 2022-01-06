# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

set -e

export VERSION=`cat destination/dist/${SERVICE}/version.txt`

export PURPOSE=`cat destination/dist/${SERVICE}/purpose.txt 2>/dev/null`
if [ ! -z "${PURPOSE}" ]
then
    export PURPOSE=/${PURPOSE}
fi


export IMAGE_TAG=`cat destination/dist/${SERVICE}/image_tag.txt 2>/dev/null`
if [ -z "${IMAGE_TAG}" ]
then
    export IMAGE_TAG=${VERSION}
fi

docker login -u _json_key --password-stdin https://us.gcr.io < $GCR_CREDENTIALS

export IMAGE_REPO="us.gcr.io/platform-205701/harness${PURPOSE}/${SNAPSHOT_PREFIX}${SERVICE}:${IMAGE_TAG}"
docker build -t ${IMAGE_REPO} destination/dist/${SERVICE} -f destination/dist/${SERVICE}/Dockerfile-gcr
docker push ${IMAGE_REPO}
