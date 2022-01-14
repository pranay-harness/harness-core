package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.persistence.HQuery;
import io.harness.service.intfc.DelegateNgTokenService;

import software.wings.beans.Account;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DefaultDelegateNgTokenMigration implements Migration {
  @Inject private HPersistence persistence;
  @Inject private DelegateNgTokenService delegateNgTokenService;

  @Override
  public void migrate() {
    generateDefaultDelegateTokenForAccounts();
  }

  private void generateDefaultDelegateTokenForAccounts() {
    log.info("Generating default tokens for accounts");
    Query<Account> accountsQuery = persistence.createQuery(Account.class, HQuery.excludeAuthority);
    try (HIterator<Account> accounts = new HIterator<>(accountsQuery.fetch())) {
      for (Account account : accountsQuery) {
        delegateNgTokenService.upsertDefaultToken(account.getUuid(), null, true);
        log.info("Successfully created default Delegate Token for account {}", account.getUuid());
      }
    }
  }
}
