package io.harness.ng;

import com.google.inject.Injector;

import io.harness.annotation.HarnessRepo;
import io.harness.connector.ConnectorPersistenceConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"io.harness.ng"}, includeFilters = @ComponentScan.Filter(HarnessRepo.class))
public class NextGenPersistenceConfig extends ConnectorPersistenceConfig {
  public NextGenPersistenceConfig(Injector injector) {
    super(injector);
  }
}
