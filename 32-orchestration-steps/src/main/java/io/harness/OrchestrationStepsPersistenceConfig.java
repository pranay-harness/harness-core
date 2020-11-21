package io.harness;

import io.harness.annotation.HarnessRepo;
import io.harness.orchestration.persistence.OrchestrationBasePersistenceConfig;
import io.harness.spring.AliasRegistrar;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Collections;
import java.util.Set;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"io.harness.steps"}, includeFilters = @ComponentScan.Filter(HarnessRepo.class),
    mongoTemplateRef = "orchestrationMongoTemplate")
public class OrchestrationStepsPersistenceConfig extends OrchestrationBasePersistenceConfig {
  @Inject
  public OrchestrationStepsPersistenceConfig(Injector injector, Set<Class<? extends AliasRegistrar>> aliasRegistrars) {
    super(injector, aliasRegistrars, Collections.emptyList());
  }
}
