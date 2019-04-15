package migrations.all;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.persistence.HIterator;
import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.InstanceStatsCollectorJob;

/**
 * @author rktummala on 10/08/2018
 */
@Slf4j
public class AddInstanceStatsCollectionJobToAllAccounts implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject @Named("BackgroundJobScheduler") private transient PersistentScheduler jobScheduler;

  @Override
  public void migrate() {
    Query<Account> query = wingsPersistence.createQuery(Account.class);

    try (HIterator<Account> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        Account account = records.next();
        InstanceStatsCollectorJob.delete(jobScheduler, account.getUuid());
        InstanceStatsCollectorJob.add(jobScheduler, account.getUuid());
        logger.info("Added InstanceStatsCollectorJob for account {}", account.getUuid());
      }
    }
  }
}
