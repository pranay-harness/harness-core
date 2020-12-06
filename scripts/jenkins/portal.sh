### Dockerization of Manager ####### Doc

mkdir -p dist ;
cd dist
cp -R ../scripts/jenkins/ .
cd ..

mkdir -p dist/manager ;

cd dist/manager

cp ../../400-rest/target/rest-capsule.jar .
cp ../../400-rest/src/main/resources/hazelcast.xml .
cp ../../keystore.jks .
cp ../../400-rest/key.pem .
cp ../../400-rest/cert.pem .
cp ../../400-rest/newrelic.yml .
cp ../../400-rest/config.yml .
cp ../../400-rest/src/main/resources/redisson-jcache.yaml .

cp ../../dockerization/manager/Dockerfile-manager-jenkins-k8 ./Dockerfile
cp ../../dockerization/manager/Dockerfile-manager-jenkins-k8-gcr ./Dockerfile-gcr
cp -r ../../dockerization/manager/scripts/ .
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

cp ../../dockerization/verification/Dockerfile-verification-jenkins-k8 ./Dockerfile
cp ../../dockerization/verification/Dockerfile-verification-jenkins-k8-gcr ./Dockerfile-gcr
cp -R ../../dockerization/verification/scripts/ .
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
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi

cd ../..

mkdir -p dist/event-server ;
cd dist/event-server
cp ../../350-event-server/target/event-server-capsule.jar .
cp ../../350-event-server/key.pem .
cp ../../350-event-server/cert.pem .
cp ../../350-event-server/event-service-config.yml .
cp ../../dockerization/event-server/Dockerfile-event-server-jenkins-k8 Dockerfile
cp ../../dockerization/event-server/Dockerfile-event-server-jenkins-k8-gcr Dockerfile-gcr
cp -r ../../dockerization/event-server/scripts/ .
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
cp ../../dockerization/batch-processing/Dockerfile-batch-processing-jenkins-k8 Dockerfile
cp ../../dockerization/batch-processing/Dockerfile-batch-processing-jenkins-k8-gcr Dockerfile-gcr
cp -r ../../dockerization/batch-processing/scripts/ .
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi
cd ../..

echo "System-Properties: version=1.0.${VERSION}" >> app.mf
echo "Application-Version: version=1.0.${VERSION}" >> app.mf

mkdir -p dist/delegate
cp 260-delegate/target/delegate-capsule.jar dist/delegate/delegate-capsule.jar
cp 260-delegate/config-delegate.yml dist/delegate/config-delegate.yml
jar ufm dist/delegate/delegate-capsule.jar app.mf
cp dist/delegate/delegate-capsule.jar delegate-${VERSION}.jar

mkdir -p dist/watcher
cp 250-watcher/target/watcher-capsule.jar dist/watcher/watcher-capsule.jar
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
cp ../../160-model-gen-tool/target/model-gen-tool-capsule.jar .
cp ../../160-model-gen-tool/config-datagen.yml .
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
