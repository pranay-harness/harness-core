#!/usr/bin/env bash

ACCOUNT_PROPERTY_FILE=accountdetails.properties
INFRA_PROPERTY_FILE=inframapping.properties
CONFIG_PROPERTY_FILE=config.properties
VERSION_PROPERTY_FILE=version.properties

source ./utils.sh

if [[ -z $1 ]]; then
   runtime_dir=$(getProperty "$CONFIG_PROPERTY_FILE" "runtime_dir")
   echo "No runtime directory supplied in argument, will default to using the value in config.properties, value=$runtime_dir"
else
  runtime_dir=$1
  echo "Using runtime directory $runtime_dir"
fi


echo "# Reading account details from $ACCOUNT_PROPERTY_FILE"
accountName=$(getProperty "$ACCOUNT_PROPERTY_FILE" "AccountName")
companyName=$(getProperty $ACCOUNT_PROPERTY_FILE "CompanyName")
adminEmail=$(getProperty $ACCOUNT_PROPERTY_FILE "AdminEmail")


echo "Reading Infra mapping from $INFRA_PROPERTY_FILE"
host1=$(getProperty "$INFRA_PROPERTY_FILE" "HOST1_IP_ADDRESS")

echo "Reading config mapping from $CONFIG_PROPERTY_FILE"
mongodbUserName=$(getProperty "$CONFIG_PROPERTY_FILE" "mongodbUserName" | base64 --decode)
mongodbPassword=$(getProperty "$CONFIG_PROPERTY_FILE" "mongodbPassword" | base64 --decode)
licenseInfo=$(getProperty "$CONFIG_PROPERTY_FILE" "licenseInfo")

echo "#######Account details start #############"
echo "AccountName="$accountName
echo "CompanyName="$companyName
echo "AdminEmail="$adminEmail
echo "#######Account details end #############"

printf "\n"

echo "#######Infrastructure details start #############"
echo "host1="$host1
echo "#######Infrastructure details end #############"
printf "\n"

sudo rm -rf config
mkdir -p $runtime_dir
cp -Rf config_template config

newinstallation=false

if [[ $(checkIfFileExists "$runtime_dir/savedState") -eq 1 ]]; then
    echo "No state found, creating a new savedState"
    newinstallation=true
    learningengine_secret=$(generateRandomStringOfLength 32)
    account_secret=$(generateRandomString)
    jwtPasswordSecret=$(generateRandomStringOfLength 80)
    jwtExternalServiceSecret=$(generateRandomStringOfLength 80)
    jwtZendeskSecret=$(generateRandomStringOfLength 80)
    jwtMultiAuthSecret=$(generateRandomStringOfLength 80)
    jwtSsoRedirectSecret=$(generateRandomStringOfLength 80)
    accountKey=$(generateRandomStringOfLength 22)
    echo "learningengine_secret"=$learningengine_secret > "$runtime_dir/savedState"
    echo "account_secret"=$account_secret >> "$runtime_dir/savedState"
    echo "jwtPasswordSecret"=$jwtPasswordSecret >> "$runtime_dir/savedState"
    echo "jwtExternalServiceSecret="$jwtExternalServiceSecret >> "$runtime_dir/savedState"
    echo "jwtZendeskSecret="$jwtZendeskSecret >> "$runtime_dir/savedState"
    echo "jwtMultiAuthSecret="$jwtMultiAuthSecret >> "$runtime_dir/savedState"
    echo "jwtSsoRedirectSecret="$jwtSsoRedirectSecret >> "$runtime_dir/savedState"
    echo "accountKey="$accountKey >> "$runtime_dir/savedState"
    echo "host1="$host1 >> "$runtime_dir/savedState"
    echo "accountName="$accountName >> "$runtime_dir/savedState"
    echo "companyName="$companyName >> "$runtime_dir/savedState"
    echo "adminEmail="$adminEmail >> "$runtime_dir/savedState"
