package io.harness.delegate.service.impl;

import io.harness.delegate.beans.DelegateRing;
import io.harness.delegate.beans.DelegateRing.DelegateRingKeys;
import io.harness.delegate.service.intfc.DelegateRingService;
import io.harness.persistence.HPersistence;

import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;

public class DelegateRingServiceImpl implements DelegateRingService {
  private final HPersistence persistence;
  private final AccountService accountService;

  @Inject
  public DelegateRingServiceImpl(HPersistence persistence, AccountService accountService) {
    this.persistence = persistence;
    this.accountService = accountService;
  }

  @Override
  public String getDelegateImageTag(String accountId) {
    return (persistence.createQuery(DelegateRing.class)
                .filter(DelegateRingKeys.ringName, accountService.get(accountId).getRingName())
                .get())
        .getDelegateImageTag();
  }

  @Override
  public String getUpgraderImageTag(String accountId) {
    return (persistence.createQuery(DelegateRing.class)
                .filter(DelegateRingKeys.ringName, accountService.get(accountId).getRingName())
                .get())
        .getUpgraderImageTag();
  }
}
