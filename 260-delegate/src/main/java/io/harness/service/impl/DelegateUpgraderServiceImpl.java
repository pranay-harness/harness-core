package io.harness.service.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateRing;
import io.harness.delegate.beans.DelegateRing.DelegateRingKeys;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateUpgraderService;

import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Singleton
@ValidateOnExecution
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateUpgraderServiceImpl implements DelegateUpgraderService {
  private final AccountService accountService;
  private final HPersistence persistence;

  @Inject
  public DelegateUpgraderServiceImpl(AccountService accountService, HPersistence persistence) {
    this.accountService = accountService;
    this.persistence = persistence;
  }

  @Override
  public Pair<Boolean, String> getDelegateImageTag(String accountId, String currentDelegateImageTag) {
    String ringName = accountService.getDelegateRingAssociatedWithAccount(accountId);

    // TODO: implement cache mechanism for getting ImageTag from DelegateRing class
    DelegateRing delegateRing =
        persistence.createQuery(DelegateRing.class).filter(DelegateRingKeys.ringName, ringName).get();
    boolean shouldUpgrade = !currentDelegateImageTag.equals(delegateRing.getDelegateImageTag());
    if (shouldUpgrade) {
      return Pair.of(shouldUpgrade, delegateRing.getDelegateImageTag());
    }
    return Pair.of(shouldUpgrade, currentDelegateImageTag);
  }

  @Override
  public Pair<Boolean, String> getUpgraderImageTag(String accountId, String currentUpgraderImageTag) {
    String ringName = accountService.getDelegateRingAssociatedWithAccount(accountId);

    // TODO: implement cache mechanism for getting ImageTag from DelegateRing class
    DelegateRing delegateRing =
        persistence.createQuery(DelegateRing.class).filter(DelegateRingKeys.ringName, ringName).get();
    boolean shouldUpgrade = !currentUpgraderImageTag.equals(delegateRing.getUpgraderImageTag());
    if (shouldUpgrade) {
      return Pair.of(shouldUpgrade, delegateRing.getUpgraderImageTag());
    }
    return Pair.of(shouldUpgrade, currentUpgraderImageTag);
  }
}
