package migrations.all;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.persistence.HIterator;
import io.harness.waiter.WaitInstance;
import io.harness.waiter.WaitInstance.WaitInstanceKeys;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;

@Slf4j
public class WaitInstanceMigration implements Migration {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      try (HIterator<WaitInstance> waitInstances =
               new HIterator<>(wingsPersistence.createQuery(WaitInstance.class)
                                   .filter(WaitInstanceKeys.status, ExecutionStatus.NEW)
                                   .fetch())) {
        for (WaitInstance waitInstance : waitInstances) {
          wingsPersistence.update(waitInstance,
              wingsPersistence.createUpdateOperations(WaitInstance.class)
                  .set(WaitInstanceKeys.waitingOnCorrelationIds, waitInstance.getCorrelationIds()));
        }
      }
    } catch (Exception ex) {
      logger.error("Exception:", ex);
    }
  }
}
