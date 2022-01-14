package io.harness.mongo.tracing;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.HQuery;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.PIPELINE)
public interface Tracer {
  void traceSpringQuery(Query query, Class<?> entityClass, MongoTemplate mongoTemplate);
  void traceMorphiaQuery(HQuery<?> query);
}
