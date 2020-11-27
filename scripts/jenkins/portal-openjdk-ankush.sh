### Dockerization of Manager ####### Doc

mkdir -p dist ;
cd dist

cp -R ../scripts/jenkins/ .
cd ..

mkdir -p dist/notification-service
cd dist/notification-service

cp ${HOME}/.bazel-dirs/bin/930-notification-service/module_deploy.jar notification-service-capsule.jar
cp ../../930-notification-service/config.yml .
cp ../../930-notification-service/keystore.jks .
cp ../../930-notification-service/key.pem .
cp ../../930-notification-service/cert.pem .
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