package io.harness;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.annotation.HarnessRepo;
import io.harness.springdata.SpringPersistenceConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
    basePackages = {"io.harness.ci.execution.dao"}, includeFilters = @ComponentScan.Filter(HarnessRepo.class))
public class CIExecutionPersistenceConfig extends SpringPersistenceConfig {
  @Inject
  public CIExecutionPersistenceConfig(Injector injector) {
    super(injector);
  }
}
