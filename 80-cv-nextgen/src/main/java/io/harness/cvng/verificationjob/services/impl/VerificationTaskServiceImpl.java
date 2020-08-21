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
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.verificationjob.beans.VerificationTaskDTO;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationTask;
import io.harness.cvng.verificationjob.entities.VerificationTask.VerificationJobKeys;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.cvng.verificationjob.services.api.VerificationTaskService;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.UpdateOperations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class VerificationTaskServiceImpl implements VerificationTaskService {
  @Inject private HPersistence hPersistence;
  @Inject private VerificationJobService verificationJobService;
  @Inject private VerificationManagerService verificationManagerService;
  @Inject private CVConfigService cvConfigService;
  @Inject private DataCollectionTaskService dataCollectionTaskService;
  @Inject private Injector injector;
  @Override
  public String create(String accountId, VerificationTaskDTO verificationTaskDTO) {
    VerificationJob verificationJob =
        verificationJobService.getVerificationJob(accountId, verificationTaskDTO.getVerificationJobIdentifier());
    Preconditions.checkNotNull(verificationJob, "No Job exists for verificationJobIdentifier: '%s'",
        verificationTaskDTO.getVerificationJobIdentifier());
    VerificationTask verificationTask =
        VerificationTask.builder()
            .verificationJobId(verificationJob.getUuid())
            .verificationJobIdentifier(verificationTaskDTO.getVerificationJobIdentifier())
            .accountId(accountId)
            .executionStatus(ExecutionStatus.QUEUED)
            .deploymentStartTime(Instant.ofEpochMilli(verificationTaskDTO.getDeploymentStartTimeMs()))
            .build();
    hPersistence.save(verificationTask);
    return verificationTask.getUuid();
  }

  @Override
  public VerificationTaskDTO get(String verificationTaskId) {
    return getVerificationTask(verificationTaskId).toDTO();
  }

  @Override
  public VerificationTask getVerificationTask(String verificationTaskId) {
    return hPersistence.get(VerificationTask.class, verificationTaskId);
  }

  @Override
  public void createDataCollectionTasks(VerificationTask verificationTask) {
    VerificationJob verificationJob = verificationJobService.get(verificationTask.getVerificationJobId());
    Preconditions.checkNotNull(verificationJob);
    List<CVConfig> cvConfigs = cvConfigService.find(verificationJob.getAccountId(), verificationJob.getDataSources());
    Set<String> connectorIds = cvConfigs.stream().map(CVConfig::getConnectorId).collect(Collectors.toSet());
    // TODO: Keeping it one perpetual task per connector. We need to figure this one out based on the next gen
    // connectors.
    List<String> dataCollectionTaskIds = new ArrayList<>();

    // TODO: add org and project identifier in verificationTask
    CVConfig cvConfig = cvConfigs.stream().findFirst().orElse(null);
    connectorIds.forEach(connectorId -> {
      String dataCollectionWorkerId = getDataCollectionWorkerId(verificationTask, connectorId);
      dataCollectionTaskIds.add(verificationManagerService.createDeploymentVerificationDataCollectionTask(
          verificationTask.getAccountId(), verificationTask.getUuid(), connectorId, cvConfig.getOrgIdentifier(),
          cvConfig.getProjectIdentifier(), dataCollectionWorkerId));
    });
    setDataCollectionTaskIds(verificationTask, dataCollectionTaskIds);
    createDataCollectionTasks(verificationTask, verificationJob, cvConfigs);
  }

  private String getDataCollectionWorkerId(VerificationTask verificationTask, String connectorId) {
    return UUID.nameUUIDFromBytes((verificationTask.getUuid() + ":" + connectorId).getBytes(Charsets.UTF_8)).toString();
  }

  private void createDataCollectionTasks(
      VerificationTask verificationTask, VerificationJob verificationJob, List<CVConfig> cvConfigs) {
    List<TimeRange> timeRanges =
        verificationJob.getDataCollectionTimeRanges(roundDownTo1MinBoundary(verificationTask.getDeploymentStartTime()));
    cvConfigs.forEach(cvConfig -> {
      List<DataCollectionTask> dataCollectionTasks = new ArrayList<>();
      timeRanges.forEach(timeRange -> {
        DataCollectionInfo dataCollectionInfo =
            injector.getInstance(Key.get(DataCollectionInfoMapper.class, Names.named(cvConfig.getType().name())))
                .toDataCollectionInfo(cvConfig);
        // TODO: design the mapper better to do this in the mapper. Take care of this in the next PR when defining the
        // DSL and everything for appdynamics.
        dataCollectionInfo.setDataCollectionDsl(cvConfig.getVerificationJobDataCollectionDsl());
        dataCollectionTasks.add(
            DataCollectionTask.builder()
                .verificationTaskId(verificationTask.getUuid())
                .dataCollectionWorkerId(getDataCollectionWorkerId(verificationTask, cvConfig.getConnectorId()))
                .startTime(timeRange.getStartTime())
                .endTime(timeRange.getEndTime())
                .accountId(verificationJob.getAccountId())
                .status(QUEUED)
                .dataCollectionInfo(dataCollectionInfo)
                .build());
      });
      dataCollectionTaskService.createSeqTasks(dataCollectionTasks);
    });
  }

  private void setDataCollectionTaskIds(VerificationTask verificationTask, List<String> dataCollectionTaskIds) {
    UpdateOperations<VerificationTask> verificationTaskUpdateOperations =
        hPersistence.createUpdateOperations(VerificationTask.class);
    verificationTaskUpdateOperations.set(VerificationJobKeys.dataCollectionTaskIds, dataCollectionTaskIds);
    hPersistence.update(verificationTask,
        hPersistence.createUpdateOperations(VerificationTask.class)
            .set(VerificationJobKeys.dataCollectionTaskIds, dataCollectionTaskIds));
  }
}
