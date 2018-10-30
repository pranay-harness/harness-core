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
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.InstanceStatsCollectorJob;

/**
 * @author rktummala on 10/08/2018
 */
public class AddInstanceStatsCollectionJobToAllAccounts implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(AddInstanceStatsCollectionJobToAllAccounts.class);
  @Inject private WingsPersistence wingsPersistence;
  @Inject @Named("JobScheduler") private transient PersistentScheduler jobScheduler;

  @Override
  public void migrate() {
    Query<Account> query = wingsPersistence.createQuery(Account.class);

    try (HIterator<Account> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        Account account = records.next();
        InstanceStatsCollectorJob.delete(jobScheduler, account.getUuid());
        InstanceStatsCollectorJob.add(jobScheduler, account);
        logger.info("Added InstanceStatsCollectorJob for account {}", account.getUuid());
      }
    }
  }
}
