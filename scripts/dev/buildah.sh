#!/usr/bin/env bash

set -xe
if [ ! -e src/github.com/containers/buildah ]
then
  git clone git@github.com:containers/buildah.git ./src/github.com/containers/buildah
fi

docker build --build-arg=GIT_USER_NAME="$(git config user.name)" --build-arg=GIT_USER_EMAIL="$(git config user.email)" \
    -f ~/github/portal/dockerization/buildah/Dockerfile-fedora-work-env -t buildah-dev .

docker run -it --cap-add=SYS_ADMIN --privileged \
  -v $(pwd):/root/buildah \
  -v /var/lib/containers:/var/lib/containers \
  buildah-dev \
  bash