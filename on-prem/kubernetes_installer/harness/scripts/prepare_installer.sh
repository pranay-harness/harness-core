#!/usr/bin/env bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
INSTALLER_DIR="$(dirname ${SCRIPT_DIR})"
source ${SCRIPT_DIR}/utils.sh

wv appSecrets.learningEngineSecret $(generateRandomStringOfLength 32)
wv secrets.jwtAuthSecret $(generateRandomStringOfLength 80)
wv secrets.jwtExternalServiceSecret $(generateRandomStringOfLength 80)
wv secrets.jwtMultiAuthSecret $(generateRandomStringOfLength 80)
wv secrets.jwtPasswordSecret $(generateRandomStringOfLength 80)
wv secrets.jwtSsoRedirectSecret $(generateRandomStringOfLength 80)
wv secrets.jwtZendeskSecret $(generateRandomStringOfLength 80)
wv accounts.accountId $(generateRandomStringOfLength 22)
wv accounts.accountSecret $(generateRandomString)

DOCKER_REGISTRY_URL=$(rv docker.registry.url)

if [[ ${DOCKER_REGISTRY_URL} != "" ]] ; then
    echo "Updating the images with private docker prefix"

    wv images.manager.repository ${DOCKER_REGISTRY_URL}/$(rv images.manager.repository)
    wv images.le.repository ${DOCKER_REGISTRY_URL}/$(rv images.le.repository)
    wv images.ui.repository ${DOCKER_REGISTRY_URL}/$(rv images.ui.repository)
    wv images.mongo.repository ${DOCKER_REGISTRY_URL}/$(rv images.mongo.repository)
    wv images.mongoInstall.repository ${DOCKER_REGISTRY_URL}/$(rv images.mongoInstall.repository)
    wv images.defaultBackend.repository ${DOCKER_REGISTRY_URL}/$(rv images.defaultBackend.repository)
    wv images.ingressController.repository ${DOCKER_REGISTRY_URL}/$(rv images.ingressController.repository)
    wv images.nginx.repository ${DOCKER_REGISTRY_URL}/$(rv images.nginx.repository)
    wv images.delegate.repository ${DOCKER_REGISTRY_URL}/$(rv images.delegate.repository)
fi

mkdir -p output
helm template -f values.internal.yaml . -x templates/harness-configs.yaml > output/harness-configs.yaml
helm template -f values.internal.yaml . -x templates/harness-ingress-controller.yaml > output/harness-ingress-controller.yaml
helm template -f values.internal.yaml . -x templates/harness-le.yaml > output/harness-le.yaml
helm template -f values.internal.yaml . -x templates/harness-manager.yaml > output/harness-manager.yaml
helm template -f values.internal.yaml . -x templates/harness-namespace.yaml > output/harness-namespace.yaml
helm template -f values.internal.yaml . -x templates/harness-regcred.yaml > output/harness-regcred.yaml
helm template -f values.internal.yaml . -x templates/harness-ui.yaml > output/harness-ui.yaml
helm template -f ${INSTALLER_DIR}/values.internal.yaml . -x ${INSTALLER_DIR}/templates/init.template.js > ${INSTALLER_DIR}/output/init.js
# To remove the top 2 unnecessary lines in the generated js using template.
tail -n +3 ${INSTALLER_DIR}/output/init.js > ${INSTALLER_DIR}/output/init.js.tmp
rm -f ${INSTALLER_DIR}/output/init.js
mv ${INSTALLER_DIR}/output/init.js.tmp ${INSTALLER_DIR}/output/init.js

wv services.mongo.configMap.init "$(cat ${INSTALLER_DIR}/output/init.js)"

${SCRIPT_DIR}/prepare_mongodb_replicaset.sh
