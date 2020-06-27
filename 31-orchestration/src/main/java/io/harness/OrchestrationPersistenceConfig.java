package io.harness;

import com.google.inject.Injector;

import io.harness.annotation.HarnessRepo;
import io.harness.engine.executions.node.NodeExecutionAfterSaveListener;
import io.harness.ng.SpringPersistenceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
    basePackages = {"io.harness.engine"}, includeFilters = @ComponentScan.Filter(HarnessRepo.class))
public class OrchestrationPersistenceConfig extends SpringPersistenceConfig {
  public OrchestrationPersistenceConfig(Injector injector) {
    super(injector);
  }

  @Bean
  public NodeExecutionAfterSaveListener nodeExecutionAfterSaveListener() {
    return new NodeExecutionAfterSaveListener();
  }
}
