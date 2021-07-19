package io.harness;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.springdata.SpringPersistenceConfig;
import io.harness.springdata.SpringPersistenceModule;

@OwnedBy(CDC)
public class TemplateServicePersistenceModule extends SpringPersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class<?>[] {SpringPersistenceConfig.class};
  }
}
