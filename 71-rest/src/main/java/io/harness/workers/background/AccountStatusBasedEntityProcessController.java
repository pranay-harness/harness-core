package io.harness.workers.background;

import io.harness.iterator.PersistentIterable;
import io.harness.mongo.EntityProcessController;
import io.harness.persistence.AccountAccess;
import software.wings.beans.AccountStatus;
import software.wings.service.intfc.AccountService;

public class AccountStatusBasedEntityProcessController<T extends PersistentIterable & AccountAccess>
    implements EntityProcessController<T> {
  private final AccountService accountService;

  public AccountStatusBasedEntityProcessController(AccountService accountService) {
    this.accountService = accountService;
  }

  @Override
  public boolean shouldProcessEntity(T entity) {
    String accountId = entity.getAccountId();
    String accountStatus = accountService.getAccountStatus(accountId);
    return AccountStatus.ACTIVE.equals(accountStatus);
  }
}
