package io.harness;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.annotation.HarnessRepo;
import io.harness.orchestration.persistence.OrchestrationBasePersistenceConfig;
import io.harness.spring.AliasRegistrar;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.Set;

@Configuration
@EnableMongoRepositories(basePackages = {"io.harness.steps"}, includeFilters = @ComponentScan.Filter(HarnessRepo.class),
    mongoTemplateRef = "orchestrationMongoTemplate")
public class OrchestrationStepsPersistenceConfig extends OrchestrationBasePersistenceConfig {
  @Inject
  public OrchestrationStepsPersistenceConfig(Injector injector, Set<Class<? extends AliasRegistrar>> aliasRegistrars) {
    super(injector, aliasRegistrars);
  }
}
