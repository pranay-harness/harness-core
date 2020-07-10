package io.harness.cvng.core.services.impl;

import static io.harness.cvng.core.services.CVNextGenConstants.DATA_COLLECTION_DELAY;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import io.harness.cvng.beans.DataCollectionTaskDTO;
import io.harness.cvng.beans.DataCollectionTaskDTO.DataCollectionTaskResult;
import io.harness.cvng.beans.ExecutionStatus;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.core.entities.DataCollectionTask.DataCollectionTaskKeys;
import io.harness.cvng.core.entities.MetricCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.utils.DateTimeUtils;
import io.harness.cvng.statemachine.services.intfc.OrchestrationService;
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
  @VisibleForTesting static int MAX_RETRY_COUNT = 10;
  @Inject private HPersistence hPersistence;
  @Inject private Injector injector;
  @Inject private Clock clock;
  @Inject private MetricPackService metricPackService;
  // move this dependency out and use helper method with no exposure to client directly
  @Inject private VerificationManagerService verificationManagerService;
  @Inject private CVConfigService cvConfigService;
  @Inject private OrchestrationService orchestrationService;

  @Override
  public void save(DataCollectionTask dataCollectionTask) {
    hPersistence.save(dataCollectionTask);
  }

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

  @Override
  public Optional<DataCollectionTaskDTO> getNextTaskDTO(String accountId, String cvConfigId) {
    Optional<DataCollectionTask> optionalTask = getNextTask(accountId, cvConfigId);
    if (optionalTask.isPresent()) {
      DataCollectionTask task = optionalTask.get();
      return Optional.of(DataCollectionTaskDTO.builder()
                             .uuid(task.getUuid())
                             .accountId(task.getAccountId())
                             .cvConfigId(task.getCvConfigId())
                             .dataCollectionInfo(task.getDataCollectionInfo())
                             .startTime(task.getStartTime())
                             .endTime(task.getEndTime())
                             .build());
    } else {
      return Optional.empty();
    }
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
    logger.info("Updating status {}", result);
    UpdateOperations<DataCollectionTask> updateOperations =
        hPersistence.createUpdateOperations(DataCollectionTask.class)
            .set(DataCollectionTaskKeys.status, result.getStatus());
    if (result.getStacktrace() != null) {
      updateOperations.set(DataCollectionTaskKeys.exception, result.getException());
      updateOperations.set(DataCollectionTaskKeys.stacktrace, result.getStacktrace());
    }
    Query<DataCollectionTask> query = hPersistence.createQuery(DataCollectionTask.class)
                                          .filter(DataCollectionTaskKeys.uuid, result.getDataCollectionTaskId());
    hPersistence.update(query, updateOperations);
    DataCollectionTask dataCollectionTask = getDataCollectionTask(result.getDataCollectionTaskId());
    if (result.getStatus() == ExecutionStatus.SUCCESS) {
      // TODO: make this an atomic operation
      orchestrationService.queueAnalysis(
          dataCollectionTask.getCvConfigId(), dataCollectionTask.getStartTime(), dataCollectionTask.getEndTime());
      createNextTask(dataCollectionTask);
    } else {
      retry(dataCollectionTask);
    }
  }

  private void retry(DataCollectionTask dataCollectionTask) {
    if (dataCollectionTask.getRetryCount() < MAX_RETRY_COUNT) {
      UpdateOperations<DataCollectionTask> updateOperations =
          hPersistence.createUpdateOperations(DataCollectionTask.class)
              .set(DataCollectionTaskKeys.status, ExecutionStatus.QUEUED)
              .inc(DataCollectionTaskKeys.retryCount);
      Query<DataCollectionTask> query = hPersistence.createQuery(DataCollectionTask.class)
                                            .filter(DataCollectionTaskKeys.uuid, dataCollectionTask.getUuid());
      hPersistence.update(query, updateOperations);
    } else {
      // TODO: handle this logic in a better way and setup alert.
      logger.error("Task retry count exceeded max limit. Not retrying anymore... {}, {}, {}",
          dataCollectionTask.getUuid(), dataCollectionTask.getException(), dataCollectionTask.getStacktrace());
    }
  }

  private void createNextTask(DataCollectionTask prevTask) {
    CVConfig cvConfig = cvConfigService.get(prevTask.getCvConfigId());
    populateMetricPack(cvConfig);
    if (cvConfig == null) {
      // TODO: delete perpetual task. We need a logic to make sure perpetual tasks are always deleted.
      // Not implementing now because this requires more thought
      throw new UnsupportedOperationException("Not implemented yet");
    }
    DataCollectionTask dataCollectionTask = getDataCollectionTask(cvConfig, prevTask.getEndTime().plusMillis(1),
        prevTask.getEndTime().plus(5, ChronoUnit.MINUTES).minusMillis(1));
    save(dataCollectionTask);
  }

  private void populateMetricPack(CVConfig cvConfig) {
    if (cvConfig instanceof MetricCVConfig) {
      // TODO: get rid of this. Adding it to unblock. We need to redesign how are we setting DSL.
      metricPackService.populateDataCollectionDsl(cvConfig.getType(), ((MetricCVConfig) cvConfig).getMetricPack());
      metricPackService.populatePaths(cvConfig.getAccountId(), cvConfig.getProjectIdentifier(), cvConfig.getType(),
          ((MetricCVConfig) cvConfig).getMetricPack());
    }
  }

  @Override
  public String enqueueFirstTask(CVConfig cvConfig) {
    logger.info("Enqueuing cvConfigId for the first time: {}", cvConfig.getUuid());
    populateMetricPack(cvConfig);

    Instant endTimeForCollection = DateTimeUtils.roundDownTo5MinBoundary(clock.instant());
    // setting it to 2:05 min hours for now. This should come from cvConfig
    DataCollectionTask dataCollectionTask = getDataCollectionTask(
        cvConfig, endTimeForCollection.minus(125, ChronoUnit.MINUTES), endTimeForCollection.minusMillis(1));

    save(dataCollectionTask);
    String dataCollectionTaskId = verificationManagerService.createDataCollectionTask(
        cvConfig.getAccountId(), cvConfig.getUuid(), cvConfig.getConnectorId());
    cvConfigService.setCollectionTaskId(cvConfig.getUuid(), dataCollectionTaskId);
    logger.info("Enqueued cvConfigId successfully: {}", cvConfig.getUuid());
    return dataCollectionTaskId;
  }

  private DataCollectionTask getDataCollectionTask(CVConfig cvConfig, Instant startTime, Instant endTime) {
    return DataCollectionTask.builder()
        .accountId(cvConfig.getAccountId())
        .cvConfigId(cvConfig.getUuid())
        .status(ExecutionStatus.QUEUED)
        .validAfter(endTime.toEpochMilli() + DATA_COLLECTION_DELAY.toMillis())
        .startTime(startTime)
        .endTime(endTime)
        .dataCollectionInfo(
            injector.getInstance(Key.get(DataCollectionInfoMapper.class, Names.named(cvConfig.getType().name())))
                .toDataCollectionInfo(cvConfig))
        .build();
  }
}
