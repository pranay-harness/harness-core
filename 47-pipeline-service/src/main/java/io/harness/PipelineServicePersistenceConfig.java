package io.harness;

import io.harness.annotation.HarnessRepo;
import io.harness.springdata.SpringPersistenceConfig;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Collections;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"io.harness.pms"}, includeFilters = @ComponentScan.Filter(HarnessRepo.class))
public class PipelineServicePersistenceConfig extends SpringPersistenceConfig {
  @Inject
  public PipelineServicePersistenceConfig(Injector injector) {
    super(injector, Collections.emptyList());
  }
}
