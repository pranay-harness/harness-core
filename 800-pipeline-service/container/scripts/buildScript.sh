bazel ${bazelrc} build //800-pipeline-service:module `bazel query "//...:*" | grep "module_deploy.jar"` ${BAZEL_ARGUMENTS} --remote_download_outputs=all

#Commands to compute hash for all registrars
bazel query "deps(//800-pipeline-service:module)" | grep -i "KryoRegistrar" | rev | cut -f 1 -d | cut -f 1 -d "." > /tmp/KryoDeps.text
cp scripts/interface-hash/module-deps.sh .
sh module-deps.sh //800-pipeline-service:module > /tmp/ProtoDeps.text
bazel ${bazelrc} run ${BAZEL_ARGUMENTS}  //001-microservice-intfc-tool:module -- kryo-file=/tmptext proto-file=/tmp/ProtoDeps.text ignore-json | grep "Codebase Hash:" > protocol.info

#Download the apln jar
curl https://storage.googleapis.com/harness-prod-public/public/shared/tools/alpn/release/8.1.13.alpn-boot-8.1.13.v20181017.jar  --output alpn-boot-8.1.13.v20181017.jar

mkdir -p dist/pipeline-service
cd dist/pipeline-service

cp ${HOME}/.bazel-dirs/bin/800-pipeline-service/module_deploy.jar pipeline-service-capsule.jar
cp ../../800-pipeline-service/config.yml .
cp ../../800-pipeline-service/keystore.jks .
cp ../../800-pipeline-service/key.pem .
cp ../../800-pipeline-service/cert.pem .
cp ../../800-pipeline-service/src/main/resources/redisson-jcache.yaml .

cp ../../alpn-boot-8.1.13.v20181017.jar .
cp ../../dockerization/pipeline-service/Dockerfile-pipeline-service-jenkins-k8-openjdk ./Dockerfile
cp ../../dockerization/pipeline-service/Dockerfile-pipeline-service-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -r ../../dockerization/pipeline-service/scripts/ .
cp ../../protocol.info .