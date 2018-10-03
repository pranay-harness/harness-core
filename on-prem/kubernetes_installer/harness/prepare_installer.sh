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

replace LEARNING_ENGINE_SECRET $learningengine_secret values.yaml
replace JWT_PASSWORD_SECRET $jwtPasswordSecret values.yaml
replace JWT_EXTERNAL_SECRET $jwtExternalServiceSecret values.yaml
replace JWT_ZENDESK_SECRET $jwtZendeskSecret values.yaml
replace JWT_SSO_SECRET $jwtSsoRedirectSecret values.yaml
replace JWT_MULTI_AUTH_SECRET $jwtMultiAuthSecret values.yaml
replace ACCOUNTID $accountId values.yaml
replace ACCOUNTSECRET $account_secret values.yaml
replace JWT_AUTH_SECRET $jwtAuthSecret values.yaml

private_docker_repo=$(yq r values.yaml privatedockerrepo.docker_registry_url)

if [[ $private_docker_repo != "" ]] ; then
   echo "Updating the images with private docker prefix"
    if [[ $(yq r values.yaml images.manager) != $(yq r values.yaml privatedockerrepo.docker_registry_url)* ]]; then
        yq w -i values.yaml images.manager $(yq r values.yaml privatedockerrepo.docker_registry_url)/$(yq r values.yaml images.manager)
    fi
    if [[ $(yq r values.yaml images.le) != $(yq r values.yaml privatedockerrepo.docker_registry_url)* ]]; then
        yq w -i values.yaml images.le $(yq r values.yaml privatedockerrepo.docker_registry_url)/$(yq r values.yaml images.le)
    fi
    if [[ $(yq r values.yaml images.ui) != $(yq r values.yaml privatedockerrepo.docker_registry_url)* ]]; then
        yq w -i values.yaml images.ui $(yq r values.yaml privatedockerrepo.docker_registry_url)/$(yq r values.yaml images.ui)
    fi
    if [[ $(yq r values.yaml images.mongo) != $(yq r values.yaml privatedockerrepo.docker_registry_url)* ]]; then
        yq w -i values.yaml images.mongo $(yq r values.yaml privatedockerrepo.docker_registry_url)/$(yq r values.yaml images.mongo)
    fi
    if [[ $(yq r values.yaml images.defaultBackend) != $(yq r values.yaml privatedockerrepo.docker_registry_url)* ]]; then
        yq w -i values.yaml images.defaultBackend $(yq r values.yaml privatedockerrepo.docker_registry_url)/$(yq r values.yaml images.defaultBackend)
    fi
    if [[ $(yq r values.yaml images.ingressController) != $(yq r values.yaml privatedockerrepo.docker_registry_url)* ]]; then
        yq w -i values.yaml images.ingressController $(yq r values.yaml privatedockerrepo.docker_registry_url)/$(yq r values.yaml images.ingressController)
    fi
    if [[ $(yq r values.yaml images.nginx) != $(yq r values.yaml privatedockerrepo.docker_registry_url)* ]]; then
        yq w -i values.yaml images.nginx $(yq r values.yaml privatedockerrepo.docker_registry_url)/$(yq r values.yaml images.nginx)
    fi
fi

mkdir -p output
helm template . -x templates/harness-configs.yaml > output/harness-configs.yaml
helm template . -x templates/harness-ingress-controller.yaml > output/harness-ingress-controller.yaml
helm template . -x templates/harness-le.yaml > output/harness-le.yaml
helm template . -x templates/harness-manager.yaml > output/harness-manager.yaml
helm template . -x templates/harness-namespace.yaml > output/harness-namespace.yaml
helm template . -x templates/harness-regcred.yaml > output/harness-regcred.yaml
helm template . -x templates/harness-ui.yaml > output/harness-ui.yaml
helm template . -x templates/harness-mongo.yaml > output/harness-mongo.yaml

