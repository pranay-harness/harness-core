package io.harness.cvng.core.services.impl;

import static io.harness.cvng.core.services.CVNextGenConstants.DATA_COLLECTION_DELAY;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import io.harness.cvng.beans.DataCollectionTaskDTO;
import io.harness.cvng.beans.DataCollectionTaskDTO.DataCollectionTaskResult;
import io.harness.cvng.beans.ExecutionStatus;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.core.entities.DataCollectionTask.DataCollectionTaskKeys;
import io.harness.cvng.core.entities.MetricCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.statemachine.services.intfc.OrchestrationService;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

  public Optional<DataCollectionTask> getNextTask(String accountId, String dataCollectionWorkerId) {
    Query<DataCollectionTask> query = hPersistence.createQuery(DataCollectionTask.class)
                                          .filter(DataCollectionTaskKeys.accountId, accountId)
                                          .filter(DataCollectionTaskKeys.dataCollectionWorkerId, dataCollectionWorkerId)
                                          .filter(DataCollectionTaskKeys.validAfter + " <=", clock.millis())
                                          .field(DataCollectionTaskKeys.retryCount)
                                          .lessThan(MAX_RETRY_COUNT)
                                          .order(Sort.ascending("lastUpdatedAt"));
    query.or(query.criteria(DataCollectionTaskKeys.status).equal(ExecutionStatus.QUEUED),
        query.and(query.criteria(DataCollectionTaskKeys.status).equal(ExecutionStatus.RUNNING),
            query.criteria(DataCollectionTaskKeys.lastUpdatedAt)
                .lessThan(clock.millis() - TimeUnit.MINUTES.toMillis(5))));
    UpdateOperations<DataCollectionTask> updateOperations =
        hPersistence.createUpdateOperations(DataCollectionTask.class)
            .set(DataCollectionTaskKeys.status, ExecutionStatus.RUNNING)
            .inc(DataCollectionTaskKeys.retryCount)
            .set(DataCollectionTaskKeys.lastUpdatedAt, clock.millis());

    DataCollectionTask task = hPersistence.findAndModify(query, updateOperations, new FindAndModifyOptions());
    return Optional.ofNullable(task);
  }

  @Override
  public Optional<DataCollectionTaskDTO> getNextTaskDTO(String accountId, String dataCollectionWorkerId) {
    Optional<DataCollectionTask> optionalTask = getNextTask(accountId, dataCollectionWorkerId);
    if (optionalTask.isPresent()) {
      DataCollectionTask task = optionalTask.get();
      return Optional.of(DataCollectionTaskDTO.builder()
                             .uuid(task.getUuid())
                             .accountId(task.getAccountId())
                             .cvConfigId(task.getCvConfigId())
                             .verificationTaskId(task.getVerificationTaskId())
                             .dataCollectionInfo(task.getDataCollectionInfo())
                             .startTime(task.getStartTime())
                             .endTime(task.getEndTime())
                             .build());
    } else {
      return Optional.empty();
    }
  }

  @Override
  public DataCollectionTask getDataCollectionTask(String dataCollectionTaskId) {
    return hPersistence.get(DataCollectionTask.class, dataCollectionTaskId);
  }

  @Override
  public void deleteDataCollectionTask(String accountId, String dataCollectionTaskId) {
    verificationManagerService.deleteDataCollectionTask(accountId, dataCollectionTaskId);
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
      if (isServiceGuardTask(dataCollectionTask)) {
        createNextTask(dataCollectionTask);
        orchestrationService.queueAnalysis(
            dataCollectionTask.getCvConfigId(), dataCollectionTask.getStartTime(), dataCollectionTask.getEndTime());
      } else {
        enqueueNextTask(dataCollectionTask);
      }
    } else {
      retry(dataCollectionTask);
    }
  }

  private void markDependentTasksFailed(DataCollectionTask task) {
    String exceptionMsg =
        task.getStatus() == ExecutionStatus.EXPIRED ? "Previous task timed out" : "Previous task failed";
    logger.info("Marking queued task failed for verificationTaskId {}", task.getVerificationTaskId());
    UpdateOperations<DataCollectionTask> updateOperations =
        hPersistence.createUpdateOperations(DataCollectionTask.class)
            .set(DataCollectionTaskKeys.status, ExecutionStatus.FAILED)
            .set(DataCollectionTaskKeys.exception, exceptionMsg);
    Query<DataCollectionTask> query =
        hPersistence.createQuery(DataCollectionTask.class)
            .filter(DataCollectionTaskKeys.verificationTaskId, task.getVerificationTaskId());
    query.or(query.criteria(DataCollectionTaskKeys.status).equal(ExecutionStatus.QUEUED),
        query.criteria(DataCollectionTaskKeys.status).equal(ExecutionStatus.WAITING));
    hPersistence.update(query, updateOperations);
  }

  private void enqueueNextTask(DataCollectionTask task) {
    if (task.getNextTaskId() != null) {
      logger.info("Enqueuing next task {}", task.getUuid());
      UpdateOperations<DataCollectionTask> updateOperations =
          hPersistence.createUpdateOperations(DataCollectionTask.class)
              .set(DataCollectionTaskKeys.status, ExecutionStatus.QUEUED);
      hPersistence.update(
          hPersistence.createQuery(DataCollectionTask.class).filter(DataCollectionTaskKeys.uuid, task.getNextTaskId()),
          updateOperations);
    }
  }

  private boolean isServiceGuardTask(DataCollectionTask dataCollectionTask) {
    return dataCollectionTask.getCvConfigId() != null;
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
      markDependentTasksFailed(dataCollectionTask);
      // TODO: handle this logic in a better way and setup alert.
      logger.error("Task retry count exceeded max limit. Not retrying anymore... {}, {}, {}",
          dataCollectionTask.getUuid(), dataCollectionTask.getException(), dataCollectionTask.getStacktrace());
    }
  }

  private void createNextTask(DataCollectionTask prevTask) {
    CVConfig cvConfig = cvConfigService.get(prevTask.getCvConfigId());
    if (cvConfig == null) {
      logger.info("CVConfig no longer exists {}", prevTask.getCvConfigId());
      return;
    }
    populateMetricPack(cvConfig);
    DataCollectionTask dataCollectionTask =
        getDataCollectionTask(cvConfig, prevTask.getEndTime(), prevTask.getEndTime().plus(5, ChronoUnit.MINUTES));
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

    TimeRange dataCollectionRange = cvConfig.getFirstTimeDataCollectionTimeRange();
    DataCollectionTask dataCollectionTask =
        getDataCollectionTask(cvConfig, dataCollectionRange.getStartTime(), dataCollectionRange.getEndTime());
    dataCollectionTask.setDataCollectionWorkerId(cvConfig.getUuid());
    String dataCollectionTaskId =
        verificationManagerService.createServiceGuardDataCollectionTask(cvConfig.getAccountId(), cvConfig.getUuid(),
            cvConfig.getConnectorId(), dataCollectionTask.getDataCollectionWorkerId());
    save(dataCollectionTask);
    cvConfigService.setCollectionTaskId(cvConfig.getUuid(), dataCollectionTaskId);

    logger.info("Enqueued cvConfigId successfully: {}", cvConfig.getUuid());
    return dataCollectionTaskId;
  }

  @Override
  public List<String> createSeqTasks(List<DataCollectionTask> dataCollectionTasks) {
    DataCollectionTask lastTask = null;
    for (DataCollectionTask task : dataCollectionTasks) {
      task.setStatus(ExecutionStatus.WAITING);
      task.setUuid(generateUuid());
      if (lastTask != null) {
        lastTask.setNextTaskId(task.getUuid());
      }
      lastTask = task;
    }
    if (dataCollectionTasks.size() > 0) {
      dataCollectionTasks.get(0).setStatus(ExecutionStatus.QUEUED);
    }
    return hPersistence.save(dataCollectionTasks);
  }

  private DataCollectionTask getDataCollectionTask(CVConfig cvConfig, Instant startTime, Instant endTime) {
    return DataCollectionTask.builder()
        .accountId(cvConfig.getAccountId())
        .dataCollectionWorkerId(cvConfig.getUuid())
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
