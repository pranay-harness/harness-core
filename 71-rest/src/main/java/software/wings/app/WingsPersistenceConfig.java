package software.wings.app;

import io.harness.springdata.SpringPersistenceConfig;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Collections;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WingsPersistenceConfig extends SpringPersistenceConfig {
  @Inject
  public WingsPersistenceConfig(Injector injector) {
    super(injector, Collections.emptyList());
  }
}
