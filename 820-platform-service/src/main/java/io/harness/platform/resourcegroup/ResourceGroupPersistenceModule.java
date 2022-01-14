package io.harness.platform.resourcegroup;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.springdata.PersistenceModule;

@OwnedBy(PL)
public class ResourceGroupPersistenceModule extends PersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class[] {ResourceGroupPersistenceConfig.class};
  }
}
