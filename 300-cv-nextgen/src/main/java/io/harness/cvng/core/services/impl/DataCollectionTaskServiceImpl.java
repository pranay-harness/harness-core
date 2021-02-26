package io.harness.cvng.core.services.impl;

import static io.harness.cvng.core.entities.DataCollectionTask.Type.SERVICE_GUARD;
import static io.harness.cvng.core.services.CVNextGenConstants.CVNG_MAX_PARALLEL_THREADS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.cvng.beans.DataCollectionExecutionStatus;
import io.harness.cvng.beans.DataCollectionTaskDTO;
import io.harness.cvng.beans.DataCollectionTaskDTO.DataCollectionTaskResult;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.core.entities.DataCollectionTask.DataCollectionTaskKeys;
import io.harness.cvng.core.entities.DeploymentDataCollectionTask;
import io.harness.cvng.core.entities.MetricCVConfig;
import io.harness.cvng.core.entities.ServiceGuardDataCollectionTask;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.MonitoringSourcePerpetualTaskService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.statemachine.services.intfc.OrchestrationService;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.DataCollectionProgressLog;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.persistence.HPersistence;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

@Slf4j
public class DataCollectionTaskServiceImpl implements DataCollectionTaskService {
  @Inject private HPersistence hPersistence;
  @Inject private Injector injector;
  @Inject private Clock clock;
  @Inject private MetricPackService metricPackService;
  // move this dependency out and use helper method with no exposure to client directly
  @Inject private VerificationManagerService verificationManagerService;
  @Inject private CVConfigService cvConfigService;
  @Inject private OrchestrationService orchestrationService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private MonitoringSourcePerpetualTaskService monitoringSourcePerpetualTaskService;

  // TODO: this is creating reverse dependency. Find a way to get rid of this dependency.
  // Probabally by moving ProgressLog concept to a separate service and model.
  @Inject private VerificationJobInstanceService verificationJobInstanceService;

  @Override
  public void save(DataCollectionTask dataCollectionTask) {
    hPersistence.save(dataCollectionTask);
  }

