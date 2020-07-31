package io.harness;

import com.google.inject.Injector;

import io.harness.annotation.HarnessRepo;
import io.harness.springdata.SpringPersistenceConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"io.harness.steps"}, includeFilters = @ComponentScan.Filter(HarnessRepo.class),
    mongoTemplateRef = "orchestrationMongoTemplate")
public class OrchestrationStepsPersistenceConfig extends SpringPersistenceConfig {
  public OrchestrationStepsPersistenceConfig(Injector injector) {
    super(injector);
  }
}
