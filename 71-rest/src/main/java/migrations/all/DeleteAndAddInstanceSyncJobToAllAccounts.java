package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.persistence.HIterator;
import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Application;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.InstanceSyncJob;

/**
 * @author rktummala on 02/9/2019
 */
@Slf4j
public class DeleteAndAddInstanceSyncJobToAllAccounts implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject @Named("BackgroundJobScheduler") private transient PersistentScheduler jobScheduler;

  @Override
  public void migrate() {
    Query<Application> query = wingsPersistence.createQuery(Application.class, excludeAuthority);

    try (HIterator<Application> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        Application application = records.next();
        InstanceSyncJob.delete(jobScheduler, application.getUuid());
        InstanceSyncJob.add(jobScheduler, application.getAccountId(), application.getUuid());
        logger.info("Added InstanceSyncJob for app {}", application.getUuid());
      }
    }
  }
}
