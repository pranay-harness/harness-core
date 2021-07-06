package io.harness.migrations.all;

import io.harness.beans.SecretUsageLog;
import io.harness.beans.SecretUsageLog.SecretUsageLogKeys;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Slf4j
public class DeleteStaleSecretUsageLogs implements Migration {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info("Deleting stale secret usage log");
    Query<Account> query = wingsPersistence.createQuery(Account.class);
    long toBeDeleted = OffsetDateTime.now().minusMonths(6).toInstant().toEpochMilli();
    try (HIterator<Account> records = new HIterator<>(query.fetch())) {
      for (Account account : records) {
        log.info("Deleting stale secret usage log for {} id {}", account.getAccountName(), account.getUuid());
        wingsPersistence.delete(wingsPersistence.createQuery(SecretUsageLog.class)
                                    .filter(SecretUsageLogKeys.accountId, account.getUuid())
                                    .field(SecretUsageLogKeys.createdAt)
                                    .lessThan(toBeDeleted));
      }
    }
  }
}
