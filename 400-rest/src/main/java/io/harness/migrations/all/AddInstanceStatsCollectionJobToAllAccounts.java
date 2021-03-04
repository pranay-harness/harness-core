package io.harness.migrations.all;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;
import io.harness.scheduler.PersistentScheduler;

import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.InstanceStatsCollectorJob;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

/**
 * @author rktummala on 10/08/2018
 */
@Slf4j
@TargetModule(Module._390_DB_MIGRATION)
public class AddInstanceStatsCollectionJobToAllAccounts implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject @Named("BackgroundJobScheduler") private transient PersistentScheduler jobScheduler;

  @Override
  public void migrate() {
    Query<Account> query = wingsPersistence.createQuery(Account.class);

    try (HIterator<Account> records = new HIterator<>(query.fetch())) {
      for (Account account : records) {
        InstanceStatsCollectorJob.delete(jobScheduler, account.getUuid());
        InstanceStatsCollectorJob.add(jobScheduler, account.getUuid());
        log.info("Added InstanceStatsCollectorJob for account {}", account.getUuid());
      }
    }
  }
}
