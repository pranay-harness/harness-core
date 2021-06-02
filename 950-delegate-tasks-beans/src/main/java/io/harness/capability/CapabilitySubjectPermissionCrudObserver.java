package io.harness.capability;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._920_DELEGATE_SERVICE_BEANS)
public interface CapabilitySubjectPermissionCrudObserver {
  void onBlockingPermissionsCreated(String accountId, String delegateId);
}