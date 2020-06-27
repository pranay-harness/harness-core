package io.harness.ng;

import com.google.inject.Inject;
import com.google.inject.Injector;

public class SpringPersistenceConfig extends SpringMongoConfig {
  @Inject
  public SpringPersistenceConfig(Injector injector) {
    super(injector);
  }
}
