package io.harness.ng;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.annotation.HarnessRepo;
import io.harness.springdata.SpringPersistenceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.Collections;

@Configuration
@EnableMongoRepositories(basePackages = {"io.harness.ng"}, includeFilters = @ComponentScan.Filter(HarnessRepo.class))
public class NextGenPersistenceConfig extends SpringPersistenceConfig {
  @Inject
  public NextGenPersistenceConfig(Injector injector) {
    super(injector, Collections.emptyList());
  }

  @Bean
  MongoTransactionManager transactionManager(MongoDbFactory dbFactory) {
    return new MongoTransactionManager(dbFactory);
  }
}
