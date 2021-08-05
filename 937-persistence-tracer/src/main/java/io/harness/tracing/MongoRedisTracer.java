package io.harness.tracing;

import static io.harness.mongo.tracing.TracerConstants.ANALYZER_CACHE_KEY;
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
  public void trace(Document queryDoc, Document sortDoc, String collectionName, MongoTemplate mongoTemplate) {
    try {
      String qHash = QueryShapeDetector.getQueryHash(collectionName, queryDoc, sortDoc);
      String queryStatsCacheKey = String.format(ANALYZER_CACHE_KEY, serviceId);
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
        Document explainDocument = new Document();
        explainDocument.put("find", collectionName);
        explainDocument.put("filter", queryDoc);
        explainDocument.put("sort", sortDoc);

        Document command = new Document();
        command.put("explain", explainDocument);

        Document explainResult = mongoTemplate.getDb().runCommand(command);

        log.debug("Explain Results");
        log.debug(explainResult.toJson());
        producer.send(Message.newBuilder()
                          .putMetadata(VERSION_KEY, versionInfoManager.getVersionInfo().getVersion())
                          .putMetadata(SERVICE_ID, serviceId)
                          .putMetadata(QUERY_HASH, qHash)
                          .setData(ByteString.copyFromUtf8(explainResult.toJson()))
                          .build());
        queryStatsCache.put(qHash, 1L);
      });
    } catch (Exception ex) {
      log.error("Unable to trace the query {}", queryDoc);
    }
  }
}
