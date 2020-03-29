### Dockerization of Manager ####### Doc

mkdir -p dist ;
cd dist
if [ ! -z ${JDK} ]
then
    echo ${JDK} > jdk.txt
fi
cp -R ../scripts/jenkins/ .
cd ..

mkdir -p dist/migrator ;

cd dist/migrator ;

cp ../../51-migrator/target/migrator-capsule.jar .
cp ../../keystore.jks .

cp ../../dockerization/migrator/Dockerfile-migrator-jenkins-k8-gcr-openjdk ./Dockerfile-gcr

echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi

cd ../..

mkdir -p dist/manager ;

cd dist/manager

cp ../../71-rest/target/rest-capsule.jar .
cp ../../71-rest/src/main/resources/hazelcast.xml .
cp ../../keystore.jks .
cp ../../71-rest/key.pem .
cp ../../71-rest/cert.pem .
cp ../../71-rest/newrelic.yml .
cp ../../71-rest/config.yml .
cp ../../tools/apm/appdynamics/AppServerAgent-4.5.0.23604.tar.gz .
cp ../../tools/monitoring/datadog/dd-java-agent.jar .

cp ../../dockerization/manager/Dockerfile-manager-jenkins-k8-openjdk ./Dockerfile
cp ../../dockerization/manager/Dockerfile-manager-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -r ../../dockerization/manager/scripts/ .
cp -r ../../dockerization/common-resources/ .
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi

cd ../..

mkdir -p dist/verification-service ;

cd dist/verification-service

cp ../../79-verification/target/verification-capsule.jar .
cp ../../79-verification/keystore.jks .
cp ../../79-verification/verification-config.yml .
cp ../../tools/apm/appdynamics/AppServerAgent-4.5.0.23604.tar.gz .

cp ../../dockerization/verification/Dockerfile-verification-jenkins-k8-openjdk ./Dockerfile
cp ../../dockerization/verification/Dockerfile-verification-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -R ../../dockerization/verification/scripts/ .
cp -r ../../dockerization/common-resources/ .
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi

cd ../..

mkdir -p dist/event-server ;
cd dist/event-server
cp ../../72-event-server/target/event-server-capsule.jar .
cp ../../72-event-server/key.pem .
cp ../../72-event-server/cert.pem .
cp ../../72-event-server/event-service-config.yml .
cp ../../dockerization/event-server/Dockerfile-event-server-jenkins-k8-openjdk Dockerfile
cp ../../dockerization/event-server/Dockerfile-event-server-jenkins-k8-gcr-openjdk Dockerfile-gcr
cp -r ../../dockerization/event-server/scripts/ .
cp -r ../../dockerization/common-resources/ .
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi
cd ../..

mkdir -p dist/batch-processing ;
cd dist/batch-processing
cp ../../78-batch-processing/target/batch-processing-capsule.jar .
cp ../../78-batch-processing/batch-processing-config.yml .
cp ../../dockerization/batch-processing/Dockerfile-batch-processing-jenkins-k8-openjdk Dockerfile
cp ../../dockerization/batch-processing/Dockerfile-batch-processing-jenkins-k8-gcr-openjdk Dockerfile-gcr
cp -r ../../dockerization/batch-processing/scripts/ .
cp -r ../../dockerization/common-resources/ .
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi
cd ../..

echo "System-Properties: version=1.0.${VERSION} logdnakey=${LOGDNA_KEY}" >> app.mf
echo "Application-Version: version=1.0.${VERSION}" >> app.mf

mkdir -p dist/delegate
cp 81-delegate/target/delegate-capsule.jar dist/delegate/delegate-capsule.jar
cp 81-delegate/config-delegate.yml dist/delegate/config-delegate.yml
jar ufm dist/delegate/delegate-capsule.jar app.mf
cp dist/delegate/delegate-capsule.jar delegate-${VERSION}.jar

mkdir -p dist/watcher
cp 82-watcher/target/watcher-capsule.jar dist/watcher/watcher-capsule.jar
jar ufm dist/watcher/watcher-capsule.jar app.mf
cp dist/watcher/watcher-capsule.jar watcher-${VERSION}.jar

rm -rf app.mf

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
cp ../../91-model-gen-tool/target/model-gen-tool-capsule.jar .
cp ../../91-model-gen-tool/config-datagen.yml .
cd ../..

mkdir -p dist/delegate-proxy
cd dist/delegate-proxy
cp ../../dockerization/delegate-proxy/setup.sh .
cp ../../dockerization/delegate-proxy/Dockerfile .
cp ../../dockerization/delegate-proxy/Dockerfile-gcr .
cp ../../dockerization/delegate-proxy/nginx.conf .
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi
cd ../..