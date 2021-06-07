package io.harness.mongo.tracing;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

public interface Tracer {
  void trace(Document queryDoc, Document sortDoc, String collectionName, MongoTemplate mongoTemplate);
}