  public Optional<DataCollectionTask> getNextTask(String accountId, String dataCollectionWorkerId) {
    Query<DataCollectionTask> query = hPersistence.createQuery(DataCollectionTask.class)
                                          .filter(DataCollectionTaskKeys.accountId, accountId)
                                          .filter(DataCollectionTaskKeys.dataCollectionWorkerId, dataCollectionWorkerId)
                                          .field(DataCollectionTaskKeys.validAfter)
                                          .lessThanOrEq(clock.instant())
                                          .order(Sort.ascending(DataCollectionTaskKeys.lastUpdatedAt));
    query.or(query.criteria(DataCollectionTaskKeys.status).equal(DataCollectionExecutionStatus.QUEUED),
        query.and(query.criteria(DataCollectionTaskKeys.status).equal(DataCollectionExecutionStatus.RUNNING),
            query.criteria(DataCollectionTaskKeys.lastUpdatedAt)
                .lessThan(clock.millis() - TimeUnit.MINUTES.toMillis(5))));
    query.or(query.criteria(DataCollectionTaskKeys.type).equal(SERVICE_GUARD),
        query.criteria(DataCollectionTaskKeys.retryCount).lessThanOrEq(DeploymentDataCollectionTask.MAX_RETRY_COUNT));
    UpdateOperations<DataCollectionTask> updateOperations =
        hPersistence.createUpdateOperations(DataCollectionTask.class)
            .set(DataCollectionTaskKeys.status, DataCollectionExecutionStatus.RUNNING)
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
  public List<DataCollectionTaskDTO> getNextTaskDTOs(String accountId, String dataCollectionWorkerId) {
    List<DataCollectionTaskDTO> dataCollectionTasks = new ArrayList<>();
    Optional<DataCollectionTaskDTO> nextTaskDTO;
    do {
      nextTaskDTO = getNextTaskDTO(accountId, dataCollectionWorkerId);
      if (nextTaskDTO.isPresent()) {
        dataCollectionTasks.add(nextTaskDTO.get());
      }
    } while (nextTaskDTO.isPresent() && dataCollectionTasks.size() < CVNG_MAX_PARALLEL_THREADS);
    return dataCollectionTasks;
  }

  @Override
  public DataCollectionTask getDataCollectionTask(String dataCollectionTaskId) {
    return hPersistence.get(DataCollectionTask.class, dataCollectionTaskId);
  }

  @Override
  public void deletePerpetualTasks(String accountId, String perpetualTaskId) {
    verificationManagerService.deletePerpetualTask(accountId, perpetualTaskId);
  }

  @Override
  public void updateTaskStatus(DataCollectionTaskResult result) {
    log.info("Updating status {}", result);
    UpdateOperations<DataCollectionTask> updateOperations =
        hPersistence.createUpdateOperations(DataCollectionTask.class)
            .set(DataCollectionTaskKeys.status, result.getStatus());
    if (result.getStacktrace() != null) {
      updateOperations.set(DataCollectionTaskKeys.exception, result.getException());
      updateOperations.set(DataCollectionTaskKeys.stacktrace, result.getStacktrace());
    }
    Query<DataCollectionTask> query = hPersistence.createQuery(DataCollectionTask.class)
                                          .filter(DataCollectionTaskKeys.uuid, result.getDataCollectionTaskId())
                                          .filter(DataCollectionTaskKeys.status, DataCollectionExecutionStatus.RUNNING);
    UpdateResults updateResults = hPersistence.update(query, updateOperations);
    if (updateResults.getUpdatedCount() == 0) {
      // https://harness.atlassian.net/browse/CVNG-1601
      log.info("Task is not in running state. Skipping the update {}", result);
      return;
    }

    DataCollectionTask dataCollectionTask = getDataCollectionTask(result.getDataCollectionTaskId());
    if (result.getStatus() == DataCollectionExecutionStatus.SUCCESS) {
      // TODO: make this an atomic operation
      if (dataCollectionTask.shouldCreateNextTask()) {
        createNextTask((ServiceGuardDataCollectionTask) dataCollectionTask);
      } else {
        enqueueNextTask(dataCollectionTask);
        if (dataCollectionTask instanceof DeploymentDataCollectionTask) {
          verificationJobInstanceService.logProgress(DataCollectionProgressLog.builder()
                                                         .executionStatus(dataCollectionTask.getStatus())
                                                         .isFinalState(false)
                                                         .startTime(dataCollectionTask.getStartTime())
                                                         .endTime(dataCollectionTask.getEndTime())
                                                         .verificationTaskId(dataCollectionTask.getVerificationTaskId())
                                                         .log("Data collection task successful")
                                                         .build());
        }
      }
      if (dataCollectionTask.shouldQueueAnalysis()) {
        orchestrationService.queueAnalysis(dataCollectionTask.getVerificationTaskId(),
            dataCollectionTask.getStartTime(), dataCollectionTask.getEndTime());
      }
    } else {
      retry(dataCollectionTask);
    }
  }

  @Override
  public void handleRecoverNextTask(CVConfig cvConfig) {
    String serviceGuardVerificationTaskId =
        verificationTaskService.getServiceGuardVerificationTaskId(cvConfig.getAccountId(), cvConfig.getUuid());

    DataCollectionTask dataCollectionTask =
        hPersistence.createQuery(DataCollectionTask.class)
            .filter(DataCollectionTaskKeys.verificationTaskId, serviceGuardVerificationTaskId)
            .order(Sort.descending(DataCollectionTaskKeys.startTime))
            .get();

    Preconditions.checkNotNull(dataCollectionTask, "dataCollectionTask can not be null");
    if (Instant.ofEpochMilli(dataCollectionTask.getLastUpdatedAt())
            .isBefore(clock.instant().minus(Duration.ofMinutes(2)))
        && dataCollectionTask.getStatus().equals(DataCollectionExecutionStatus.SUCCESS)) {
      createNextTask((ServiceGuardDataCollectionTask) dataCollectionTask);
      log.warn(
          "Recovered from next task creation issue. DataCollectionTask uuid: {}, account: {}, projectIdentifier: {}, orgIdentifier: {}, ",
          dataCollectionTask.getUuid(), cvConfig.getAccountId(), cvConfig.getProjectIdentifier(),
          cvConfig.getOrgIdentifier());
    }
  }

  private void markDependentTasksFailed(DataCollectionTask task) {
    if (task instanceof DeploymentDataCollectionTask) {
      verificationJobInstanceService.logProgress(
          DataCollectionProgressLog.builder()
              .executionStatus(task.getStatus())
              .isFinalState(false)
              .startTime(task.getStartTime())
              .endTime(task.getEndTime())
              .verificationTaskId(task.getVerificationTaskId())
              .log("Data collection failed with exception: " + task.getException())
              .build());
    }
    String exceptionMsg =
        task.getStatus() == DataCollectionExecutionStatus.EXPIRED ? "Previous task timed out" : "Previous task failed";
    log.info("Marking queued task failed for verificationTaskId {}", task.getVerificationTaskId());
    UpdateOperations<DataCollectionTask> updateOperations =
        hPersistence.createUpdateOperations(DataCollectionTask.class)
            .set(DataCollectionTaskKeys.status, DataCollectionExecutionStatus.FAILED)
            .set(DataCollectionTaskKeys.exception, exceptionMsg);
    Query<DataCollectionTask> query =
        hPersistence.createQuery(DataCollectionTask.class)
            .filter(DataCollectionTaskKeys.verificationTaskId, task.getVerificationTaskId());
    query.or(query.criteria(DataCollectionTaskKeys.status).equal(DataCollectionExecutionStatus.QUEUED),
        query.criteria(DataCollectionTaskKeys.status).equal(DataCollectionExecutionStatus.WAITING));
    hPersistence.update(query, updateOperations);
  }

  private void enqueueNextTask(DataCollectionTask task) {
    if (task.getNextTaskId() != null) {
      log.info("Enqueuing next task {}", task.getUuid());
      UpdateOperations<DataCollectionTask> updateOperations =
          hPersistence.createUpdateOperations(DataCollectionTask.class)
              .set(DataCollectionTaskKeys.status, DataCollectionExecutionStatus.QUEUED);
      hPersistence.update(
          hPersistence.createQuery(DataCollectionTask.class).filter(DataCollectionTaskKeys.uuid, task.getNextTaskId()),
          updateOperations);
    }
  }

  private void retry(DataCollectionTask dataCollectionTask) {
    if (dataCollectionTask.eligibleForRetry(clock.instant())) {
      UpdateOperations<DataCollectionTask> updateOperations =
          hPersistence.createUpdateOperations(DataCollectionTask.class)
              .set(DataCollectionTaskKeys.status, DataCollectionExecutionStatus.QUEUED)
              .set(DataCollectionTaskKeys.validAfter, dataCollectionTask.getNextValidAfter(clock.instant()))
              .inc(DataCollectionTaskKeys.retryCount);
      Query<DataCollectionTask> query = hPersistence.createQuery(DataCollectionTask.class)
                                            .filter(DataCollectionTaskKeys.uuid, dataCollectionTask.getUuid());
      hPersistence.update(query, updateOperations);
    } else {
      markDependentTasksFailed(dataCollectionTask);
      if (dataCollectionTask.shouldCreateNextTask()) {
        createNextTask((ServiceGuardDataCollectionTask) dataCollectionTask);
      }
      // TODO: handle this logic in a better way and setup alert.
      log.error("Task is in the past. Enqueuing next task with new data collection startTime. {}, {}, {}",
          dataCollectionTask.getUuid(), dataCollectionTask.getException(), dataCollectionTask.getStacktrace());
    }
  }

  private void createNextTask(ServiceGuardDataCollectionTask prevTask) {
    CVConfig cvConfig = cvConfigService.get(verificationTaskService.getCVConfigId(prevTask.getVerificationTaskId()));
    if (cvConfig == null) {
      log.info("CVConfig no longer exists for verificationTaskId {}", prevTask.getVerificationTaskId());
      return;
    }
    populateMetricPack(cvConfig);
    Instant nextTaskStartTime = prevTask.getEndTime();
    Instant currentTime = clock.instant();
    if (nextTaskStartTime.isBefore(prevTask.getDataCollectionPastTimeCutoff(currentTime))) {
      nextTaskStartTime = prevTask.getDataCollectionPastTimeCutoff(currentTime);
      log.info("Restarting Data collection startTime: {}", nextTaskStartTime);
    }
    DataCollectionTask dataCollectionTask =
        getDataCollectionTask(cvConfig, nextTaskStartTime, nextTaskStartTime.plus(5, ChronoUnit.MINUTES));
    if (prevTask.getStatus() != DataCollectionExecutionStatus.SUCCESS) {
      dataCollectionTask.setRetryCount(prevTask.getRetryCount());
      dataCollectionTask.setValidAfter(dataCollectionTask.getNextValidAfter(clock.instant()));
    }
    validateIfAlreadyExists(dataCollectionTask);
    save(dataCollectionTask);
  }

  private void validateIfAlreadyExists(DataCollectionTask dataCollectionTask) {
    if (hPersistence.createQuery(DataCollectionTask.class)
            .filter(DataCollectionTaskKeys.accountId, dataCollectionTask.getAccountId())
            .filter(DataCollectionTaskKeys.verificationTaskId, dataCollectionTask.getVerificationTaskId())
            .filter(DataCollectionTaskKeys.startTime, dataCollectionTask.getStartTime())
            .get()
        != null) {
      log.error(
          "DataCollectionTask with same startTime already exist. This shouldn't be happening. Please check delegate logs. verificationTaskId={}, startTime={}",
          dataCollectionTask.getVerificationTaskId(), dataCollectionTask.getStartTime());
      throw new IllegalStateException(
          "DataCollectionTask with same startTime already exist. This shouldn't be happening. Please check delegate logs");
    }
  }

  private void populateMetricPack(CVConfig cvConfig) {
    if (cvConfig instanceof MetricCVConfig) {
      // TODO: get rid of this. Adding it to unblock. We need to redesign how are we setting DSL.
      metricPackService.populateDataCollectionDsl(cvConfig.getType(), ((MetricCVConfig) cvConfig).getMetricPack());
      metricPackService.populatePaths(cvConfig.getAccountId(), cvConfig.getOrgIdentifier(),
          cvConfig.getProjectIdentifier(), cvConfig.getType(), ((MetricCVConfig) cvConfig).getMetricPack());
    }
  }

  @Override
  public void enqueueFirstTask(CVConfig cvConfig) {
    log.info("Enqueuing cvConfigId for the first time: {}", cvConfig.getUuid());
    populateMetricPack(cvConfig);
    TimeRange dataCollectionRange = cvConfig.getFirstTimeDataCollectionTimeRange();
    DataCollectionTask dataCollectionTask =
        getDataCollectionTask(cvConfig, dataCollectionRange.getStartTime(), dataCollectionRange.getEndTime());

    save(dataCollectionTask);
    cvConfigService.markFirstTaskCollected(cvConfig);
    log.info("Enqueued cvConfigId successfully: {}", cvConfig.getUuid());
  }

  @Override
  public List<String> createSeqTasks(List<DataCollectionTask> dataCollectionTasks) {
    DataCollectionTask lastTask = null;
    for (DataCollectionTask task : dataCollectionTasks) {
      task.setStatus(DataCollectionExecutionStatus.WAITING);
      task.setUuid(generateUuid());
      if (lastTask != null) {
        lastTask.setNextTaskId(task.getUuid());
      }
      lastTask = task;
    }
    if (dataCollectionTasks.size() > 0) {
      dataCollectionTasks.get(0).setStatus(DataCollectionExecutionStatus.QUEUED);
    }
    return hPersistence.save(dataCollectionTasks);
  }

  private DataCollectionTask getDataCollectionTask(CVConfig cvConfig, Instant startTime, Instant endTime) {
    String dataCollectionWorkerId = monitoringSourcePerpetualTaskService.getLiveMonitoringWorkerId(
        cvConfig.getAccountId(), cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier(),
        cvConfig.getConnectorIdentifier(), cvConfig.getIdentifier());
    return ServiceGuardDataCollectionTask.builder()
        .accountId(cvConfig.getAccountId())
        .type(SERVICE_GUARD)
        .dataCollectionWorkerId(dataCollectionWorkerId)
        .status(DataCollectionExecutionStatus.QUEUED)
        .startTime(startTime)
        .endTime(endTime)
        .verificationTaskId(
            verificationTaskService.getServiceGuardVerificationTaskId(cvConfig.getAccountId(), cvConfig.getUuid()))
        .dataCollectionInfo(
            injector.getInstance(Key.get(DataCollectionInfoMapper.class, Names.named(cvConfig.getType().name())))
                .toDataCollectionInfo(cvConfig))
        .build();
  }
}
