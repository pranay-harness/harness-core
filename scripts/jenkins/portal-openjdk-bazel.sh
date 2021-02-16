### Dockerization of Manager ####### Doc
set -x

mkdir -p dist ;
cd dist

cp -R ../scripts/jenkins/ .
cd ..

mkdir -p dist/manager ;

cd dist/manager

cp ${HOME}/.bazel-dirs/bin/360-cg-manager/module_deploy.jar rest-capsule.jar
cp ../../400-rest/src/main/resources/hazelcast.xml .
cp ../../keystore.jks .
cp ../../360-cg-manager/key.pem .
cp ../../360-cg-manager/cert.pem .
cp ../../360-cg-manager/newrelic.yml .
cp ../../360-cg-manager/config.yml .
cp ../../400-rest/src/main/resources/redisson-jcache.yaml .

cp ../../dockerization/manager/Dockerfile-manager-jenkins-k8-openjdk ./Dockerfile
cp ../../dockerization/manager/Dockerfile-manager-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -r ../../dockerization/manager/scripts/ .
mv scripts/start_process_bazel.sh scripts/start_process.sh

echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi

cd ../..

mkdir -p dist/cv-nextgen ;

cd dist/cv-nextgen

cp ../../300-cv-nextgen/target/cv-nextgen-capsule.jar .
cp ../../300-cv-nextgen/keystore.jks .
cp ../../300-cv-nextgen/cv-nextgen-config.yml .

cp ../../dockerization/cv-nextgen/Dockerfile-verification-jenkins-k8-openjdk ./Dockerfile
cp ../../dockerization/cv-nextgen/Dockerfile-verification-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -R ../../dockerization/cv-nextgen/scripts/ .
echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi

cd ../..

mkdir -p dist/verification-service ;

cd dist/verification-service

cp ../../270-verification/target/verification-capsule.jar .
cp ../../270-verification/keystore.jks .
cp ../../270-verification/verification-config.yml .

cp ../../dockerization/verification/Dockerfile-verification-jenkins-k8-openjdk ./Dockerfile
cp ../../dockerization/verification/Dockerfile-verification-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -R ../../dockerization/verification/scripts/ .
echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi

cd ../..

mkdir -p dist/command-library-server ;

cd dist/command-library-server

cp ../../210-command-library-server/target/command-library-app-capsule.jar .
cp ../../210-command-library-server/keystore.jks .
cp ../../210-command-library-server/command-library-server-config.yml .

cp ../../dockerization/command-library-server/Dockerfile-command-library-server-jenkins-k8-openjdk ./Dockerfile
cp ../../dockerization/command-library-server/Dockerfile-command-library-server-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -R ../../dockerization/command-library-server/scripts/ .
echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi

cd ../..

mkdir -p dist/event-server ;
cd dist/event-server
cp ${HOME}/.bazel-dirs/bin/350-event-server/module_deploy.jar event-server-capsule.jar
cp ../../350-event-server/key.pem .
cp ../../350-event-server/cert.pem .
cp ../../350-event-server/event-service-config.yml .
cp ../../dockerization/event-server/Dockerfile-event-server-jenkins-k8-openjdk Dockerfile
cp ../../dockerization/event-server/Dockerfile-event-server-jenkins-k8-gcr-openjdk Dockerfile-gcr
cp -r ../../dockerization/event-server/scripts/ .
echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi
cd ../..

mkdir -p dist/batch-processing ;
cd dist/batch-processing
cp ../../280-batch-processing/target/batch-processing-capsule.jar .
cp ../../280-batch-processing/batch-processing-config.yml .
cp ../../dockerization/batch-processing/Dockerfile-batch-processing-jenkins-k8-openjdk Dockerfile
cp ../../dockerization/batch-processing/Dockerfile-batch-processing-jenkins-k8-gcr-openjdk Dockerfile-gcr
cp -r ../../dockerization/batch-processing/scripts/ .
echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi
cd ../..

mkdir -p dist/delegate
cp ${HOME}/.bazel-dirs/bin/260-delegate/module_deploy.jar dist/delegate/delegate-capsule.jar
#cp 260-delegate/target/delegate-capsule.jar dist/delegate/delegate-capsule.jar
cp 260-delegate/config-delegate.yml dist/delegate/config-delegate.yml
jarsigner -storetype pkcs12 -keystore ${KEY_STORE} -storepass ${KEY_STORE_PASSWORD} dist/delegate/delegate-capsule.jar ${KEY_STORE_ALIAS}
cp dist/delegate/delegate-capsule.jar delegate-${VERSION}.jar

mkdir -p dist/watcher
cp 250-watcher/target/watcher-capsule.jar dist/watcher/watcher-capsule.jar
jarsigner -storetype pkcs12 -keystore ${KEY_STORE} -storepass ${KEY_STORE_PASSWORD} dist/watcher/watcher-capsule.jar ${KEY_STORE_ALIAS}
cp dist/watcher/watcher-capsule.jar watcher-${VERSION}.jar

