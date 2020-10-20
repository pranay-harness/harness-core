package io.harness.pms;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.annotation.HarnessRepo;
import io.harness.orchestration.persistence.OrchestrationBasePersistenceConfig;
import io.harness.pms.mongo.NodeExecutionReadConverter;
import io.harness.pms.mongo.NodeExecutionWriteConverter;
import io.harness.spring.AliasRegistrar;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.Set;

@Configuration
@EnableMongoRepositories(basePackages = {"io.harness.pms"}, includeFilters = @ComponentScan.Filter(HarnessRepo.class),
    mongoTemplateRef = "orchestrationMongoTemplate")
public class PmsPersistenceConfig extends OrchestrationBasePersistenceConfig {
  @Inject
  public PmsPersistenceConfig(Injector injector, Set<Class<? extends AliasRegistrar>> aliasRegistrars) {
    super(injector, aliasRegistrars);
  }

  @Override
  public CustomConversions customConversions() {
    return new MongoCustomConversions(
        ImmutableList.of(new NodeExecutionWriteConverter(), new NodeExecutionReadConverter()));
  }
}