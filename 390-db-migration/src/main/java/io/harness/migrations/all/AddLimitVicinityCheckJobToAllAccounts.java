package io.harness.migrations.all;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;
import io.harness.scheduler.PersistentScheduler;

import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.LimitVicinityCheckerJob;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Slf4j
public class AddLimitVicinityCheckJobToAllAccounts implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject @Named("BackgroundJobScheduler") private transient PersistentScheduler jobScheduler;

  @Override
  public void migrate() {
    Query<Account> query = wingsPersistence.createQuery(Account.class);

    try (HIterator<Account> records = new HIterator<>(query.fetch())) {
      for (Account account : records) {
        if (GLOBAL_ACCOUNT_ID.equals(account.getUuid())) {
          continue;
        }
        LimitVicinityCheckerJob.delete(jobScheduler, account.getUuid());
        LimitVicinityCheckerJob.add(jobScheduler, account.getUuid());
        log.info("Added LimitVicinityCheckerJob for account {}", account.getUuid());
      }
    }
  }
}
