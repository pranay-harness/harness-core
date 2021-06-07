package io.harness.tracing;

import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.mongo.tracing.Tracer;
import io.harness.observer.AsyncInformObserver;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.concurrent.ExecutorService;
import javax.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

@Slf4j
public class MongoRedisTracer implements Tracer, AsyncInformObserver {
  @Inject @Named(PersistenceTracerConstants.TRACING_THREAD_POOL) private ExecutorService executorService;
  @Inject @Named(PersistenceTracerConstants.QUERY_ANALYSIS_PRODUCER) private Producer producer;

  @Override
  public void trace(Document queryDoc, String collectionName, MongoTemplate mongoTemplate) {
    // TODO : Check with the ShapeDetector and ge the query hash
    log.debug("Tracing Query {}", queryDoc.toJson());
    Document explainDocument = new Document();
    explainDocument.put("find", collectionName);
    explainDocument.put("filter", queryDoc);

    Document command = new Document();
    command.put("explain", explainDocument);

    // TODO: Check if we have this hash stored in cache, if not run explain
    Document explainResult = mongoTemplate.getDb().runCommand(command);

    log.debug("Explain Results");
    log.debug(explainResult.toJson());
    producer.send(Message.newBuilder().setData(ByteString.copyFromUtf8(explainResult.toJson())).build());
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executorService;
  }
}