else
    echo "Reading configuration from the savedState in ${runtime_dir}"
fi


learningengine_secret=$(getProperty "$runtime_dir/savedState" "learningengine_secret")
account_secret=$(getProperty "$runtime_dir/savedState" "account_secret")
jwtPasswordSecret=$(getProperty "$runtime_dir/savedState" "jwtPasswordSecret")
jwtExternalServiceSecret=$(getProperty "$runtime_dir/savedState" "jwtExternalServiceSecret")
jwtZendeskSecret=$(getProperty "$runtime_dir/savedState" "jwtZendeskSecret")
jwtMultiAuthSecret=$(getProperty "$runtime_dir/savedState" "jwtMultiAuthSecret")
jwtSsoRedirectSecret=$(getProperty "$runtime_dir/savedState" "jwtSsoRedirectSecret")
accountKey=$(getProperty "$runtime_dir/savedState" "accountKey")
host1=$(getProperty "$runtime_dir/savedState" "host1")
accountKey=$(getProperty "$runtime_dir/savedState" "accountKey")
sshUser=$(getProperty "$runtime_dir/savedState" "sshUser")
sshKeyPath=$(getProperty "$runtime_dir/savedState" "sshKeyPath")
accountName=$(getProperty "$runtime_dir/savedState" "accountName")
companyName=$(getProperty "$runtime_dir/savedState" "companyName")
adminEmail=$(getProperty "$runtime_dir/savedState" "adminEmail")

sanitizedhost1="$(echo $host1 | sed -e 's|\.|\\.|g')"

echo "#######MongoDB details start #############"

##MONGO related properties###
harness_db=$(getProperty "config_template/mongo/mongoconfig.properties" "harness_application_db")
mongodb_port=$(getProperty "config_template/mongo/mongoconfig.properties" "mongodb_port")
mongodb_data_dir=$(getProperty "config_template/mongo/mongoconfig.properties" "mongodb_data_dir")
mongodb_sys_log_file=$(getProperty "config_template/mongo/mongoconfig.properties" "mongodb_sys_log_file")
mongodb_sys_log_dir=$(getProperty "config_template/mongo/mongoconfig.properties" "mongodb_sys_log_dir")
replicaset_name=$(getProperty "config_template/mongo/mongoconfig.properties" "replicaset_name")
admin_db=$(getProperty "config_template/mongo/mongoconfig.properties" "admin_db")
admin_user_role=$(getProperty "config_template/mongo/mongoconfig.properties" "admin_user_role")

echo "HarnessDB="$harness_db
echo "MongoPort="$mongodb_port
echo "#######MongoDB details end #############"
printf "\n"


echo "Created config folder from config_template"

$(replace "COMPANYNAME" "$companyName")
$(replace "ACCOUNTNAME" "$accountName")
$(replace "ACCOUNT_SECRET_KEY" "$account_secret")
$(replace "LEARNING_ENGINE_SECRET" "$learningengine_secret")
$(replace "kmpySmUISimoRrJL6NL73w" $accountKey)
$(replace "EMAIL" $adminEmail)
$(replace "HARNESSDB" $harness_db)
$(replace "MONGODB_PORT" $mongodb_port)

proxyPort=$(getProperty "config_template/proxy/proxyconfig.properties" "proxy_port")
managerport=$(getProperty "config_template/manager/manager.properties" "manager_port")
uiport=$(getProperty "config_template/ui/ui.properties" "ui_port")
verificationport=$(getProperty "config_template/verification/verification_service.properties" "verification_port")
MANAGER1=$host1:$managerport
VERIFICATION1=$host1:$verificationport
UI1=$host1:$uiport
WWW_DIR_LOCATION=$runtime_dir/data/proxy/www
STORAGE_DIR_LOCATION=$WWW_DIR_LOCATION/data/storage
PROXY_VERSION=$(getProperty "version.properties" "PROXY_VERSION")
LOAD_BALANCER_URL=http://$host1:$proxyPort
MONGO_URI=mongodb://$mongodbUserName:$mongodbPassword@$host1:$mongodb_port/harness?authSource=admin
mkdir -p $STORAGE_DIR_LOCATION



