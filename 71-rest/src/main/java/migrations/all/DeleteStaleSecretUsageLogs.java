package migrations.all;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.SecretUsageLog;

import java.time.OffsetDateTime;

@Slf4j
public class DeleteStaleSecretUsageLogs implements Migration {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    logger.info("Deleting stale secret usage log");
    Query<Account> query = wingsPersistence.createQuery(Account.class);
    long toBeDeleted = OffsetDateTime.now().minusMonths(6).toInstant().toEpochMilli();
    try (HIterator<Account> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        Account account = records.next();
        logger.info("Deleting stale secret usage log for {} id {}", account.getAccountName(), account.getUuid());
        wingsPersistence.delete(wingsPersistence.createQuery(SecretUsageLog.class)
                                    .filter("accountId", account.getUuid())
                                    .field(SecretUsageLog.CREATED_AT_KEY)
                                    .lessThan(toBeDeleted));
      }
    }
  }
}