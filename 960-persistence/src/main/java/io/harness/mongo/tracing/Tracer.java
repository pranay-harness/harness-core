package io.harness.mongo.tracing;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(HarnessTeam.PIPELINE)
public interface Tracer {
  void trace(Document queryDoc, Document sortDoc, String collectionName, MongoTemplate mongoTemplate);
}
