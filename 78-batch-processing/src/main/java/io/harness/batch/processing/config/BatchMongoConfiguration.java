package io.harness.batch.processing.config;

import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;

@Configuration
@Slf4j
public class BatchMongoConfiguration {
  @Autowired @Qualifier("primaryDatastore") private AdvancedDatastore primaryDatastore;

  @Bean
  public MongoDbFactory mongoDbFactory() {
    return new SimpleMongoDbFactory(primaryDatastore.getMongo(), primaryDatastore.getDB().getName());
  }

  @Bean
  public MongoTemplate mongoTemplate(MongoDbFactory mongoDbFactory) {
    return new MongoTemplate(mongoDbFactory);
  }
}
