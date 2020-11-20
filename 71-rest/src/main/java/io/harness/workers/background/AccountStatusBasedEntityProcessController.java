package io.harness.workers.background;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistentIterable;
import io.harness.mongo.EntityProcessController;
import io.harness.persistence.AccountAccess;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AccountStatus;
import software.wings.exception.AccountNotFoundException;
import software.wings.service.intfc.AccountService;

@Slf4j
@OwnedBy(PL)
public class AccountStatusBasedEntityProcessController<T extends PersistentIterable & AccountAccess>
    implements EntityProcessController<T> {
  private final AccountService accountService;

  public AccountStatusBasedEntityProcessController(AccountService accountService) {
    this.accountService = accountService;
  }

  @Override
  public boolean shouldProcessEntity(T entity) {
    String accountId = entity.getAccountId();
    String accountStatus;

    try {
      accountStatus = accountService.getAccountStatus(accountId);
    } catch (AccountNotFoundException ex) {
      log.warn("Skipping processing entity. Account {} does not exist", accountId, ex);
      accountService.handleNonExistentAccount(accountId);
      return false;
    }
    return AccountStatus.ACTIVE.equals(accountStatus);
  }
}
