package io.harness.platform.audit;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.springdata.PersistenceModule;

@OwnedBy(PL)
public class AuditPersistenceModule extends PersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class[] {AuditPersistenceConfig.class};
  }
}
