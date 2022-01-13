#!/usr/bin/env bash
# Copyright 2018 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

#Assumption:
#
#Repo: harness-on-prem
#master: common templatized - Setup + Applications and so on. (No encrypted reference)
#
#
#
#Shell Script - Wrapper
#
#Input
#        - property file -> Account Name, Company Name, Account Admin Name, Admin Email
#        - property file -> Infra details
#
#Args: Prod Auth Token
#
#Steps:
#- REST: Create Account Customer.com in prod
#- REST: create secrets
#
#- Checkout master
#- create branch
#- Replace placeholder
#- Replace encrypted text/file placeholders
#- Checkin back into the branch
#
#- REST: Setup Yaml repo (branch) for this account

if [ "$#" -ne 2 ]; then
    echo "Please enter the correct arguments for running the new account creation script"
    echo "1: API URL"
    echo "2: Token"
    exit 1
fi

ACCOUNT_PROPERTY_FILE=accountdetails.properties
INFRA_PROPERTY_FILE=inframapping.properties
CONFIG_PROPERTY_FILE=config.properties
API_URL=$1
TOKEN=$2

function getProperty () {
   FILENAME=$1
   PROP_KEY=$2
   PROP_VALUE=`cat "$FILENAME" | grep "$PROP_KEY" | cut -d'=' -f2`
   echo $PROP_VALUE
}

function generateRandomString(){
   echo `hexdump -n 16 -e '4/4 "%08X" 1 "\n"' /dev/urandom`
}

function generateRandomStringOfLength(){
    LENGTH=$1
    echo `cat /dev/urandom | LC_CTYPE=C tr -dc "[:alnum:]" | head -c $LENGTH`
}

function replace() {
        if [[ "$OSTYPE" == "darwin"* ]]; then
                find Setup -type f -name "*.yaml" -exec sed -i '' -e "s|$1|$2|g" {} +
        else
                find Setup -type f -name "*.yaml" -exec sed -i "s|$1|$2|g" {} +
        fi
}
echo "# Reading account details from $ACCOUNT_PROPERTY_FILE"
accountName=$(getProperty "$ACCOUNT_PROPERTY_FILE" "AccountName")
companyName=$(getProperty $ACCOUNT_PROPERTY_FILE "CompanyName")
adminEmail=$(getProperty $ACCOUNT_PROPERTY_FILE "AdminEmail")


echo "Reading Infra mapping from $INFRA_PROPERTY_FILE"
host1=$(getProperty "$INFRA_PROPERTY_FILE" "HOST1_IP_ADDRESS")
host2=$(getProperty "$INFRA_PROPERTY_FILE" "HOST2_IP_ADDRESS")
host3=$(getProperty "$INFRA_PROPERTY_FILE" "HOST3_IP_ADDRESS")
loadbalancer=$(getProperty "$INFRA_PROPERTY_FILE" "LOAD_BALANCER_URL")
sshUser=$(getProperty $INFRA_PROPERTY_FILE "sshUser")
sshKeyPath=$(getProperty $INFRA_PROPERTY_FILE "sshKeyPath")

echo "Reading config mapping from $CONFIG_PROPERTY_FILE"
mongodbUserName=$(getProperty "$CONFIG_PROPERTY_FILE" "mongodbUserName" | base64 --decode)
mongodbPassword=$(getProperty "$CONFIG_PROPERTY_FILE" "mongodbPassword" | base64 --decode)
dockerPassword=$(getProperty "$CONFIG_PROPERTY_FILE" "dockerPassword"  | base64 --decode)
newreliclicensekey=$(getProperty "$CONFIG_PROPERTY_FILE" "newreliclicensekey" | base64 --decode)

echo "#######Account details#############"
echo "AccountName="$accountName
echo "CompanyName="$companyName
echo "AdminEmail="$adminEmail

printf "\n"

echo "#######Infrastructure details #############"
echo "host1="$host1
echo "host2="$host2
echo "host3="$host3
echo "loadbalancer="$loadbalancer
echo "sshUser="$sshUser
echo "sshKeyPath="$sshKeyPath

