package io.harness.app;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.annotation.HarnessRepo;
import io.harness.springdata.SpringPersistenceConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
    basePackages = {"io.harness.app.dao"}, includeFilters = @ComponentScan.Filter(HarnessRepo.class))
public class CIManagerPersistenceConfig extends SpringPersistenceConfig {
  @Inject
  public CIManagerPersistenceConfig(Injector injector) {
    super(injector);
  }
}