function setUpMongoDB(){

    echo "################################Starting up MongoDB################################"

    printf "\n\n\n\n"

    echo "##### Setting up directories for mongodb######## "
    chmod 666 config/mongo/mongod.conf
    chmod 666 config/mongo/add_first_user.js
    chmod 666 config/mongo/add_learning_engine_secret.js

    mkdir -p $runtime_dir/mongo/$mongodb_sys_log_dir
    mkdir -p $runtime_dir/mongo/$mongodb_data_dir
    touch $runtime_dir/mongo/$mongodb_sys_log_dir/$mongodb_sys_log_file
    echo "Creating file : "$runtime_dir/mongo/$mongodb_sys_log_dir/$mongodb_sys_log_file
    mv config/mongo/mongod.conf $runtime_dir/mongo
    mkdir -p $runtime_dir/mongo/scripts
    mv config/mongo/add_first_user.js $runtime_dir/mongo/scripts
    mv config/mongo/add_learning_engine_secret.js $runtime_dir/mongo/scripts

    chown -R 999 $runtime_dir/mongo/*
    chmod 777 $runtime_dir/mongo/$mongodb_data_dir

    docker run -p $mongodb_port:$mongodb_port --name mongoContainer -d -v "$runtime_dir/mongo/mongod.conf":/etc/mongod.conf -v $runtime_dir/mongo/data/db:/data/db -v $runtime_dir/mongo/scripts:/scripts --rm mongo:3.4 -f /etc/mongod.conf

    mongoContainerId=$(docker ps -q -f name=mongoContainer)

    if [[ "${mongoContainerId}" == "" ]]; then
        echo "MongoContainer did not start"
        exit 1
    else
        echo "MongoContainer started with ID="$mongoContainerId
    fi

    echo "Sleeping for 5 seconds before proceeding"
    sleep 5

}

function seedMongoDB(){
    echo "################################Seeding MongoDB with data ################################"

    docker exec mongoContainer mongo --port $mongodb_port admin --eval "db.createUser({user: '$mongodbUserName', pwd: '$mongodbPassword', roles:[{role:'$admin_user_role',db:'admin'}]});"

    docker exec mongoContainer bash -c "mongo  mongodb://$mongodbUserName:$mongodbPassword@$host1:$mongodb_port < /scripts/add_first_user.js"

    docker exec mongoContainer bash -c "mongo  mongodb://$mongodbUserName:$mongodbPassword@$host1:$mongodb_port < /scripts/add_learning_engine_secret.js"

}


function setUpProxy(){
    echo "################################Setting up proxy ################################"

    docker run -d --name harness-proxy --rm -p $proxyPort:7143 -e MANAGER1=$MANAGER1 -e VERIFICATION1=$VERIFICATION1 -e UI1=$UI1 -v  $WWW_DIR_LOCATION:/www  harness/proxy:$PROXY_VERSION
    sleep 5

    if [[ $(checkDockerImageRunning "harness-proxy") -eq 1 ]]; then
      echo "Harness Proxy is not running"
    fi
}

function setupManager(){
    echo "################################Setting up Manager ################################"

    ALLOWED_ORIGINS=$LOAD_BALANCER_URL
    DELEGATE_METADATA_URL=$LOAD_BALANCER_URL/storage/wingsdelegates/delegateprod.txt
    SERVER_PORT=$managerport
    TCP_HOSTS_DETAILS=$host1:$(getProperty "config_template/manager/manager.properties" "HAZELCAST_PORT")
    FEATURES=$(getProperty "config_template/manager/manager.properties" "FEATURES")
    jre_version=$(getProperty "config_template/manager/manager.properties" "jre_version")
    UI_SERVER_URL=$LOAD_BALANCER_URL
    CAPSULE_JAR=$(getProperty "config_template/manager/manager.properties" "CAPSULE_JAR")
    MEMORY=$(getProperty "config_template/manager/manager.properties" "MEMORY")
    LOGGING_LEVEL=$(getProperty "config_template/manager/manager.properties" "LOGGING_LEVEL")
    DEPLOY_MODE=ONPREM
    WATCHER_METADATA_URL=$LOAD_BALANCER_URL/storage/wingswatchers/watcherprod.txt
    HAZELCAST_PORT=$(getProperty "config_template/manager/manager.properties" "HAZELCAST_PORT")
    managerVersion=$(getProperty "version.properties" "MANAGER_VERSION")

#    echo $LOAD_BALANCER_URL
#    echo $ALLOWED_ORIGINS
#    echo $DELEGATE_METADATA_URL
#    echo $MONGO_URI
#    echo $SERVER_PORT
#    echo $TCP_HOSTS_DETAILS
#    echo $FEATURES
#    echo $jre_version
#    echo $UI_SERVER_URL
#    echo $NEWRELIC_ENV
#    echo $CAPSULE_JAR
#    echo $NEWRELIC_LICENSE_KEY
#    echo $MEMORY
#    echo $LOGGING_LEVEL
#    echo $DEPLOY_MODE

  sudo mkdir -p $runtime_dir/manager/logs
  sudo chmod -R 777 $runtime_dir/manager

 docker run -d --net=host --rm -p $SERVER_PORT:$SERVER_PORT --name harnessManager -e LOGGING_LEVEL=$LOGGING_LEVEL -e MEMORY=$MEMORY -e WATCHER_METADATA_URL=$WATCHER_METADATA_URL -e LICENSE_INFO=$licenseInfo -e ALLOWED_ORIGINS=$ALLOWED_ORIGINS -e CAPSULE_JAR=$CAPSULE_JAR -e DELEGATE_METADATA_URL=$DELEGATE_METADATA_URL -e HZ_CLUSTER_NAME=docker-manager-onprem -e SERVER_PORT=$SERVER_PORT -e UI_SERVER_URL=$UI_SERVER_URL -e MONGO_URI="$MONGO_URI" -e DEPLOY_MODE=$DEPLOY_MODE -e TCP_HOSTS_DETAILS=$TCP_HOSTS_DETAILS -e CIDR=127.0.0.1 -e API_URL=$LOAD_BALANCER_URL -e HAZELCAST_PORT=$HAZELCAST_PORT -e jwtPasswordSecret=$jwtPasswordSecret -e jwtExternalServiceSecret=$jwtExternalServiceSecret -e jwtZendeskSecret=$jwtZendeskSecret -e jwtMultiAuthSecret=$jwtMultiAuthSecret -e jwtSsoRedirectSecret=$jwtSsoRedirectSecret -e FEATURES=$FEATURES -e SKIP_LOGS=true -v $runtime_dir/manager/logs:/opt/harness/logs  harness/manager:$managerVersion

 sleep 10

 if [[ $(checkDockerImageRunning "harnessManager") -eq 1 ]]; then
      echo "Harness Manager is not running"
 fi

}

function setUpVerificationService(){
   echo "################################Setting up Verification Service ################################"
   verificationServiceVersion=$(getProperty "version.properties" "VERIFICATION_SERVICE_VERSION")
   env=$(getProperty "version.properties" "ENV")
   docker run -d --rm --name verificationService -e MANAGER_URL=$LOAD_BALANCER_URL/api/ -e MONGO_URI="$MONGO_URI" -e ENV=$env -e VERIFICATION_PORT=$verificationport -v $runtime_dir/verification/logs:/opt/harness/logs harness/verification-service:$verificationServiceVersion

    if [[ $(checkDockerImageRunning "verificationService") -eq 1 ]]; then
        echo "Verification service is not running"
    fi

}

function setupUI(){
   echo "################################Setting up UI ################################"
   ui_version=$(getProperty "version.properties" "UI_VERSION")
   UI_PORT=$(getProperty "config_template/ui/ui.properties" "ui_port")
   docker run -d --name harness_ui -p $UI_PORT:80 --rm -e API_URL="$LOAD_BALANCER_URL" harness/ui:$ui_version

   if [[ $(checkDockerImageRunning "harness_ui") -eq 1 ]]; then
      echo "Harness UI is not running"
   fi

}

function setUpLearningEngine(){
   echo "################################Setting up Learning Engine ################################"
   learningEngineVersion=$(getProperty "version.properties" "LEARNING_ENGINE_VERSION")
   https_port=$(getProperty "config_template/learning_engine/learning_engine.properties" "https_port")
   docker run -d --rm --name learningEngine -e learning_env=on_prem -e https_port=$https_port -e server_url=$LOAD_BALANCER_URL -e service_secret=$learningengine_secret harness/learning-engine:$learningEngineVersion

    if [[ $(checkDockerImageRunning "learningEngine") -eq 1 ]]; then
        echo "LearningEngine is not running"
    fi


}

function setupDelegateJars(){
   echo "################################Setting up Delegate Jars ################################"

    DELEGATE_VERSION=$(getProperty "version.properties" "DELEGATE_VERSION")
    WATCHER_VERSION=$(getProperty "version.properties" "WATCHER_VERSION")

    mkdir -p $STORAGE_DIR_LOCATION/wingsdelegates/jre/${jre_version}/
    cp images/*.gz $STORAGE_DIR_LOCATION/wingsdelegates/jre/${jre_version}/

    rm -rf ${STORAGE_DIR_LOCATION}/wingsdelegates/jobs/deploy-prod-delegate/*
    mkdir -p  ${STORAGE_DIR_LOCATION}/wingsdelegates/jobs/deploy-prod-delegate/${DELEGATE_VERSION}
    cp images/delegate.jar ${STORAGE_DIR_LOCATION}/wingsdelegates/jobs/deploy-prod-delegate/${DELEGATE_VERSION}/

    echo "1.0.${DELEGATE_VERSION} jobs/deploy-prod-delegate/${DELEGATE_VERSION}/delegate.jar" > delegateprod.txt

    mv delegateprod.txt ${STORAGE_DIR_LOCATION}/wingsdelegates

    rm -rf ${STORAGE_DIR_LOCATION}/wingswatchers/jobs/deploy-prod-watcher/*
    mkdir -p  ${STORAGE_DIR_LOCATION}/wingswatchers/jobs/deploy-prod-watcher/${WATCHER_VERSION}
    cp images/watcher.jar ${STORAGE_DIR_LOCATION}/wingswatchers/jobs/deploy-prod-watcher/${WATCHER_VERSION}/
    echo "1.0.${WATCHER_VERSION} jobs/deploy-prod-watcher/${WATCHER_VERSION}/watcher.jar" > watcherprod.txt
    sudo mv watcherprod.txt ${STORAGE_DIR_LOCATION}/wingswatchers

}


function loadDockerImages(){
    docker load --input images/proxy.tar
    docker load --input images/manager.tar
    docker load --input images/verification_service.tar
    docker load --input images/ui.tar
    docker load --input images/mongo.tar
    docker load --input images/learning_engine.tar
}

function startUp(){
    loadDockerImages
    setUpMongoDB
    if [[ ${newinstallation} == "true" ]];then
        seedMongoDB
    else
        echo "Not seeding Mongo,existing installation found "
    fi

    setUpProxy
    setupManager
    setUpVerificationService
    setupUI
    setUpLearningEngine
    setupDelegateJars
}

function cleanupAfterStart(){
   echo "################################Cleaning up after start ################################"
    sudo rm -rf config
}

stopContainers
startUp
cleanupAfterStart
echo "Server is running at http://${host1}:${proxyPort}"