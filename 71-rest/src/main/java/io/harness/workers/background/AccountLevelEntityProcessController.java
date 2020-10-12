package io.harness.workers.background;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.EntityProcessController;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.exception.AccountNotFoundException;
import software.wings.service.intfc.AccountService;

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
      logger.warn("Skipping processing account {}. It does not exist", accountId, ex);
      return false;
    }
    return AccountStatus.ACTIVE.equals(accountStatus);
  }
}
