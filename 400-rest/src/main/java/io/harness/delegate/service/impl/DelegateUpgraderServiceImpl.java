package io.harness.delegate.service.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateRing;
import io.harness.delegate.beans.DelegateRing.DelegateRingKeys;
import io.harness.delegate.beans.UpgradeCheckResult;
import io.harness.delegate.service.intfc.DelegateUpgraderService;
import io.harness.persistence.HPersistence;

import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;

@Singleton
@ValidateOnExecution
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateUpgraderServiceImpl implements DelegateUpgraderService {
  private final HPersistence persistence;
  private final AccountService accountService;

  @Inject
  public DelegateUpgraderServiceImpl(HPersistence persistence, AccountService accountService) {
    this.persistence = persistence;
    this.accountService = accountService;
  }

  @Override
  public UpgradeCheckResult getDelegateImageTag(String accountId, String currentDelegateImageTag) {
    // TODO: ARPIT implement cache mechanism for getting ImageTag from DelegateRing class
    DelegateRing delegateRing = persistence.createQuery(DelegateRing.class)
                                    .filter(DelegateRingKeys.ringName, accountService.get(accountId).getRingName())
                                    .get();
    boolean shouldUpgrade = !currentDelegateImageTag.equals(delegateRing.getDelegateImageTag());
    if (shouldUpgrade) {
      return new UpgradeCheckResult(delegateRing.getDelegateImageTag(), shouldUpgrade);
    }
    return new UpgradeCheckResult(currentDelegateImageTag, shouldUpgrade);
  }

  @Override
  public UpgradeCheckResult getUpgraderImageTag(String accountId, String currentUpgraderImageTag) {
    // TODO: ARPIT implement cache mechanism for getting ImageTag from DelegateRing class
    DelegateRing delegateRing = persistence.createQuery(DelegateRing.class)
                                    .filter(DelegateRingKeys.ringName, accountService.get(accountId).getRingName())
                                    .get();
    boolean shouldUpgrade = !currentUpgraderImageTag.equals(delegateRing.getUpgraderImageTag());
    if (shouldUpgrade) {
      return new UpgradeCheckResult(delegateRing.getUpgraderImageTag(), shouldUpgrade);
    }
    return new UpgradeCheckResult(currentUpgraderImageTag, shouldUpgrade);
  }
}
