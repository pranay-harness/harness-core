package io.harness.mongo.tracing;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

public interface Tracer {
  void trace(Document queryDoc, String collectionName, MongoTemplate mongoTemplate);
}
