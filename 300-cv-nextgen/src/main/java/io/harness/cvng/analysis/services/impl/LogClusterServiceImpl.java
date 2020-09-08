package io.harness.cvng.analysis.services.impl;

import static io.harness.cvng.CVConstants.SERVICE_BASE_URL;
import static io.harness.cvng.analysis.CVAnalysisConstants.LOG_CLUSTER_RESOURCE;
import static io.harness.cvng.core.utils.DateTimeUtils.instantToEpochMinute;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.cvng.analysis.beans.ExecutionStatus;
import io.harness.cvng.analysis.beans.LogClusterDTO;
import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.analysis.entities.ClusteredLog;
import io.harness.cvng.analysis.entities.ClusteredLog.ClusteredLogKeys;
import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType;
import io.harness.cvng.analysis.entities.LogClusterTask;
import io.harness.cvng.analysis.exceptions.ServiceGuardAnalysisException;
import io.harness.cvng.analysis.services.api.LearningEngineTaskService;
import io.harness.cvng.analysis.services.api.LogClusterService;
import io.harness.cvng.core.entities.LogRecord;
import io.harness.cvng.core.services.api.LogRecordService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;
import io.harness.cvng.verificationjob.entities.DeploymentVerificationTask;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.services.api.DeploymentVerificationTaskService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class LogClusterServiceImpl implements LogClusterService {
  @Inject private LearningEngineTaskService learningEngineTaskService;
  @Inject private HPersistence hPersistence;
  @Inject private LogRecordService logRecordService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private DeploymentVerificationTaskService deploymentVerificationTaskService;
  @Inject private VerificationJobService verificationJobService;

  @Override
  public List<String> scheduleClusteringTasks(AnalysisInput input, LogClusterLevel clusterLevel) {
    List<LearningEngineTask> clusterTasks = new ArrayList<>();
    List<String> taskIds = new ArrayList<>();
    switch (clusterLevel) {
      case L1:
        clusterTasks.addAll(buildClusterTasksForLogL1Clustering(input));
        break;
      case L2:
        LogClusterTask task = buildClusterTasksForLogL2Clustering(input);
        if (task != null) {
          clusterTasks.add(task);
        }
        break;
      default:
        throw new AnalysisStateMachineException("Unknown clusterlevel when scheduling tasks: " + clusterLevel);
    }

    if (isNotEmpty(clusterTasks)) {
      learningEngineTaskService.createLearningEngineTasks(clusterTasks);
      clusterTasks.forEach(task -> taskIds.add(task.getUuid()));
    }
    logger.info("Scheduled {} log cluster tasks for input {} and clusterLevel {}", taskIds.size(), input, clusterLevel);
    return taskIds;
  }

  @Override
  public Map<String, ExecutionStatus> getTaskStatus(Set<String> taskIds) {
    return learningEngineTaskService.getTaskStatus(taskIds);
  }

  private List<LogClusterTask> buildClusterTasksForLogL1Clustering(AnalysisInput input) {
    List<LogClusterTask> clusterTasks = new ArrayList<>();
    Instant timestamp = input.getStartTime().truncatedTo(ChronoUnit.SECONDS);
    while (timestamp.isBefore(input.getEndTime().truncatedTo(ChronoUnit.SECONDS))) {
      clusterTasks.add(createLogClusterTaskForMinute(input.getVerificationTaskId(), timestamp, LogClusterLevel.L1));
      timestamp = timestamp.plus(1, ChronoUnit.MINUTES);
    }
    return clusterTasks;
  }

  private LogClusterTask buildClusterTasksForLogL2Clustering(AnalysisInput input) {
    Instant timeForL2Task = input.getEndTime().truncatedTo(ChronoUnit.SECONDS).minus(1, ChronoUnit.MINUTES);
    List<LogClusterDTO> clusterLogs = getClusteredLogData(
        input.getVerificationTaskId(), input.getStartTime(), input.getEndTime(), LogClusterLevel.L1);
    if (isEmpty(clusterLogs)) {
      return null;
    }
    return createTaskPojo(input.getVerificationTaskId(), timeForL2Task, LogClusterLevel.L2);
  }

  private List<LogRecord> getLogRecordsForMinute(String cvConfigId, Instant timestamp) {
    return logRecordService.getLogRecords(cvConfigId, timestamp, timestamp.plus(Duration.ofMinutes(1)));
  }

  private LogClusterTask createLogClusterTaskForMinute(
      String verificationTaskId, Instant timestamp, LogClusterLevel clusterLevel) {
    List<LogRecord> logRecords = getLogRecordsForMinute(verificationTaskId, timestamp);
    if (logRecords != null) {
      return createTaskPojo(verificationTaskId, timestamp, clusterLevel);
    }
    return null;
  }

  private LogClusterTask createTaskPojo(String verificationTaskId, Instant timestamp, LogClusterLevel clusterLevel) {
    String taskId = generateUuid();
    LogClusterTask task = LogClusterTask.builder()
                              .clusterLevel(clusterLevel)
                              .testDataUrl(buildTestDataUrlForL1Clustering(verificationTaskId, timestamp, clusterLevel))
                              .build();
    task.setVerificationTaskId(verificationTaskId);
    task.setAnalysisEndEpochMinute(instantToEpochMinute(timestamp));
    task.setUuid(taskId);
    task.setAnalysisType(LearningEngineTaskType.LOG_CLUSTER);
    task.setFailureUrl(learningEngineTaskService.createFailureUrl(taskId));
    task.setAnalysisSaveUrl(buildClusterSaveUrl(verificationTaskId, timestamp, taskId, clusterLevel));
    return task;
  }

  private String buildTestDataUrlForL1Clustering(
      String verificationTaskId, Instant timestamp, LogClusterLevel clusterLevel) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SERVICE_BASE_URL + "/" + LOG_CLUSTER_RESOURCE + "/test-data");
    uriBuilder.addParameter(ClusteredLogKeys.verificationTaskId, verificationTaskId);
    // TODO: this logic requires refactoring and should come as a parameters from the states.
    if (verificationTaskService.isServiceGuardId(verificationTaskId)) {
      if (clusterLevel == LogClusterLevel.L1) {
        uriBuilder.addParameter("startTime", String.valueOf(timestamp.toEpochMilli()));
        uriBuilder.addParameter("endTime", String.valueOf(timestamp.plus(Duration.ofMinutes(1)).toEpochMilli()));
      } else {
        uriBuilder.addParameter("startTime", String.valueOf(timestamp.minus(Duration.ofMinutes(5)).toEpochMilli()));
        uriBuilder.addParameter("endTime", String.valueOf(timestamp.toEpochMilli()));
      }
    } else {
      if (clusterLevel == LogClusterLevel.L1) {
        uriBuilder.addParameter("startTime", String.valueOf(timestamp.toEpochMilli()));
        uriBuilder.addParameter("endTime", String.valueOf(timestamp.plus(Duration.ofMinutes(1)).toEpochMilli()));
      } else {
        DeploymentVerificationTask deploymentVerificationTask = deploymentVerificationTaskService.getVerificationTask(
            verificationTaskService.getDeploymentVerificationTaskId(verificationTaskId));
        VerificationJob verificationJob = verificationJobService.get(deploymentVerificationTask.getVerificationJobId());
        uriBuilder.addParameter("startTime",
            String.valueOf(
                verificationJob.getPreDeploymentTimeRanges(deploymentVerificationTask.getDeploymentStartTime())
                    .getStartTime()
                    .toEpochMilli()));
        uriBuilder.addParameter("endTime", String.valueOf(timestamp.plus(Duration.ofMinutes(1)).toEpochMilli()));
      }
    }
    uriBuilder.addParameter(ClusteredLogKeys.clusterLevel, clusterLevel.name());
    return getUriString(uriBuilder);
  }

  private String buildClusterSaveUrl(
      String verificationTaskId, Instant timestamp, String taskId, LogClusterLevel clusterLevel) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SERVICE_BASE_URL + "/" + LOG_CLUSTER_RESOURCE + "/save-clustered-logs");
    uriBuilder.addParameter("taskId", taskId);
    uriBuilder.addParameter(ClusteredLogKeys.verificationTaskId, verificationTaskId);
    uriBuilder.addParameter(ClusteredLogKeys.timestamp, timestamp.toString());
    uriBuilder.addParameter(ClusteredLogKeys.clusterLevel, clusterLevel.name());
    return getUriString(uriBuilder);
  }

  private String getUriString(URIBuilder uriBuilder) {
    try {
      return uriBuilder.build().toString();
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public List<LogClusterDTO> getDataForLogCluster(
      String verificationTaskId, Instant startTime, Instant endTime, String host, LogClusterLevel clusterLevel) {
    switch (clusterLevel) {
      case L1:
        return getTestDataForL1Clustering(verificationTaskId, startTime, endTime);
      case L2:
        return getClusteredLogData(verificationTaskId, startTime, endTime, LogClusterLevel.L1);
      default:
        throw new ServiceGuardAnalysisException("Unknown clusterlevel in getDataForLogCluster: " + clusterLevel);
    }
  }

  private List<LogClusterDTO> getTestDataForL1Clustering(
      String verificationTaskId, Instant startTime, Instant endTime) {
    List<LogClusterDTO> clusterData = new ArrayList<>();
    List<LogRecord> logRecords = logRecordService.getLogRecords(verificationTaskId, startTime, endTime);
    if (logRecords != null) {
      logRecords.forEach(record -> clusterData.add(record.toLogClusterDTO()));
    }
    return clusterData;
  }

  @Override
  public List<LogClusterDTO> getClusteredLogData(
      String verificationTaskId, Instant startTime, Instant endTime, LogClusterLevel clusterLevel) {
    List<LogClusterDTO> clusterData = new ArrayList<>();
    List<ClusteredLog> clusteredLogs = hPersistence.createQuery(ClusteredLog.class, excludeAuthority)
                                           .filter(ClusteredLogKeys.verificationTaskId, verificationTaskId)
                                           .filter(ClusteredLogKeys.clusterLevel, clusterLevel)
                                           .field(ClusteredLogKeys.timestamp)
                                           .greaterThanOrEq(startTime)
                                           .field(ClusteredLogKeys.timestamp)
                                           .lessThan(endTime)
                                           .asList();
    if (clusteredLogs == null) {
      return null;
    }
    clusteredLogs.forEach(clusteredLog -> clusterData.add(clusteredLog.toDTO()));
    return clusterData;
  }

  @Override
  public void saveClusteredData(List<LogClusterDTO> logClusterDTOs, String verificationTaskId, Instant timestamp,
      String taskId, LogClusterLevel clusterLevel) {
    List<ClusteredLog> clusteredLogList = new ArrayList<>();
    logClusterDTOs.forEach(logClusterDTO -> {
      ClusteredLog clusteredLog = logClusterDTO.toClusteredLog();
      clusteredLog.setClusterLevel(clusterLevel);
      clusteredLog.setVerificationTaskId(verificationTaskId);
      clusteredLogList.add(clusteredLog);
    });
    hPersistence.save(clusteredLogList);

    logger.info("Saved {} clustered logs for verificationTaskId {} with clusterLevel {} and timestamp {} ",
        verificationTaskId, clusterLevel, timestamp);
    learningEngineTaskService.markCompleted(taskId);
  }
}
