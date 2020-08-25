package io.harness.cvng.verificationjob.services.impl;

import static io.harness.cvng.beans.ExecutionStatus.QUEUED;
import static io.harness.cvng.core.utils.DateTimeUtils.roundDownTo1MinBoundary;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import io.harness.cvng.analysis.beans.ExecutionStatus;
import io.harness.cvng.beans.DataCollectionInfo;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.core.entities.MetricCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.statemachine.entities.AnalysisStatus;
import io.harness.cvng.verificationjob.beans.DeploymentVerificationTaskDTO;
import io.harness.cvng.verificationjob.entities.DeploymentVerificationTask;
import io.harness.cvng.verificationjob.entities.DeploymentVerificationTask.DeploymentVerificationTaskKeys;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.services.api.DeploymentVerificationTaskService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.UpdateOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class DeploymentVerificationTaskServiceImpl implements DeploymentVerificationTaskService {
  @Inject private HPersistence hPersistence;
  @Inject private VerificationJobService verificationJobService;
  @Inject private VerificationManagerService verificationManagerService;
  @Inject private CVConfigService cvConfigService;
  @Inject private DataCollectionTaskService dataCollectionTaskService;
  @Inject private Injector injector;
  @Inject private MetricPackService metricPackService;
  @Inject private VerificationTaskService verificationTaskService;
  @Override
  public String create(String accountId, DeploymentVerificationTaskDTO deploymentVerificationTaskDTO) {
    VerificationJob verificationJob = verificationJobService.getVerificationJob(
        accountId, deploymentVerificationTaskDTO.getVerificationJobIdentifier());
    Preconditions.checkNotNull(verificationJob, "No Job exists for verificationJobIdentifier: '%s'",
        deploymentVerificationTaskDTO.getVerificationJobIdentifier());
    DeploymentVerificationTask deploymentVerificationTask =
        DeploymentVerificationTask.builder()
            .verificationJobId(verificationJob.getUuid())
            .verificationJobIdentifier(deploymentVerificationTaskDTO.getVerificationJobIdentifier())
            .accountId(accountId)
            .executionStatus(ExecutionStatus.QUEUED)
            .deploymentStartTime(deploymentVerificationTaskDTO.getDeploymentStartTime())
            .startTime(deploymentVerificationTaskDTO.getVerificationStartTime())
            .dataCollectionDelay(deploymentVerificationTaskDTO.getDataCollectionDelay())
            .newVersionHosts(deploymentVerificationTaskDTO.getNewVersionHosts())
            .oldVersionHosts(deploymentVerificationTaskDTO.getOldVersionHosts())
            .newHostsTrafficSplitPercentage(deploymentVerificationTaskDTO.getNewHostsTrafficSplitPercentage())
            .duration(verificationJob.getDuration())
            .build();
    hPersistence.save(deploymentVerificationTask);
    return deploymentVerificationTask.getUuid();
  }

  @Override
  public DeploymentVerificationTaskDTO get(String verificationTaskId) {
    return getVerificationTask(verificationTaskId).toDTO();
  }

  @Override
  public DeploymentVerificationTask getVerificationTask(String verificationTaskId) {
    return hPersistence.get(DeploymentVerificationTask.class, verificationTaskId);
  }

  @Override
  public void createDataCollectionTasks(DeploymentVerificationTask deploymentVerificationTask) {
    VerificationJob verificationJob = verificationJobService.get(deploymentVerificationTask.getVerificationJobId());
    Preconditions.checkNotNull(verificationJob);
    List<CVConfig> cvConfigs = cvConfigService.find(verificationJob.getAccountId(), verificationJob.getDataSources());
    Set<String> connectorIds = cvConfigs.stream().map(CVConfig::getConnectorId).collect(Collectors.toSet());
    // TODO: Keeping it one perpetual task per connector. We need to figure this one out based on the next gen
    // connectors.
    List<String> dataCollectionTaskIds = new ArrayList<>();

    connectorIds.forEach(connectorId -> {
      String dataCollectionWorkerId = getDataCollectionWorkerId(deploymentVerificationTask, connectorId);
      dataCollectionTaskIds.add(verificationManagerService.createDeploymentVerificationDataCollectionTask(
          deploymentVerificationTask.getAccountId(), connectorId, verificationJob.getOrgIdentifier(),
          verificationJob.getProjectIdentifier(), dataCollectionWorkerId));
    });
    setDataCollectionTaskIds(deploymentVerificationTask, dataCollectionTaskIds);
    createDataCollectionTasks(deploymentVerificationTask, verificationJob, cvConfigs);
    markRunning(deploymentVerificationTask.getUuid());
  }

  @Override
  public void logProgress(
      String deploymentVerificationId, Instant startTime, Instant endTime, AnalysisStatus analysisStatus) {
    DeploymentVerificationTask deploymentVerificationTask = getVerificationTask(deploymentVerificationId);
    DeploymentVerificationTask.ProgressLog progressLog = DeploymentVerificationTask.ProgressLog.builder()
                                                             .startTime(startTime)
                                                             .endTime(endTime)
                                                             .analysisStatus(analysisStatus)
                                                             .build();
    UpdateOperations<DeploymentVerificationTask> deploymentVerificationTaskUpdateOperations =
        hPersistence.createUpdateOperations(DeploymentVerificationTask.class)
            .addToSet(DeploymentVerificationTaskKeys.progressLogs, progressLog);
    if (endTime.equals(deploymentVerificationTask.getEndTime())
        || AnalysisStatus.getFailedStatuses().contains(analysisStatus)) {
      deploymentVerificationTaskUpdateOperations.set(
          DeploymentVerificationTaskKeys.executionStatus, mapToExecutionStatus(analysisStatus));
    }
    UpdateOptions options = new UpdateOptions();
    options.upsert(true);
    hPersistence.getDatastore(DeploymentVerificationTask.class)
        .update(hPersistence.createQuery(DeploymentVerificationTask.class)
                    .filter(DeploymentVerificationTaskKeys.uuid, deploymentVerificationId),
            deploymentVerificationTaskUpdateOperations, options);
  }

  private ExecutionStatus mapToExecutionStatus(AnalysisStatus analysisStatus) {
    // TODO: do we need a uniform single status for both of these?
    switch (analysisStatus) {
      case SUCCESS:
        return ExecutionStatus.SUCCESS;
      case FAILED:
        return ExecutionStatus.FAILED;
      case TIMEOUT:
        return ExecutionStatus.TIMEOUT;
      default:
        throw new IllegalStateException("AnalysisStatus " + analysisStatus
            + " should be one of final status. Mapping to executionStatus not defined.");
    }
  }

  private String getDataCollectionWorkerId(DeploymentVerificationTask deploymentVerificationTask, String connectorId) {
    return UUID.nameUUIDFromBytes((deploymentVerificationTask.getUuid() + ":" + connectorId).getBytes(Charsets.UTF_8))
        .toString();
  }

  private void createDataCollectionTasks(DeploymentVerificationTask deploymentVerificationTask,
      VerificationJob verificationJob, List<CVConfig> cvConfigs) {
    List<TimeRange> timeRanges =
        verificationJob.getDataCollectionTimeRanges(roundDownTo1MinBoundary(deploymentVerificationTask.getStartTime()));
    cvConfigs.forEach(cvConfig -> {
      populateMetricPack(cvConfig);
      List<DataCollectionTask> dataCollectionTasks = new ArrayList<>();
      String verificationTaskId = verificationTaskService.create(
          cvConfig.getAccountId(), cvConfig.getUuid(), deploymentVerificationTask.getUuid());
      timeRanges.forEach(timeRange -> {
        DataCollectionInfo dataCollectionInfo =
            injector.getInstance(Key.get(DataCollectionInfoMapper.class, Names.named(cvConfig.getType().name())))
                .toDataCollectionInfo(cvConfig);
        // TODO: For Now the DSL is same for both. We need to see how this evolves when implementation other provider.
        // Keeping this simple for now.
        dataCollectionInfo.setDataCollectionDsl(cvConfig.getVerificationJobDataCollectionDsl());
        dataCollectionInfo.setCollectHostData(true);
        dataCollectionTasks.add(
            DataCollectionTask.builder()
                .verificationTaskId(verificationTaskId)
                .dataCollectionWorkerId(
                    getDataCollectionWorkerId(deploymentVerificationTask, cvConfig.getConnectorId()))
                .startTime(timeRange.getStartTime())
                .endTime(timeRange.getEndTime())
                .validAfter(
                    timeRange.getEndTime().plus(deploymentVerificationTask.getDataCollectionDelay()).toEpochMilli())
                .accountId(verificationJob.getAccountId())
                .status(QUEUED)
                .dataCollectionInfo(dataCollectionInfo)
                .build());
      });
      // TODO: check if getting zero always make sense.
      dataCollectionTasks.get(0).setQueueAnalysis(false);
      dataCollectionTaskService.createSeqTasks(dataCollectionTasks);
    });
  }
  private void populateMetricPack(CVConfig cvConfig) {
    if (cvConfig instanceof MetricCVConfig) {
      // TODO: get rid of this. Adding it to unblock. We need to redesign how are we setting DSL.
      metricPackService.populateDataCollectionDsl(cvConfig.getType(), ((MetricCVConfig) cvConfig).getMetricPack());
      metricPackService.populatePaths(cvConfig.getAccountId(), cvConfig.getProjectIdentifier(), cvConfig.getType(),
          ((MetricCVConfig) cvConfig).getMetricPack());
    }
  }

  private void setDataCollectionTaskIds(
      DeploymentVerificationTask deploymentVerificationTask, List<String> dataCollectionTaskIds) {
    UpdateOperations<DeploymentVerificationTask> verificationTaskUpdateOperations =
        hPersistence.createUpdateOperations(DeploymentVerificationTask.class);
    verificationTaskUpdateOperations.set(DeploymentVerificationTaskKeys.dataCollectionTaskIds, dataCollectionTaskIds);
    hPersistence.update(deploymentVerificationTask,
        hPersistence.createUpdateOperations(DeploymentVerificationTask.class)
            .set(DeploymentVerificationTaskKeys.dataCollectionTaskIds, dataCollectionTaskIds));
  }

  private void markRunning(String verificationTaskId) {
    UpdateOperations<DeploymentVerificationTask> updateOperations =
        hPersistence.createUpdateOperations(DeploymentVerificationTask.class)
            .set(DeploymentVerificationTaskKeys.executionStatus, ExecutionStatus.RUNNING);
    Query<DeploymentVerificationTask> query = hPersistence.createQuery(DeploymentVerificationTask.class)
                                                  .filter(DeploymentVerificationTaskKeys.uuid, verificationTaskId);
    hPersistence.update(query, updateOperations);
  }
}
