package io.harness.cdng;

import io.harness.annotation.HarnessRepo;
import io.harness.springdata.SpringPersistenceConfig;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Collections;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"io.harness.cdng"}, includeFilters = @ComponentScan.Filter(HarnessRepo.class))
public class CDNGPersistenceConfig extends SpringPersistenceConfig {
  @Inject
  public CDNGPersistenceConfig(Injector injector) {
    super(injector, Collections.emptyList());
  }
}
