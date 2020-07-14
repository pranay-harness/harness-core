package software.wings.app;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.SpringPersistenceConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongoRepositories(basePackages = {"io.harness.cdng"}, includeFilters = @ComponentScan.Filter(HarnessRepo.class))
@Configuration
public class WingsPersistenceConfig extends SpringPersistenceConfig {
  @Inject
  public WingsPersistenceConfig(Injector injector) {
    super(injector);
  }
}
