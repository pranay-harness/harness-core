/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.workers.background;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.EntityProcessController;

import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.exception.AccountNotFoundException;
import software.wings.service.intfc.AccountService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PL)
public class AccountLevelEntityProcessController implements EntityProcessController<Account> {
  private final AccountService accountService;

  public AccountLevelEntityProcessController(AccountService accountService) {
    this.accountService = accountService;
  }

  @Override
  public boolean shouldProcessEntity(Account account) {
    String accountId = account.getUuid();
    String accountStatus;

    try {
      accountStatus = accountService.getAccountStatus(accountId);
    } catch (AccountNotFoundException ex) {
      log.warn("Skipping processing account {}. It does not exist", accountId, ex);
      accountService.handleNonExistentAccount(accountId);
      return false;
    }
    return AccountStatus.ACTIVE.equals(accountStatus);
  }
}
