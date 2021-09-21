package software.wings.service.impl.instance.instancesyncperpetualtaskstatus;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.infrastructure.instance.InstanceSyncPerpetualTaskStatus;
import software.wings.beans.infrastructure.instance.InstanceSyncPerpetualTaskStatus.InstanceSyncPerpetualTaskStatusKeys;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
@OwnedBy(HarnessTeam.PL)
@TargetModule(HarnessModule._441_CG_INSTANCE_SYNC)
public class InstanceSyncPerpetualTaskStatusServiceImpl implements InstanceSyncPerpetualTaskStatusService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public boolean handlePerpetualTaskFailure(String perpetualTaskId, String errorMessage) {
    InstanceSyncPerpetualTaskStatus status = get(perpetualTaskId);
    if (status != null) {
      if (System.currentTimeMillis() - status.getInitialFailureAt() >= Duration.ofDays(7).toMillis()) {
        delete(perpetualTaskId);
        return true;
      }
    }

    updateSyncFailure(perpetualTaskId, errorMessage);
    return false;
  }

  @Override
  public void updatePerpetualTaskSuccess(String perpetualTaskId) {
    delete(perpetualTaskId);
  }

  private void updateSyncFailure(String perpetualTaskId, String errorMessage) {
    Query<InstanceSyncPerpetualTaskStatus> query = wingsPersistence.createQuery(InstanceSyncPerpetualTaskStatus.class)
                                                       .field(InstanceSyncPerpetualTaskStatusKeys.perpetualTaskId)
                                                       .equal(perpetualTaskId);
    UpdateOperations<InstanceSyncPerpetualTaskStatus> updateOperations =
        wingsPersistence.createUpdateOperations(InstanceSyncPerpetualTaskStatus.class)
            .setOnInsert(InstanceSyncPerpetualTaskStatusKeys.perpetualTaskId, perpetualTaskId)
            .setOnInsert(InstanceSyncPerpetualTaskStatusKeys.initialFailureAt, System.currentTimeMillis())
            .set(InstanceSyncPerpetualTaskStatusKeys.lastFailureReason, errorMessage);
    wingsPersistence.upsert(query, updateOperations, new FindAndModifyOptions().upsert(true).returnNew(true));
  }

  private InstanceSyncPerpetualTaskStatus get(String perpetualTaskId) {
    return wingsPersistence.createQuery(InstanceSyncPerpetualTaskStatus.class)
        .filter(InstanceSyncPerpetualTaskStatusKeys.perpetualTaskId, perpetualTaskId)
        .get();
  }

  private void delete(String perpetualTaskId) {
    Query<InstanceSyncPerpetualTaskStatus> query = wingsPersistence.createQuery(InstanceSyncPerpetualTaskStatus.class)
                                                       .field(InstanceSyncPerpetualTaskStatusKeys.perpetualTaskId)
                                                       .equal(perpetualTaskId);
    wingsPersistence.delete(query);
  }
}
