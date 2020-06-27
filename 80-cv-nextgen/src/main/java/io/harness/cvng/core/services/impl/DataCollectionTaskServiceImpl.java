package io.harness.cvng.core.services.impl;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.entities.CVConfig;
import io.harness.cvng.core.services.entities.DataCollectionTask;
import io.harness.cvng.core.services.entities.DataCollectionTask.DataCollectionTaskKeys;
import io.harness.cvng.core.services.entities.DataCollectionTask.DataCollectionTaskResult;
import io.harness.cvng.core.services.entities.DataCollectionTask.ExecutionStatus;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
public class DataCollectionTaskServiceImpl implements DataCollectionTaskService {
  @Inject private HPersistence hPersistence;
  @Inject private Injector injector;
  @Inject private Clock clock;
  // move this dependency out and use helper method with no exposure to client directly
  @Inject private VerificationManagerService verificationManagerService;
  @Inject private CVConfigService cvConfigService;

  @Override
  public void save(DataCollectionTask dataCollectionTask) {
    hPersistence.save(dataCollectionTask);
  }

  @Override
  public Optional<DataCollectionTask> getNextTask(String accountId, String cvConfigId) {
    DataCollectionTask task = hPersistence.createQuery(DataCollectionTask.class)
                                  .filter(DataCollectionTaskKeys.accountId, accountId)
                                  .filter(DataCollectionTaskKeys.cvConfigId, cvConfigId)
                                  .filter(DataCollectionTaskKeys.status, ExecutionStatus.QUEUED)
                                  .filter(DataCollectionTaskKeys.validAfter + " <=", clock.millis())
                                  .order(DataCollectionTaskKeys.lastUpdatedAt)
                                  .get();
    if (task == null) {
      return Optional.empty();
    }
    task.setStatus(ExecutionStatus.RUNNING);
    updateTaskStatus(task.getUuid(), ExecutionStatus.RUNNING);
    return Optional.of(task);
  }

  private void updateTaskStatus(String taskId, ExecutionStatus status) {
    UpdateOperations<DataCollectionTask> updateOperations =
        hPersistence.createUpdateOperations(DataCollectionTask.class).set(DataCollectionTaskKeys.status, status);
    Query<DataCollectionTask> query =
        hPersistence.createQuery(DataCollectionTask.class).filter(DataCollectionTaskKeys.uuid, taskId);
    hPersistence.update(query, updateOperations);
  }

  @Override
  public DataCollectionTask getDataCollectionTask(String dataCollectionTaskId) {
    return hPersistence.get(DataCollectionTask.class, dataCollectionTaskId);
  }

  @Override
  public void updateTaskStatus(DataCollectionTaskResult result) {
    UpdateOperations<DataCollectionTask> updateOperations =
        hPersistence.createUpdateOperations(DataCollectionTask.class)
            .set(DataCollectionTaskKeys.status, result.getStatus());
    if (result.getException() != null) {
      updateOperations.set(DataCollectionTaskKeys.exception, result.getException());
    }
    Query<DataCollectionTask> query = hPersistence.createQuery(DataCollectionTask.class)
                                          .filter(DataCollectionTaskKeys.uuid, result.getDataCollectionTaskId());
    hPersistence.update(query, updateOperations);
  }

  @Override
  public String enqueueFirstTask(CVConfig cvConfig) {
    logger.info("Enqueuing cvConfigId for the first time: {}", cvConfig.getUuid());
    Instant now = clock.instant();
    DataCollectionTask dataCollectionTask =
        DataCollectionTask.builder()
            .accountId(cvConfig.getAccountId())
            .cvConfigId(cvConfig.getUuid())
            .status(ExecutionStatus.QUEUED)
            .startTime(now.minus(2, ChronoUnit.HOURS)) // setting it to 2 hours for now. This should come from cvConfig
            .endTime(now)
            .dataCollectionInfo(injector.getInstance(cvConfig.getType().getDataCollectionInfoMapperClass())
                                    .toDataCollectionInfo(cvConfig))
            .build();
    save(dataCollectionTask);
    String dataCollectionTaskId =
        verificationManagerService.createDataCollectionTask(cvConfig.getAccountId(), cvConfig.getUuid());
    cvConfigService.setCollectionTaskId(cvConfig.getUuid(), dataCollectionTaskId);
    logger.info("Enqueued cvConfigId successfully: {}", cvConfig.getUuid());
    return dataCollectionTaskId;
  }
}
