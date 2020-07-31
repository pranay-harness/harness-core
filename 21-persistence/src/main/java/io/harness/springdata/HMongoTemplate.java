package io.harness.springdata;

import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;

public class HMongoTemplate extends MongoTemplate {
  public HMongoTemplate(MongoDbFactory mongoDbFactory, MongoConverter mongoConverter) {
    super(mongoDbFactory, mongoConverter);
  }
}
