package io.harness.cvng.verificationjob.services.impl;

import static io.harness.cvng.beans.DataCollectionExecutionStatus.QUEUED;
import static io.harness.cvng.core.utils.DateTimeUtils.roundDownTo1MinBoundary;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

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
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.verificationjob.beans.VerificationJobInstanceDTO;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ExecutionStatus;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ProgressLog;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.VerificationJobInstanceKeys;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.UpdateOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class VerificationJobInstanceServiceImpl implements VerificationJobInstanceService {
  @Inject private HPersistence hPersistence;
  @Inject private VerificationJobService verificationJobService;
  @Inject private VerificationManagerService verificationManagerService;
  @Inject private CVConfigService cvConfigService;
  @Inject private DataCollectionTaskService dataCollectionTaskService;
  @Inject private Injector injector;
  @Inject private MetricPackService metricPackService;
  @Inject private VerificationTaskService verificationTaskService;
  @Override
  public String create(String accountId, VerificationJobInstanceDTO verificationJobInstanceDTO) {
    // TODO: Is this API even needed anymore ?
    VerificationJob verificationJob =
        verificationJobService.getVerificationJob(accountId, verificationJobInstanceDTO.getVerificationJobIdentifier());
    Preconditions.checkNotNull(verificationJob, "No Job exists for verificationJobIdentifier: '%s'",
        verificationJobInstanceDTO.getVerificationJobIdentifier());
    VerificationJobInstance verificationJobInstance =
        VerificationJobInstance.builder()
            .verificationJobIdentifier(verificationJobInstanceDTO.getVerificationJobIdentifier())
            .accountId(accountId)
            .executionStatus(ExecutionStatus.QUEUED)
            .deploymentStartTime(verificationJobInstanceDTO.getDeploymentStartTime())
            .startTime(verificationJobInstanceDTO.getVerificationStartTime())
            .dataCollectionDelay(verificationJobInstanceDTO.getDataCollectionDelay())
            .newVersionHosts(verificationJobInstanceDTO.getNewVersionHosts())
            .oldVersionHosts(verificationJobInstanceDTO.getOldVersionHosts())
            .newHostsTrafficSplitPercentage(verificationJobInstanceDTO.getNewHostsTrafficSplitPercentage())
            // TODO: Change these to extract runtime params
            .duration(verificationJob.getDuration())
            .build();
    hPersistence.save(verificationJobInstance);
    return verificationJobInstance.getUuid();
  }

  @Override
  public List<String> create(List<VerificationJobInstance> verificationJobInstances) {
    if (isNotEmpty(verificationJobInstances)) {
      List<String> jobInstanceIds = new ArrayList<>();
      verificationJobInstances.forEach(verificationJobInstance -> {
        String uuid = generateUuid();
        verificationJobInstance.setUuid(uuid);
        jobInstanceIds.add(uuid);
      });
      hPersistence.save(verificationJobInstances);
      return jobInstanceIds;
    }
    return null;
  }

  @Override
  public VerificationJobInstanceDTO get(String verificationJobInstanceId) {
    return getVerificationJobInstance(verificationJobInstanceId).toDTO();
  }

  @Override
  public VerificationJobInstance getVerificationJobInstance(String verificationJobInstanceId) {
    return hPersistence.get(VerificationJobInstance.class, verificationJobInstanceId);
  }

  @Override
  public void createDataCollectionTasks(VerificationJobInstance verificationJobInstance) {
    VerificationJob verificationJob = verificationJobInstance.getResolvedJob();
    Preconditions.checkNotNull(verificationJob);
    List<CVConfig> cvConfigs = cvConfigService.find(verificationJob.getAccountId(), verificationJob.getDataSources());
    Set<String> connectorIds = cvConfigs.stream().map(CVConfig::getConnectorIdentifier).collect(Collectors.toSet());
    // TODO: Keeping it one perpetual task per connector. We need to figure this one out based on the next gen
    // connectors.
    List<String> perpetualTaskIds = new ArrayList<>();

    connectorIds.forEach(connectorId -> {
      String dataCollectionWorkerId = getDataCollectionWorkerId(verificationJobInstance, connectorId);
      perpetualTaskIds.add(verificationManagerService.createDeploymentVerificationPerpetualTask(
          verificationJobInstance.getAccountId(), connectorId, verificationJob.getOrgIdentifier(),
          verificationJob.getProjectIdentifier(), dataCollectionWorkerId));
    });
    setPerpetualTaskIds(verificationJobInstance, perpetualTaskIds);
    createDataCollectionTasks(verificationJobInstance, verificationJob, cvConfigs);
    markRunning(verificationJobInstance.getUuid());
  }

  @Override
  public void logProgress(String verificationJobInstanceId, ProgressLog progressLog) {
    VerificationJobInstance verificationJobInstance = getVerificationJobInstance(verificationJobInstanceId);

    UpdateOperations<VerificationJobInstance> verificationJobInstanceUpdateOperations =
        hPersistence.createUpdateOperations(VerificationJobInstance.class)
            .addToSet(VerificationJobInstanceKeys.progressLogs, progressLog);
    if ((progressLog.getEndTime().equals(verificationJobInstance.getEndTime()) && progressLog.isFinalState())
        || AnalysisStatus.getFailedStatuses().contains(progressLog.getAnalysisStatus())) {
      verificationJobInstanceUpdateOperations.set(VerificationJobInstanceKeys.executionStatus,
          AnalysisStatus.mapToVerificationJobExecutionStatus(progressLog.getAnalysisStatus()));
    }
    UpdateOptions options = new UpdateOptions();
    options.upsert(true);
    hPersistence.getDatastore(VerificationJobInstance.class)
        .update(hPersistence.createQuery(VerificationJobInstance.class)
                    .filter(VerificationJobInstanceKeys.uuid, verificationJobInstanceId),
            verificationJobInstanceUpdateOperations, options);
  }

  @Override
  public void deletePerpetualTasks(VerificationJobInstance entity) {
    verificationManagerService.deletePerpetualTasks(entity.getAccountId(), entity.getPerpetualTaskIds());
    UpdateOperations<VerificationJobInstance> updateOperations =
        hPersistence.createUpdateOperations(VerificationJobInstance.class);
    updateOperations.unset(VerificationJobInstanceKeys.perpetualTaskIds);
    hPersistence.update(entity, updateOperations);
  }

  @Override
  public TimeRange getPreDeploymentTimeRange(String verificationJobInstanceId) {
    VerificationJobInstance verificationJobInstance = getVerificationJobInstance(verificationJobInstanceId);
    VerificationJob verificationJob = verificationJobInstance.getResolvedJob();
    return verificationJob.getPreDeploymentTimeRange(verificationJobInstance.getDeploymentStartTime());
  }

  private String getDataCollectionWorkerId(VerificationJobInstance verificationJobInstance, String connectorId) {
    return UUID.nameUUIDFromBytes((verificationJobInstance.getUuid() + ":" + connectorId).getBytes(Charsets.UTF_8))
        .toString();
  }

  private void createDataCollectionTasks(
      VerificationJobInstance verificationJobInstance, VerificationJob verificationJob, List<CVConfig> cvConfigs) {
    TimeRange preDeploymentTimeRange =
        verificationJob.getPreDeploymentTimeRange(verificationJobInstance.getDeploymentStartTime());
    List<TimeRange> timeRanges =
        verificationJob.getDataCollectionTimeRanges(roundDownTo1MinBoundary(verificationJobInstance.getStartTime()));
    cvConfigs.forEach(cvConfig -> {
      populateMetricPack(cvConfig);
      List<DataCollectionTask> dataCollectionTasks = new ArrayList<>();
      String verificationTaskId = verificationTaskService.create(
          cvConfig.getAccountId(), cvConfig.getUuid(), verificationJobInstance.getUuid());
      DataCollectionInfoMapper dataCollectionInfoMapper =
          injector.getInstance(Key.get(DataCollectionInfoMapper.class, Names.named(cvConfig.getType().name())));

      DataCollectionInfo preDeploymentDataCollectionInfo = dataCollectionInfoMapper.toDataCollectionInfo(cvConfig);
      preDeploymentDataCollectionInfo.setDataCollectionDsl(cvConfig.getVerificationJobDataCollectionDsl());
      preDeploymentDataCollectionInfo.setCollectHostData(true);
      dataCollectionTasks.add(DataCollectionTask.builder()
                                  .verificationTaskId(verificationTaskId)
                                  .dataCollectionWorkerId(getDataCollectionWorkerId(
                                      verificationJobInstance, cvConfig.getConnectorIdentifier()))
                                  .startTime(preDeploymentTimeRange.getStartTime())
                                  .endTime(preDeploymentTimeRange.getEndTime())
                                  .validAfter(preDeploymentTimeRange.getEndTime()
                                                  .plus(verificationJobInstance.getDataCollectionDelay())
                                                  .toEpochMilli())
                                  .accountId(verificationJob.getAccountId())
                                  .status(QUEUED)
                                  .dataCollectionInfo(preDeploymentDataCollectionInfo)
                                  .queueAnalysis(cvConfig.queueAnalysisForPreDeploymentTask())
                                  .build());

      timeRanges.forEach(timeRange -> {
        DataCollectionInfo dataCollectionInfo = dataCollectionInfoMapper.toDataCollectionInfo(cvConfig);
        // TODO: For Now the DSL is same for both. We need to see how this evolves when implementation other provider.
        // Keeping this simple for now.
        dataCollectionInfo.setDataCollectionDsl(cvConfig.getVerificationJobDataCollectionDsl());
        dataCollectionInfo.setCollectHostData(true);
        dataCollectionTasks.add(
            DataCollectionTask.builder()
                .verificationTaskId(verificationTaskId)
                .dataCollectionWorkerId(
                    getDataCollectionWorkerId(verificationJobInstance, cvConfig.getConnectorIdentifier()))
                .startTime(timeRange.getStartTime())
                .endTime(timeRange.getEndTime())
                .validAfter(
                    timeRange.getEndTime().plus(verificationJobInstance.getDataCollectionDelay()).toEpochMilli())
                .accountId(verificationJob.getAccountId())
                .status(QUEUED)
                .dataCollectionInfo(dataCollectionInfo)
                .build());
      });
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

  private void setPerpetualTaskIds(VerificationJobInstance verificationJobInstance, List<String> perpetualTaskIds) {
    UpdateOperations<VerificationJobInstance> verificationTaskUpdateOperations =
        hPersistence.createUpdateOperations(VerificationJobInstance.class);
    verificationTaskUpdateOperations.set(VerificationJobInstanceKeys.perpetualTaskIds, perpetualTaskIds);
    hPersistence.update(verificationJobInstance,
        hPersistence.createUpdateOperations(VerificationJobInstance.class)
            .set(VerificationJobInstanceKeys.perpetualTaskIds, perpetualTaskIds));
  }

  private void markRunning(String verificationTaskId) {
    UpdateOperations<VerificationJobInstance> updateOperations =
        hPersistence.createUpdateOperations(VerificationJobInstance.class)
            .set(VerificationJobInstanceKeys.executionStatus, ExecutionStatus.RUNNING);
    Query<VerificationJobInstance> query = hPersistence.createQuery(VerificationJobInstance.class)
                                               .filter(VerificationJobInstanceKeys.uuid, verificationTaskId);
    hPersistence.update(query, updateOperations);
  }
}
