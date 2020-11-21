package io.harness.orchestration.persistence;

import io.harness.springdata.HMongoTemplate;

import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;

public class OrchestrationMongoTemplate extends HMongoTemplate {
  public OrchestrationMongoTemplate(MongoDbFactory mongoDbFactory, MongoConverter mongoConverter) {
    super(mongoDbFactory, mongoConverter);
  }
}