###### ACCOUNT CREATION SECTION START ##################################

curlstatement="curl -X POST -k $API_URL/api/users/account -H 'Authorization: Bearer $TOKEN' -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' -d '{ \"accountName\":\"$accountName\", \"companyName\":\"$companyName\" }'"

echo "curl command sent is : "$curlstatement

response="$(eval $curlstatement)"
echo "Response is "$response

#response='{"metaData":{},"resource":{"uuid":"P-280t2zQ5C8idcb8_-64Q","appId":"__GLOBAL_APP_ID__","createdBy":null,"createdAt":1520543497776,"lastUpdatedBy":{"uuid":"c0RigPdWTlOCUeeAsdolJQ","name":"Admin","email":"admin@harness.io"},"lastUpdatedAt":1520543497776,"keywords":null,"companyName":"BigBank","accountName":"BigBank","accountKey":"83e3f0b0271f180238525d6f394eed9e","licenseId":null,"licenseExpiryTime":0},"responseMessages":[]}'

if [[ $response = *"$accountName"* ]]; then
  echo "Account creation is successful"
else
  echo "Account creation failed"
  exit 1;
fi

accountId=$(echo $response | cut -d "," -f2 | cut -d ":" -f3 | cut -d "\"" -f2)

echo "AccountID="$accountId

###### ACCOUNT CREATION SECTION END ##################################


####Generate secrets section START ######################################

learningengine_secret=$(generateRandomString)
learningengine_curl_statement="curl -X POST -k '$API_URL/api/secrets/add-local-secret?accountId=$accountId' -H 'Authorization: Bearer $TOKEN' -H 'Content-Type: application/json' -d '{ \"name\":\"LEARNING_ENGINE_SECRET\",\"value\":\"$learningengine_secret\"}'"
echo "learningengine_curl_statement sent is " $learningengine_curl_statement
learning_engine_response="$(eval $learningengine_curl_statement)"
echo "LearningEngine response = " $learning_engine_response
learning_engine_secret_token="$(echo $learning_engine_response | awk -F "," '{print $2}' | awk -F ":" '{print $2}' | awk -F "\"" '{print $2}')"
echo "learning_engine_secret_token=" $learning_engine_secret_token

account_secret=$(generateRandomString)
account_secret_curl_statement="curl -X POST -k '$API_URL/api/secrets/add-local-secret?accountId=$accountId' -H 'Authorization: Bearer $TOKEN' -H 'Content-Type: application/json' -d '{ \"name\":\"ACCOUNT_SECRET_KEY\",\"value\":\"$account_secret\"}'"
echo "account_secret_curl_statement sent is " $account_secret_curl_statement
account_secret_response="$(eval $account_secret_curl_statement)"
echo "account_secret response = " $account_secret_response
account_secret_token="$(echo $account_secret_response | awk -F "," '{print $2}' | awk -F ":" '{print $2}' | awk -F "\"" '{print $2}')"
echo "account_secret_token="$account_secret_token

jwtPasswordSecret=$(generateRandomStringOfLength 80)
jwtExternalServiceSecret=$(generateRandomStringOfLength 80)
jwtZendeskSecret=$(generateRandomStringOfLength 80)
jwtMultiAuthSecret=$(generateRandomStringOfLength 80)
jwtSsoRedirectSecret=$(generateRandomStringOfLength 80)

jwtPasswordSecret_curl_statement="curl -X POST -k '$API_URL/api/secrets/add-local-secret?accountId=$accountId' -H 'Authorization: Bearer $TOKEN' -H 'Content-Type: application/json' -d '{ \"name\":\"jwtPasswordSecret\",\"value\":\"$jwtPasswordSecret\"}'"
echo "jwtPasswordSecret_curl_statement sent is " $jwtPasswordSecret_curl_statement
jwtPasswordSecret_response="$(eval $jwtPasswordSecret_curl_statement)"
echo "jwtPasswordSecret response = " $jwtPasswordSecret_response
jwtPasswordSecret_token="$(echo $jwtPasswordSecret_response | awk -F "," '{print $2}' | awk -F ":" '{print $2}' | awk -F "\"" '{print $2}')"
echo "jwtPasswordSecret_token="$jwtPasswordSecret_token

