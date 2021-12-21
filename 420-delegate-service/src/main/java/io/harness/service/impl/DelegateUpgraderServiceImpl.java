package io.harness.service.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.UpgradeCheckResult;
import io.harness.service.intfc.DelegateUpgraderService;

import com.google.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;

@Singleton
@ValidateOnExecution
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateUpgraderServiceImpl implements DelegateUpgraderService {
  private static final String DELEGATE_IMAGE_TAG = "harness/delegate:latest";
  private static final String UPGRADER_IMAGE_TAG = "harness/upgrader:latest";

  @Override
  public UpgradeCheckResult getDelegateImageTag(String accountId, String currentDelegateImageTag) {
    return new UpgradeCheckResult(DELEGATE_IMAGE_TAG, false);
  }

  @Override
  public UpgradeCheckResult getUpgraderImageTag(String accountId, String currentUpgraderImageTag) {
    return new UpgradeCheckResult(UPGRADER_IMAGE_TAG, false);
  }
}
