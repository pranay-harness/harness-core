package io.harness.connector;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.SpringPersistenceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
    basePackages = {"io.harness.connector"}, includeFilters = @ComponentScan.Filter(HarnessRepo.class))
public class ConnectorPersistenceConfig extends SpringPersistenceConfig {
  @Inject
  public ConnectorPersistenceConfig(Injector injector) {
    super(injector);
  }

  @Bean(name = "connectorMongoTemplate")
  @Primary
  public MongoTemplate connectorMongoTemplate() throws Exception {
    return new MongoTemplate(mongoDbFactory(), mappingMongoConverter());
  }
}
