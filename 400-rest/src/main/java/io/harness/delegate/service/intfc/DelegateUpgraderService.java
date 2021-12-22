package io.harness.delegate.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.UpgradeCheckResult;

@OwnedBy(HarnessTeam.DEL)
public interface DelegateUpgraderService {
  UpgradeCheckResult getDelegateImageTag(String accountId, String currentDelegateImageTag);

  UpgradeCheckResult getUpgraderImageTag(String accountId, String currentUpgraderImageTag);
}
