package io.harness.cdng;

import com.mongodb.MongoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.guice.annotation.EnableGuiceModules;

@EnableMongoRepositories(basePackages = CDNextGenConfiguration.BASE_PACKAGE)
@EnableGuiceModules
public class CDNextGenSpringConfiguration {
  @Autowired private MongoClient mongoClient;

  @Bean
  private MongoTemplate mongoTemplate() {
    return new MongoTemplate(mongoClient, "cd-nextgen");
  }
}
