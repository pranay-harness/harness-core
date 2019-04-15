package migrations.all;

import static io.harness.mongo.MongoUtils.setUnset;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.time.Timestamp;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.sm.StateExecutionInstance;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FixCVDashboardStatusMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      List<ContinuousVerificationExecutionMetaData> cvMetadataList =
          wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class)
              .filter("executionStatus", "RUNNING")
              .field(ContinuousVerificationExecutionMetaData.CREATED_AT_KEY)
              .greaterThanOrEq(Timestamp.currentMinuteBoundary() - TimeUnit.DAYS.toMillis(30))
              .asList();

      for (ContinuousVerificationExecutionMetaData cvData : cvMetadataList) {
        String stateExecId = cvData.getStateExecutionId();
        StateExecutionInstance instance =
            wingsPersistence.createQuery(StateExecutionInstance.class).filter("_id", stateExecId).get();
        ExecutionStatus realStatus = cvData.getExecutionStatus();
        if (instance != null) {
          realStatus = instance.getStatus();
        }
        UpdateOperations<ContinuousVerificationExecutionMetaData> op =
            wingsPersistence.createUpdateOperations(ContinuousVerificationExecutionMetaData.class);
        setUnset(op, "executionStatus", realStatus);
        wingsPersistence.update(wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class)
                                    .filter("stateExecutionId", stateExecId),
            op);
        logger.info("Updating CVMetadata for {} to {}", stateExecId, realStatus);
      }
    } catch (Exception ex) {
      logger.error("Exception while running FixCVDashboardStatusMigration", ex);
    }
  }
}