mkdir -p dist/disconnected_on_prem_pov
cd dist/disconnected_on_prem_pov
cp -r ../../on-prem/harness_disconnected_on_prem_pov_final .
cp -r ../../on-prem/disconnected_on_prem_pov_installer .
tar -zcvf disconnected_on_prem_pov_template.tar.gz *
cd ../..
cp dist/disconnected_on_prem_pov/disconnected_on_prem_pov_template.tar.gz disconnected_on_prem_pov_template.tar.gz

mkdir -p dist/disconnected_on_prem_k8s
cd dist/disconnected_on_prem_k8s
cp -r ../../on-prem/kubernetes_installer .
tar -zcvf disconnected_on_prem_k8s_installer_builder.tar.gz *
cd ../..
cp dist/disconnected_on_prem_k8s/disconnected_on_prem_k8s_installer_builder.tar.gz disconnected_on_prem_k8s_installer_builder.tar.gz

mkdir -p dist/test
cd dist/test
cp ../../160-model-gen-tool/target/model-gen-tool-capsule.jar .
cp ../../160-model-gen-tool/config-datagen.yml .
cd ../..

mkdir -p dist/delegate-proxy
cd dist/delegate-proxy
cp ../../dockerization/delegate-proxy/setup.sh .
cp ../../dockerization/delegate-proxy/Dockerfile .
cp ../../dockerization/delegate-proxy/Dockerfile-gcr .
cp ../../dockerization/delegate-proxy/nginx.conf .
echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi
cd ../..

mkdir -p dist/ng-manager
cd dist/ng-manager
cp ../../120-ng-manager/target/ng-manager-capsule.jar .
cp ../../120-ng-manager/config.yml .
cp ../../keystore.jks .
cp ../../120-ng-manager/key.pem .
cp ../../120-ng-manager/cert.pem .

cp ../../dockerization/ng-manager/Dockerfile-ng-manager-jenkins-k8-openjdk ./Dockerfile
cp ../../dockerization/ng-manager/Dockerfile-ng-manager-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -r ../../dockerization/ng-manager/scripts/ .
echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi

cd ../..


mkdir -p dist/ci-manager
cd dist/ci-manager
cp ../../310-ci-manager/target/ci-manager-capsule.jar .
cp ../../310-ci-manager/ci-manager-config.yml .
cp ../../keystore.jks .
cp ../../310-ci-manager/key.pem .
cp ../../310-ci-manager/cert.pem .

cp ../../dockerization/ci-manager/Dockerfile-ci-manager-jenkins-k8-openjdk ./Dockerfile
cp ../../dockerization/ci-manager/Dockerfile-ci-manager-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -r ../../dockerization/ci-manager/scripts/ .
echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi

cd ../..

mkdir -p dist/notification-service
cd dist/notification-service

cp ${HOME}/.bazel-dirs/bin/830-notification-service/module_deploy.jar notification-service-capsule.jar
cp ../../830-notification-service/config.yml .
cp ../../830-notification-service/keystore.jks .
cp ../../830-notification-service/key.pem .
cp ../../830-notification-service/cert.pem .
cp ../../dockerization/notification-service/Dockerfile-notification-service-jenkins-k8-openjdk ./Dockerfile
cp ../../dockerization/notification-service/Dockerfile-notification-service-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -r ../../dockerization/notification-service/scripts/ .

echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi

cd ../..

mkdir -p dist/pipeline-service
cd dist/pipeline-service

cp ${HOME}/.bazel-dirs/bin/800-pipeline-service/module_deploy.jar pipeline-service-capsule.jar
cp ../../800-pipeline-service/config.yml .
cp ../../800-pipeline-service/keystore.jks .
cp ../../800-pipeline-service/key.pem .
cp ../../800-pipeline-service/cert.pem .
cp ../../dockerization/pipeline-service/Dockerfile-pipeline-service-jenkins-k8-openjdk ./Dockerfile
cp ../../dockerization/pipeline-service/Dockerfile-pipeline-service-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -r ../../dockerization/pipeline-service/scripts/ .

echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi

cd ../..

mkdir -p dist/eventsapi-monitor
cd dist/eventsapi-monitor

cp -r ../../950-events-api/src/monitoring/* .

echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi

cd ../..

mkdir -p dist/accesscontrol-service
cd dist/accesscontrol-service

cp ${HOME}/.bazel-dirs/bin/905-access-control-core/module_deploy.jar accesscontrol-service-capsule.jar
cp ../../905-access-control-core/config.yml .
cp ../../905-access-control-core/keystore.jks .
cp ../../dockerization/accesscontrol-service/Dockerfile-accesscontrol-service-jenkins-k8-openjdk ./Dockerfile
cp ../../dockerization/accesscontrol-service/Dockerfile-accesscontrol-service-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -r ../../dockerization/accesscontrol-service/scripts/ .

echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi

cd ../..