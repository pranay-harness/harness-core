#!/usr/bin/env bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
