#!/usr/bin/env bash

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

if [[ "$OSTYPE" == "darwin"* ]]; then
        output=$(sed --help | grep GNU)
        if [[ $? -eq 1 ]]; then
            echo "GNU Sed is required for running this script, the sed on Mac OSX may not work"
            printf " Use these commands \n $ brew uninstall gnu-sed \n $ brew install gnu-sed --with-default-names \n \n"
            exit 1
        fi
fi

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
   LENGTH=$1
   echo `openssl rand -base64 $LENGTH`
}

echo "# Reading account details from $ACCOUNT_PROPERTY_FILE"
accountName=$(getProperty "$ACCOUNT_PROPERTY_FILE" "AccountName")
companyName=$(getProperty $ACCOUNT_PROPERTY_FILE "CompanyName")
adminName=$(getProperty $ACCOUNT_PROPERTY_FILE "AdminName")
adminEmail=$(getProperty $ACCOUNT_PROPERTY_FILE "AdminEmail")


echo "Reading Infra mapping from $INFRA_PROPERTY_FILE"
host1=$(getProperty "$INFRA_PROPERTY_FILE" "HOST1")
host2=$(getProperty "$INFRA_PROPERTY_FILE" "HOST2")
host3=$(getProperty "$INFRA_PROPERTY_FILE" "HOST3")
loadbalancer=$(getProperty "$INFRA_PROPERTY_FILE" "LOAD_BALANCER_URL")

echo "Reading config mapping from $CONFIG_PROPERTY_FILE"
mongodbUserName=$(getProperty "$CONFIG_PROPERTY_FILE" "mongodbUserName" | base64 --decode)
mongodbPassword=$(getProperty "$CONFIG_PROPERTY_FILE" "mongodbPassword" | base64 --decode)
dockerPassword=$(getProperty "$CONFIG_PROPERTY_FILE" "dockerPassword"  | base64 --decode)
newreliclicensekey=$(getProperty "$CONFIG_PROPERTY_FILE" "newreliclicensekey" | base64 --decode)

echo "#######Account details#############"
echo "AccountName="$accountName
echo "CompanyName="$companyName
echo "AdminName="$adminName
echo "AdminEmail="$adminEmail

printf "\n"

echo "#######Infrastructure details #############"
echo "host1="$host1
echo "host2="$host2
echo "host3="$host3
echo "loadbalancer="$loadbalancer

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

learningengine_secret=$(generateRandomString "32")
learningengine_curl_statement="curl -X POST -k '$API_URL/api/secrets/add-local-secret?accountId=$accountId' -H 'Authorization: Bearer $TOKEN' -H 'Content-Type: application/json' -d '{ \"name\":\"LEARNING_ENGINE_SECRET\",\"value\":\"$learningengine_secret\"}'"
echo "learningengine_curl_statement sent is " $learningengine_curl_statement
learning_engine_response="$(eval $learningengine_curl_statement)"
echo "LearningEngine response = " $learning_engine_response
learning_engine_secret_token="$(echo $learning_engine_response | awk -F "," '{print $2}' | awk -F ":" '{print $2}' | awk -F "\"" '{print $2}')"
echo "learning_engine_secret_token=" $learning_engine_secret_token

account_secret=$(generateRandomString "32")
account_secret_curl_statement="curl -X POST -k '$API_URL/api/secrets/add-local-secret?accountId=$accountId' -H 'Authorization: Bearer $TOKEN' -H 'Content-Type: application/json' -d '{ \"name\":\"ACCOUNT_SECRET_KEY\",\"value\":\"$account_secret\"}'"
echo "account_secret_curl_statement sent is " $account_secret_curl_statement
account_secret_response="$(eval $account_secret_curl_statement)"
echo "account_secret response = " $account_secret_response
account_secret_token="$(echo $account_secret_response | awk -F "," '{print $2}' | awk -F ":" '{print $2}' | awk -F "\"" '{print $2}')"
echo "account_secret_token="$account_secret_token

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

## Copy master directory into working directory :

rm -rf Setup
cp -Rf Setup_Master_Copy Setup

find Setup -type f -exec sed -i "s|<MONGODB_USERNAME_PLACEHOLDER>|safeharness:$mongodb_user_token|g" {} +
find Setup -type f -exec sed -i "s|<MONGODB_PASSWORD_PLACEHOLDER>|safeharness:$mongodb_password_token|g" {} +
find Setup -type f -exec sed -i "s|<LEARNING_ENGINE_SECRET_KEY_PLACEHOLDER>|safeharness:$learning_engine_secret_token|g" {} +
find Setup -type f -exec sed -i "s|<ACCOUNT_SECRET_KEY_PLACEHOLDER>|safeharness:$account_secret_token|g" {} +
find Setup -type f -exec sed -i "s|<DOCKER_LOGIN_PASSWORD_PLACEHOLDER>|safeharness:$docker_password_token|g" {} +
find Setup -type f -exec sed -i "s|<NEWRELIC_LICENSE_KEY_PLACEHOLDER>|safeharness:$newrelic_license_token|g" {} +
find Setup -type f -exec sed -i "s|<HOST1_PLACEHOLDER>|$sanitizedhost1|g" {} +
find Setup -type f -exec sed -i "s|<HOST2_PLACEHOLDER>|$sanitizedhost2|g" {} +
find Setup -type f -exec sed -i "s|<HOST3_PLACEHOLDER>|$sanitizedhost3|g" {} +
find Setup -type f -exec sed -i "s|<LOAD_BALANCER_URL_PLACEHOLDER>|$loadbalancer|g" {} +

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
#  rm -rf Setup.zip
  rm -rf Setup
fi