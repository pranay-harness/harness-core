package migrations.all;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.persistence.HIterator;
import io.harness.scheduler.PersistentScheduler;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.Base;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.LimitVicinityCheckerJob;

public class AddLimitVicinityCheckJobToAllAccounts implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(AddLimitVicinityCheckJobToAllAccounts.class);
  @Inject private WingsPersistence wingsPersistence;
  @Inject @Named("BackgroundJobScheduler") private transient PersistentScheduler jobScheduler;

  @Override
  public void migrate() {
    Query<Account> query = wingsPersistence.createQuery(Account.class);

    try (HIterator<Account> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        Account account = records.next();
        if (Base.GLOBAL_ACCOUNT_ID.equals(account.getUuid())) {
          continue;
        }
        LimitVicinityCheckerJob.delete(jobScheduler, account.getUuid());
        LimitVicinityCheckerJob.add(jobScheduler, account.getUuid());
        logger.info("Added LimitVicinityCheckerJob for account {}", account.getUuid());
      }
    }
  }
}
