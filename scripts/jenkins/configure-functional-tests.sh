sed -i "s|uri: mongodb://localhost:27017/harness|uri: ${MONGO_URI}|" 400-rest/config.yml
sed -i 's|disabledCache: \[\]|disabledCache: \["userPermissionCache"\]|' 400-rest/config.yml
sed -i 's|disabledCaches: \[\]|disabledCaches: \["userPermissionCache"\]|' 400-rest/config.yml
sed -i "s|uri: http://localhost:9200|uri: ${ELASTICSEARCH_URI}|" 400-rest/config.yml
sed -i "s|indexSuffix: _default|indexSuffix: ${ELASTICSEARCH_INDEX_SUFFIX}|" 400-rest/config.yml
sed -i "s|searchEnabled: false|searchEnabled: true|" 400-rest/config.yml
sed -i "s|sentinel: false|sentinel: true|" 400-rest/config.yml
sed -i "s|masterName: \"test\"|masterName: ${REDIS_MASTER_NAME}|" 400-rest/config.yml
sed -i "s|envNamespace: \"\"|envNamespace: ${TIMESTAMP}|" 400-rest/config.yml
sed -i "s|redis://redis1:26379|${REDIS_SENTINEL1}|" 400-rest/config.yml
sed -i "s|redis://redis2:26379|${REDIS_SENTINEL2}|" 400-rest/config.yml
sed -i "s|redis://redis3:26379|${REDIS_SENTINEL3}|" 400-rest/config.yml
sed -i "s|distributedLockImplementation: MONGO|distributedLockImplementation: REDIS|" 400-rest/config.yml

sed -i "s|uri: mongodb://localhost:27017/harness|uri: ${MONGO_URI}|" 350-event-server/event-service-config.yml
sed -i "s|uri: mongodb://localhost:27017/harness|uri: ${MONGO_URI}|" 280-batch-processing/batch-processing-config.yml
sed -i "s|uri: mongodb://localhost:27017/events|uri: ${MONGO_URI}_events|" 280-batch-processing/batch-processing-config.yml
sed -i "s|uri: mongodb://localhost:27017/harness|uri: ${MONGO_URI}|" 210-command-library-server/command-library-server-config.yml
sed -i "s|uri: mongodb://localhost:27017/harness|uri: ${MONGO_URI}|" 310-ci-manager/ci-manager-config.yml
