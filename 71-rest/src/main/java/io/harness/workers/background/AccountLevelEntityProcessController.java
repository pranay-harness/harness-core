package io.harness.workers.background;

import io.harness.mongo.EntityProcessController;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.service.intfc.AccountService;

public class AccountLevelEntityProcessController implements EntityProcessController<Account> {
  private final AccountService accountService;

  public AccountLevelEntityProcessController(AccountService accountService) {
    this.accountService = accountService;
  }

  @Override
  public boolean shouldProcessEntity(Account account) {
    String accountStatus = accountService.getAccountStatus(account.getUuid());
    return AccountStatus.ACTIVE.equals(accountStatus);
  }
}
