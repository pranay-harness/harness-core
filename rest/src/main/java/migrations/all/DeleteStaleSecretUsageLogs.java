package migrations.all;

import com.google.inject.Inject;

import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.dl.HIterator;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.SecretUsageLog;

import java.time.OffsetDateTime;

public class DeleteStaleSecretUsageLogs implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(DeleteStaleSecretUsageLogs.class);

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
                                    .field("createdAt")
                                    .lessThan(toBeDeleted));
      }
    }
  }
}