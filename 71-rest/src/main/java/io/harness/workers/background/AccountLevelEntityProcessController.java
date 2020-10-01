package io.harness.workers.background;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.EntityProcessController;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.service.intfc.AccountService;

@OwnedBy(PL)
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
