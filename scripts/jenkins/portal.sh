### Dockerization of Manager ####### Doc

mkdir -p dist ;
cd dist
cp -R ../scripts/jenkins/ .
cd ..

mkdir -p dist/manager ;

cd dist/manager

cp ../../71-rest/target/rest-capsule.jar .
cp ../../71-rest/src/main/resources/hazelcast.xml .
cp ../../keystore.jks .
cp ../../71-rest/newrelic.yml .
cp ../../71-rest/config.yml .
cp ../../tools/apm/appdynamics/AppServerAgent-4.5.0.23604.tar.gz .

cp ../../dockerization/manager/Dockerfile-manager-jenkins-k8 ./Dockerfile
cp ../../dockerization/manager/Dockerfile-manager-jenkins-k8-gcr ./Dockerfile-gcr
cp -r ../../dockerization/manager/scripts/ .
cp -r ../../dockerization/common-resources/ .
echo ${VERSION} > version.txt
cd ../..

mkdir -p dist/verification-service ;

cd dist/verification-service

cp ../../79-verification/target/verification-capsule.jar .
cp ../../79-verification/keystore.jks .
cp ../../79-verification/verification-config.yml .
cp ../../tools/apm/appdynamics/AppServerAgent-4.5.0.23604.tar.gz .

cp ../../dockerization/verification/Dockerfile-verification-jenkins-k8 ./Dockerfile
cp ../../dockerization/verification/Dockerfile-verification-jenkins-k8-gcr ./Dockerfile-gcr
cp -R ../../dockerization/verification/scripts/ .
cp -r ../../dockerization/common-resources/ .
echo ${VERSION} > version.txt
cd ../..


mkdir -p dist/delegate
cd dist/delegate
cp ../../81-delegate/target/delegate-capsule.jar .
cd ../..
cp 81-delegate/target/delegate-capsule.jar delegate-${VERSION}.jar

mkdir -p dist/watcher
cd dist/watcher
cp ../../82-watcher/target/watcher-capsule.jar .
cd ../..
cp 82-watcher/target/watcher-capsule.jar watcher-${VERSION}.jar

mkdir -p dist/disconnected_on_prem_pov
cd dist/disconnected_on_prem_pov
cp -r ../../on-prem/harness_disconnected_on_prem_pov_final .
cp -r ../../on-prem/disconnected_on_prem_pov_installer .
tar -zcvf disconnected_on_prem_pov_template.tar.gz *
cd ../..
cp dist/disconnected_on_prem_pov/disconnected_on_prem_pov_template.tar.gz disconnected_on_prem_pov_template.tar.gz

mkdir -p dist/test
cd dist/test
cp ../../91-model-gen-tool/target/model-gen-tool-capsule.jar .
cp ../../91-model-gen-tool/config-datagen.yml .
cd ../..
