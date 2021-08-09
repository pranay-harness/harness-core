package io.harness.tracing;

import static io.harness.mongo.tracing.TracerConstants.ANALYZER_CACHE_NAME;
import static io.harness.mongo.tracing.TracerConstants.QUERY_HASH;
import static io.harness.mongo.tracing.TracerConstants.SERVICE_ID;
import static io.harness.version.VersionConstants.VERSION_KEY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.mongo.tracing.Tracer;
import io.harness.tracing.shapedetector.QueryShapeDetector;
import io.harness.version.VersionInfoManager;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.util.concurrent.ExecutorService;
import javax.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.query.Query;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class MongoRedisTracer implements Tracer {
  private static final int SAMPLE_SIZE = 120; // For per seconds poller sample every 2 min

  @Inject @Named(PersistenceTracerConstants.TRACING_THREAD_POOL) private ExecutorService executorService;
  @Inject @Named(PersistenceTracerConstants.QUERY_ANALYSIS_PRODUCER) private Producer producer;
  @Inject @Named(SERVICE_ID) private String serviceId;
  @Inject private VersionInfoManager versionInfoManager;

  @Inject @Named(ANALYZER_CACHE_NAME) Cache<String, Long> queryStatsCache;

  @Override
  public void trace(Query query, Class<?> entityClass, MongoTemplate mongoTemplate) {
    try {
      traceInternal(query, entityClass, mongoTemplate);
    } catch (Exception ex) {
      log.error("Unable to trace query: {}", query.getQueryObject());
    }
  }

  private void traceInternal(Query query, Class<?> entityClass, MongoTemplate mongoTemplate) {
    String collectionName = mongoTemplate.getCollectionName(entityClass);
    Document queryDoc = query.getQueryObject();
    Document sortDoc = query.getSortObject();
    String qHash = QueryShapeDetector.getQueryHash(collectionName, queryDoc, sortDoc);
    if (queryStatsCache.containsKey(qHash)) {
      Long count = queryStatsCache.get(qHash);
      count = count + 1;
      queryStatsCache.put(qHash, count);
      if (count % SAMPLE_SIZE != 0) {
        return;
      }
      log.info("Sampling the query....");
    }

    executorService.submit(() -> {
      // Recalculate hash based on properly mapped queries
      MongoConverter mongoConverter = mongoTemplate.getConverter();
      MongoPersistentEntity<?> entity = mongoConverter.getMappingContext().getPersistentEntity(entityClass);
      QueryMapper queryMapper = new QueryMapper(mongoConverter);
      Document finalQueryDoc = queryMapper.getMappedObject(query.getQueryObject(), entity);
      Document finalSortDoc = queryMapper.getMappedSort(query.getSortObject(), entity);
      String finalQHash = QueryShapeDetector.getQueryHash(collectionName, finalQueryDoc, finalSortDoc);

      Document explainDocument = new Document();
      explainDocument.put("find", collectionName);
      explainDocument.put("filter", finalQueryDoc);
      explainDocument.put("sort", finalSortDoc);

      Document command = new Document();
      command.put("explain", explainDocument);

      Document explainResult = mongoTemplate.getDb().runCommand(command);

      log.debug("Explain Results");
      log.debug(explainResult.toJson());
      producer.send(Message.newBuilder()
                        .putMetadata(VERSION_KEY, versionInfoManager.getVersionInfo().getVersion())
                        .putMetadata(SERVICE_ID, serviceId)
                        .putMetadata(QUERY_HASH, finalQHash)
                        .setData(ByteString.copyFromUtf8(explainResult.toJson()))
                        .build());
      queryStatsCache.put(finalQHash, 1L);
    });
  }
}