jwtExternalServiceSecret_curl_statement="curl -X POST -k '$API_URL/api/secrets/add-local-secret?accountId=$accountId' -H 'Authorization: Bearer $TOKEN' -H 'Content-Type: application/json' -d '{ \"name\":\"jwtExternalServiceSecret\",\"value\":\"$jwtExternalServiceSecret\"}'"
echo "jwtExternalServiceSecret_curl_statement sent is " $jwtExternalServiceSecret_curl_statement
jwtExternalServiceSecret_response="$(eval $jwtExternalServiceSecret_curl_statement)"
echo "jwtExternalServiceSecret response = " $jwtExternalServiceSecret_response
jwtExternalServiceSecret_token="$(echo $jwtExternalServiceSecret_response | awk -F "," '{print $2}' | awk -F ":" '{print $2}' | awk -F "\"" '{print $2}')"
echo "jwtExternalServiceSecret_token="$jwtExternalServiceSecret_token

jwtZendeskSecret_curl_statement="curl -X POST -k '$API_URL/api/secrets/add-local-secret?accountId=$accountId' -H 'Authorization: Bearer $TOKEN' -H 'Content-Type: application/json' -d '{ \"name\":\"jwtZendeskSecret\",\"value\":\"$jwtZendeskSecret\"}'"
echo "jwtZendeskSecret_curl_statement sent is " $jwtZendeskSecret_curl_statement
jwtZendeskSecret_response="$(eval $jwtZendeskSecret_curl_statement)"
echo "jwtZendeskSecret response = " $jwtZendeskSecret_response
jwtZendeskSecret_token="$(echo $jwtZendeskSecret_response | awk -F "," '{print $2}' | awk -F ":" '{print $2}' | awk -F "\"" '{print $2}')"
echo "jwtZendeskSecret_token="$jwtZendeskSecret_token

jwtMultiAuthSecret_curl_statement="curl -X POST -k '$API_URL/api/secrets/add-local-secret?accountId=$accountId' -H 'Authorization: Bearer $TOKEN' -H 'Content-Type: application/json' -d '{ \"name\":\"jwtMultiAuthSecret\",\"value\":\"$jwtMultiAuthSecret\"}'"
echo "jwtMultiAuthSecret_curl_statement sent is " $jwtMultiAuthSecret_curl_statement
jwtMultiAuthSecret_response="$(eval $jwtMultiAuthSecret_curl_statement)"
echo "jwtMultiAuthSecret response = " $jwtMultiAuthSecret_response
jwtMultiAuthSecret_token="$(echo $jwtMultiAuthSecret_response | awk -F "," '{print $2}' | awk -F ":" '{print $2}' | awk -F "\"" '{print $2}')"
echo "jwtMultiAuthSecret_token="$jwtMultiAuthSecret_token

jwtSsoRedirectSecret_curl_statement="curl -X POST -k '$API_URL/api/secrets/add-local-secret?accountId=$accountId' -H 'Authorization: Bearer $TOKEN' -H 'Content-Type: application/json' -d '{ \"name\":\"jwtSsoRedirectSecret\",\"value\":\"$jwtSsoRedirectSecret\"}'"
echo "jwtSsoRedirectSecret_curl_statement sent is " $jwtSsoRedirectSecret_curl_statement
jwtSsoRedirectSecret_response="$(eval $jwtSsoRedirectSecret_curl_statement)"
echo "jwtSsoRedirectSecret response = " $jwtSsoRedirectSecret_response
jwtSsoRedirectSecret_token="$(echo $jwtSsoRedirectSecret_response | awk -F "," '{print $2}' | awk -F ":" '{print $2}' | awk -F "\"" '{print $2}')"
echo "jwtSsoRedirectSecret_token="$jwtSsoRedirectSecret_token

