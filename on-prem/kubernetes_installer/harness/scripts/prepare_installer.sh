#!/usr/bin/env bash

set -e

source utils.sh

learningengine_secret=$(generateRandomStringOfLength 32)
account_secret=$(generateRandomString)
jwtPasswordSecret=$(generateRandomStringOfLength 80)
jwtAuthSecret=$(generateRandomStringOfLength 80)
jwtExternalServiceSecret=$(generateRandomStringOfLength 80)
jwtZendeskSecret=$(generateRandomStringOfLength 80)
jwtMultiAuthSecret=$(generateRandomStringOfLength 80)
jwtSsoRedirectSecret=$(generateRandomStringOfLength 80)
accountId=$(generateRandomStringOfLength 22)

replace LEARNING_ENGINE_SECRET $learningengine_secret values.internal.yaml
replace JWT_PASSWORD_SECRET $jwtPasswordSecret values.internal.yaml
replace JWT_EXTERNAL_SECRET $jwtExternalServiceSecret values.internal.yaml
replace JWT_ZENDESK_SECRET $jwtZendeskSecret values.internal.yaml
replace JWT_SSO_SECRET $jwtSsoRedirectSecret values.internal.yaml
replace JWT_MULTI_AUTH_SECRET $jwtMultiAuthSecret values.internal.yaml
replace ACCOUNTID $accountId values.internal.yaml
replace ACCOUNTSECRET $account_secret values.internal.yaml
replace JWT_AUTH_SECRET $jwtAuthSecret values.internal.yaml

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

