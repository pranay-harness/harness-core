sed -i "s|uri: mongodb://localhost:27017/harness|uri: ${MONGO_URI}|" 71-rest/config.yml
sed -i 's|disabledCache: \[\]|disabledCache: \["userPermissionCache"\]|' 71-rest/config.yml
sed -i 's|disabledCaches: \[\]|disabledCaches: \["userPermissionCache"\]|' 71-rest/config.yml
sed -i "s|uri: http://localhost:9200|uri: ${ELASTICSEARCH_URI}|" 71-rest/config.yml
sed -i "s|indexSuffix: _default|indexSuffix: ${ELASTICSEARCH_INDEX_SUFFIX}|" 71-rest/config.yml
sed -i "s|searchEnabled: false|searchEnabled: true|" 71-rest/config.yml
sed -i "s|sentinel: false|sentinel: true|" 71-rest/config.yml
sed -i "s|masterName: \"test\"|masterName: ${REDIS_MASTER_NAME}|" 71-rest/config.yml
sed -i "s|envNamespace: \"\"|envNamespace: ${TIMESTAMP}|" 71-rest/config.yml
sed -i "s|redis://redis1:26379|${REDIS_SENTINEL1}|" 71-rest/config.yml
sed -i "s|redis://redis2:26379|${REDIS_SENTINEL2}|" 71-rest/config.yml
sed -i "s|redis://redis3:26379|${REDIS_SENTINEL3}|" 71-rest/config.yml
sed -i "s|distributedLockImplementation: MONGO|distributedLockImplementation: REDIS|" 71-rest/config.yml

sed -i "s|uri: mongodb://localhost:27017/harness|uri: ${MONGO_URI}|" 72-event-server/event-service-config.yml
sed -i "s|uri: mongodb://localhost:27017/harness|uri: ${MONGO_URI}|" 78-batch-processing/batch-processing-config.yml
sed -i "s|uri: mongodb://localhost:27017/events|uri: ${MONGO_URI}_events|" 78-batch-processing/batch-processing-config.yml
sed -i "s|uri: mongodb://localhost:27017/harness|uri: ${MONGO_URI}|" 210-command-library-server/command-library-server-config.yml
