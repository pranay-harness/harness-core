package software.wings.app;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.springdata.SpringPersistenceConfig;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class WingsPersistenceConfig extends SpringPersistenceConfig {
  @Inject
  public WingsPersistenceConfig(Injector injector) {
    super(injector, Collections.emptyList());
  }
}
