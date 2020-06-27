package software.wings.app;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.OrchestrationPersistenceConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WingsPersistenceConfig extends OrchestrationPersistenceConfig {
  @Inject
  public WingsPersistenceConfig(Injector injector) {
    super(injector);
  }
}
