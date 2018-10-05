#!/usr/bin/env bash

set -e

source utils.sh

yq w -i values.internal.yaml appSecrets.learningEngineSecret $(generateRandomStringOfLength 32)
yq w -i values.internal.yaml secrets.jwtAuthSecret $(generateRandomStringOfLength 80)
yq w -i values.internal.yaml secrets.jwtExternalServiceSecret $(generateRandomStringOfLength 80)
yq w -i values.internal.yaml secrets.jwtMultiAuthSecret $(generateRandomStringOfLength 80)
yq w -i values.internal.yaml secrets.jwtPasswordSecret $(generateRandomStringOfLength 80)
yq w -i values.internal.yaml secrets.jwtSsoRedirectSecret $(generateRandomStringOfLength 80)
yq w -i values.internal.yaml secrets.jwtZendeskSecret $(generateRandomStringOfLength 80)
yq w -i values.internal.yaml accounts.accountId $(generateRandomStringOfLength 22)
yq w -i values.internal.yaml accounts.accountSecret $(generateRandomString)

private_docker_repo=$(yq r values.internal.yaml privatedockerrepo.docker_registry_url)

if [[ $private_docker_repo != "" ]] ; then
   echo "Updating the images with private docker prefix"
    if [[ $(yq r values.internal.yaml images.manager) != $(yq r values.internal.yaml privatedockerrepo.docker_registry_url)* ]]; then
        yq w -i values.internal.yaml images.manager $(yq r values.internal.yaml privatedockerrepo.docker_registry_url)/$(yq r values.internal.yaml images.manager)
    fi
    if [[ $(yq r values.internal.yaml images.le) != $(yq r values.internal.yaml privatedockerrepo.docker_registry_url)* ]]; then
        yq w -i values.internal.yaml images.le $(yq r values.internal.yaml privatedockerrepo.docker_registry_url)/$(yq r values.internal.yaml images.le)
    fi
    if [[ $(yq r values.internal.yaml images.ui) != $(yq r values.internal.yaml privatedockerrepo.docker_registry_url)* ]]; then
        yq w -i values.internal.yaml images.ui $(yq r values.internal.yaml privatedockerrepo.docker_registry_url)/$(yq r values.internal.yaml images.ui)
    fi
    if [[ $(yq r values.internal.yaml images.mongo) != $(yq r values.internal.yaml privatedockerrepo.docker_registry_url)* ]]; then
        yq w -i values.internal.yaml images.mongo $(yq r values.internal.yaml privatedockerrepo.docker_registry_url)/$(yq r values.internal.yaml images.mongo)
    fi
    if [[ $(yq r values.internal.yaml images.defaultBackend) != $(yq r values.internal.yaml privatedockerrepo.docker_registry_url)* ]]; then
        yq w -i values.internal.yaml images.defaultBackend $(yq r values.internal.yaml privatedockerrepo.docker_registry_url)/$(yq r values.internal.yaml images.defaultBackend)
    fi
    if [[ $(yq r values.internal.yaml images.ingressController) != $(yq r values.internal.yaml privatedockerrepo.docker_registry_url)* ]]; then
        yq w -i values.internal.yaml images.ingressController $(yq r values.internal.yaml privatedockerrepo.docker_registry_url)/$(yq r values.internal.yaml images.ingressController)
    fi
    if [[ $(yq r values.internal.yaml images.nginx) != $(yq r values.internal.yaml privatedockerrepo.docker_registry_url)* ]]; then
        yq w -i values.internal.yaml images.nginx $(yq r values.internal.yaml privatedockerrepo.docker_registry_url)/$(yq r values.internal.yaml images.nginx)
    fi
fi

mkdir -p output
helm template -f values.internal.yaml . -x templates/harness-configs.yaml > output/harness-configs.yaml
helm template -f values.internal.yaml . -x templates/harness-ingress-controller.yaml > output/harness-ingress-controller.yaml
helm template -f values.internal.yaml . -x templates/harness-le.yaml > output/harness-le.yaml
helm template -f values.internal.yaml . -x templates/harness-manager.yaml > output/harness-manager.yaml
helm template -f values.internal.yaml . -x templates/harness-namespace.yaml > output/harness-namespace.yaml
helm template -f values.internal.yaml . -x templates/harness-regcred.yaml > output/harness-regcred.yaml
helm template -f values.internal.yaml . -x templates/harness-ui.yaml > output/harness-ui.yaml
helm template -f values.internal.yaml . -x templates/harness-mongo.yaml > output/harness-mongo.yaml