mongodb_username_curl_statement="curl -X POST -k '$API_URL/api/secrets/add-local-secret?accountId=$accountId' -H 'Authorization: Bearer $TOKEN' -H 'Content-Type: application/json' -d '{ \"name\":\"mongodb_admin_user\",\"value\":\"$mongodbUserName\"}'"
echo "mongodb_username_curl_statement sent is " $mongodb_username_curl_statement
mongodb_user_response="$(eval $mongodb_username_curl_statement)"
echo "account_secret response = " $mongodb_user_response
mongodb_user_token="$(echo $mongodb_user_response | awk -F "," '{print $2}' | awk -F ":" '{print $2}' | awk -F "\"" '{print $2}')"
echo "mongodb_user_token="$mongodb_user_token

mongodb_password_curl_statement="curl -X POST -k '$API_URL/api/secrets/add-local-secret?accountId=$accountId' -H 'Authorization: Bearer $TOKEN' -H 'Content-Type: application/json' -d '{ \"name\":\"mongodb_admin_password\",\"value\":\"$mongodbPassword\"}'"
echo "mongodb_password_curl_statement sent is " $mongodb_password_curl_statement
mongodb_password_response="$(eval $mongodb_password_curl_statement)"
echo "mongodb_password_response = " $mongodb_password_response
mongodb_password_token="$(echo $mongodb_password_response | awk -F "," '{print $2}' | awk -F ":" '{print $2}' | awk -F "\"" '{print $2}')"
echo "mongodb_password_token="$mongodb_password_token

docker_password_curl_statement="curl -X POST -k '$API_URL/api/secrets/add-local-secret?accountId=$accountId' -H 'Authorization: Bearer $TOKEN' -H 'Content-Type: application/json' -d '{ \"name\":\"docker_login_password\",\"value\":\"$dockerPassword\"}'"
echo "docker_password_curl_statement sent is " $docker_password_curl_statement
docker_password_response="$(eval $docker_password_curl_statement)"
echo "docker_password_response = " $docker_password_response
docker_password_token="$(echo $docker_password_response | awk -F "," '{print $2}' | awk -F ":" '{print $2}' | awk -F "\"" '{print $2}')"
echo "docker_password_token="$docker_password_token

newrelic_license_curl_statement="curl -X POST -k '$API_URL/api/secrets/add-local-secret?accountId=$accountId' -H 'Authorization: Bearer $TOKEN' -H 'Content-Type: application/json' -d '{ \"name\":\"NEWRELIC_LICENSE_KEY\",\"value\":\"$newreliclicensekey\"}'"
echo "newrelic_license_curl_statement sent is " $newrelic_license_curl_statement
newrelic_license_response="$(eval $newrelic_license_curl_statement)"
echo "newrelic_license_response = " $newrelic_license_response
newrelic_license_token="$(echo $newrelic_license_response | awk -F "," '{print $2}' | awk -F ":" '{print $2}' | awk -F "\"" '{print $2}')"
echo "newrelic_license_token="$newrelic_license_token

sanitizedhost1="$(echo $host1 | sed -e 's|\.|\\.|g')"
sanitizedhost2="$(echo $host2 | sed -e 's|\.|\\.|g')"
sanitizedhost3="$(echo $host3 | sed -e 's|\.|\\.|g')"

####Generate secrets section END ######################################

#ssh_key_curl_statement="curl -X POST -k \
#  '$API_URL/api/settings?accountId=$accountId' \
#  -H 'Authorization: Bearer $TOKEN' \
#  -H 'Cache-Control: no-cache' \
#  -H 'Content-Type: application/json' \
#  -d '{\"value\":{\"userName\":\"ubuntu\", \
#  \"key\":\"`sed -E ':a;N;$!ba;s/\r{0,1}\n/\\n/g' amazonkey.pem`\",\"connectionType\":\"SSH\",\"accessType\":\"KEY\",\"type\":\"HOST_CONNECTION_ATTRIBUTES\"},\"name\":\"amazonkey-nonprod\",\"category\":\"SETTING\",\"accountId\":\"$accountId\"}'"
#echo "Sending ssh key curl statement = " $ssh_key_curl_statement
#ssh_key_response="$(eval $ssh_key_curl_statement)"
#echo "SSH Key Response = " $ssh_key_response

