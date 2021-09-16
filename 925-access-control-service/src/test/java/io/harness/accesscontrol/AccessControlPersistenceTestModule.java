package io.harness.accesscontrol;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.springdata.SpringPersistenceModule;

@OwnedBy(PL)
public class AccessControlPersistenceTestModule extends SpringPersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class<?>[] {AccessControlPersistenceTestConfig.class};
  }
}
