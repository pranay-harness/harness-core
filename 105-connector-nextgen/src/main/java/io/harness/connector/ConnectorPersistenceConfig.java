package io.harness.connector;

import com.google.inject.Injector;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.SpringPersistenceConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
    basePackages = {"io.harness.connector"}, includeFilters = @ComponentScan.Filter(HarnessRepo.class))
public class ConnectorPersistenceConfig extends SpringPersistenceConfig {
  public ConnectorPersistenceConfig(Injector injector) {
    super(injector);
  }
}