ssh_key_curl_statement="curl -X POST -k \
  '$API_URL/api/settings?accountId=$accountId' \
  -H 'Authorization: Bearer $TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{\"value\":{\"type\":\"HOST_CONNECTION_ATTRIBUTES\",\"userName\":\"$sshUser\",\"keyless\":true,\"keyPath\":\"$sshKeyPath\",\"connectionType\":\"SSH\",\"accessType\":\"KEY\"},\"name\":\"onprem-key\",\"category\":\"SETTING\"}'"

echo "Sending ssh key curl statement = " $ssh_key_curl_statement
ssh_key_response="$(eval $ssh_key_curl_statement)"
echo "SSH Key Response = " $ssh_key_response

## Copy master directory into working directory :

rm -rf Setup
cp -Rf Setup_Master_Copy Setup

$(replace "<MONGODB_USERNAME_PLACEHOLDER>" "safeharness:$mongodb_user_token")
$(replace "<MONGODB_PASSWORD_PLACEHOLDER>" "safeharness:$mongodb_password_token")
$(replace "<LEARNING_ENGINE_SECRET_KEY_PLACEHOLDER>" "safeharness:$learning_engine_secret_token")
$(replace "<ACCOUNT_SECRET_KEY_PLACEHOLDER>" "safeharness:$account_secret_token")
$(replace "<DOCKER_LOGIN_PASSWORD_PLACEHOLDER>" "safeharness:$docker_password_token")
$(replace "<NEWRELIC_LICENSE_KEY_PLACEHOLDER>" "safeharness:$newrelic_license_token")
$(replace "<JWT_SSO_REDIRECT_SECRET_PLACEHOLDER>" "safeharness:$jwtSsoRedirectSecret_token")
$(replace "<JWT_MULTI_AUTH_SECRET_PLACEHOLDER>" "safeharness:$jwtMultiAuthSecret_token")
$(replace "<JWT_ZENDESK_SECRET_PLACEHOLDER>" "safeharness:$jwtZendeskSecret_token")
$(replace "<JWT_EXTERNAL_SERVICE_SECRET_PLACEHOLDER>" "safeharness:$jwtExternalServiceSecret_token")
$(replace "<JWT_PASSWORD_SECRET_PLACEHOLDER>" "safeharness:$jwtPasswordSecret_token")
$(replace "<HOST1_PLACEHOLDER>" "$sanitizedhost1")
$(replace "<HOST2_PLACEHOLDER>" "$sanitizedhost2")
$(replace "<HOST3_PLACEHOLDER>" "$sanitizedhost3")
$(replace "<LOAD_BALANCER_URL_PLACEHOLDER>" "$loadbalancer")
$(replace "<COMPANYNAME_PLACEHOLDER>" "$companyName")
$(replace "<ACCOUNTNAME_PLACEHOLDER>" "$accountName")
$(replace "<EMAIL_PLACEHOLDER>" "$adminEmail")

zip -r Setup.zip Setup

echo "Created Setup.zip with the correct parameters"

yamlCurlStatement="curl -X POST -k \
  '$API_URL/api/setup-as-code/yaml/yaml-as-zip?accountId=$accountId' \
  -H 'Authorization: Bearer $TOKEN' \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: multipart/form-data' \
  -H 'content-type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW' \
  -F file=@`pwd`/Setup.zip"

echo "yaml curl statement sent is " $yamlCurlStatement
yamlCurlResponse="$(eval $yamlCurlStatement)"

echo "Yaml Response received is " $yamlCurlResponse

if [[ $response = *"ERROR"* ]]; then
  echo "YAML Request Failed"
else
  echo "YAML Request succeeded"
  rm -rf Setup.zip
  rm -rf Setup
fi
