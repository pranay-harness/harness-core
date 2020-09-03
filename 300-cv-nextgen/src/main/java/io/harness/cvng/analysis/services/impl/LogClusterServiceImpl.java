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
import io.harness.cvng.analysis.entities.ServiceGuardLogClusterTask;
import io.harness.cvng.analysis.exceptions.ServiceGuardAnalysisException;
import io.harness.cvng.analysis.services.api.LearningEngineTaskService;
import io.harness.cvng.analysis.services.api.LogClusterService;
import io.harness.cvng.core.entities.LogRecord;
import io.harness.cvng.core.entities.LogRecord.LogRecordKeys;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class LogClusterServiceImpl implements LogClusterService {
  @Inject LearningEngineTaskService learningEngineTaskService;
  @Inject HPersistence hPersistence;

  @Override
  public List<String> scheduleClusteringTasks(AnalysisInput input, LogClusterLevel clusterLevel) {
    List<LearningEngineTask> clusterTasks = new ArrayList<>();
    List<String> taskIds = new ArrayList<>();
    switch (clusterLevel) {
      case L1:
        clusterTasks.addAll(buildClusterTasksForLogL1Clustering(input));
        break;
      case L2:
        ServiceGuardLogClusterTask task = buildClusterTasksForLogL2Clustering(input);
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
  public Map<String, ExecutionStatus> getTaskStatus(String cvConfigId, Set<String> taskIds) {
    return learningEngineTaskService.getTaskStatus(taskIds);
  }

  private List<ServiceGuardLogClusterTask> buildClusterTasksForLogL1Clustering(AnalysisInput input) {
    List<ServiceGuardLogClusterTask> clusterTasks = new ArrayList<>();
    Instant timestamp = input.getStartTime().truncatedTo(ChronoUnit.SECONDS);
    while (timestamp.isBefore(input.getEndTime().truncatedTo(ChronoUnit.SECONDS))) {
      clusterTasks.add(createLogClusterTaskForMinute(input.getCvConfigId(), timestamp, LogClusterLevel.L1));
      timestamp = timestamp.plus(1, ChronoUnit.MINUTES);
    }
    return clusterTasks;
  }

  private ServiceGuardLogClusterTask buildClusterTasksForLogL2Clustering(AnalysisInput input) {
    Instant timeForL2Task = input.getEndTime().truncatedTo(ChronoUnit.SECONDS).minus(1, ChronoUnit.MINUTES);
    List<LogClusterDTO> clusterLogs =
        getClusteredLogData(input.getCvConfigId(), input.getStartTime(), input.getEndTime(), LogClusterLevel.L1);
    if (isEmpty(clusterLogs)) {
      return null;
    }
    return createTaskPojo(input.getCvConfigId(), timeForL2Task, LogClusterLevel.L2);
  }

  private ServiceGuardLogClusterTask createLogClusterTaskForMinute(
      String cvConfigId, Instant timestamp, LogClusterLevel clusterLevel) {
    List<LogRecord> logRecords = hPersistence.createQuery(LogRecord.class, excludeAuthority)
                                     .filter(LogRecordKeys.cvConfigId, cvConfigId)
                                     .field(LogRecordKeys.timestamp)
                                     .greaterThanOrEq(timestamp)
                                     .field(LogRecordKeys.timestamp)
                                     .lessThan(timestamp.plus(1, ChronoUnit.MINUTES))
                                     .asList();
    if (logRecords != null) {
      return createTaskPojo(cvConfigId, timestamp, clusterLevel);
    }
    return null;
  }

  private ServiceGuardLogClusterTask createTaskPojo(
      String cvConfigId, Instant timestamp, LogClusterLevel clusterLevel) {
    String taskId = generateUuid();
    ServiceGuardLogClusterTask task =
        ServiceGuardLogClusterTask.builder()
            .clusterLevel(clusterLevel)
            .testDataUrl(buildTestDataUrlForL1Clustering(cvConfigId, timestamp, clusterLevel))
            .build();
    task.setCvConfigId(cvConfigId);
    task.setAnalysisEndEpochMinute(instantToEpochMinute(timestamp));
    task.setUuid(taskId);
    task.setAnalysisType(LearningEngineTaskType.SERVICE_GUARD_LOG_CLUSTER);
    task.setFailureUrl(learningEngineTaskService.createFailureUrl(taskId));
    task.setAnalysisSaveUrl(buildClusterSaveUrl(cvConfigId, timestamp, taskId, clusterLevel));
    return task;
  }

  private String buildTestDataUrlForL1Clustering(String cvConfigId, Instant timestamp, LogClusterLevel clusterLevel) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SERVICE_BASE_URL + "/" + LOG_CLUSTER_RESOURCE + "/serviceguard-test-data");
    uriBuilder.addParameter(ClusteredLogKeys.cvConfigId, cvConfigId);
    uriBuilder.addParameter(ClusteredLogKeys.timestamp, timestamp.toString());
    uriBuilder.addParameter(ClusteredLogKeys.clusterLevel, clusterLevel.name());
    return getUriString(uriBuilder);
  }

  private String buildClusterSaveUrl(
      String cvConfigId, Instant timestamp, String taskId, LogClusterLevel clusterLevel) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SERVICE_BASE_URL + "/" + LOG_CLUSTER_RESOURCE + "/serviceguard-save-clustered-logs");
    uriBuilder.addParameter("taskId", taskId);
    uriBuilder.addParameter(ClusteredLogKeys.cvConfigId, cvConfigId);
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
      String cvConfigId, Instant timestamp, String host, LogClusterLevel clusterLevel) {
    switch (clusterLevel) {
      case L1:
        return getTestDataForL1Clustering(cvConfigId, timestamp);
      case L2:
        return getClusteredLogData(cvConfigId, timestamp.minus(5, ChronoUnit.MINUTES), timestamp, LogClusterLevel.L1);
      default:
        throw new ServiceGuardAnalysisException("Unknown clusterlevel in getDataForLogCluster: " + clusterLevel);
    }
  }

  private List<LogClusterDTO> getTestDataForL1Clustering(String cvConfigId, Instant timestamp) {
    List<LogClusterDTO> clusterData = new ArrayList<>();
    List<LogRecord> logRecords = hPersistence.createQuery(LogRecord.class, excludeAuthority)
                                     .filter(LogRecordKeys.cvConfigId, cvConfigId)
                                     .field(LogRecordKeys.timestamp)
                                     .greaterThanOrEq(timestamp)
                                     .field(LogRecordKeys.timestamp)
                                     .lessThan(timestamp.plus(1, ChronoUnit.MINUTES))
                                     .asList();
    if (logRecords != null) {
      logRecords.forEach(record -> clusterData.add(record.toLogClusterDTO()));
    }
    return clusterData;
  }

  @Override
  public List<LogClusterDTO> getClusteredLogData(
      String cvConfigId, Instant startTime, Instant endTime, LogClusterLevel clusterLevel) {
    List<LogClusterDTO> clusterData = new ArrayList<>();
    List<ClusteredLog> clusteredLogs = hPersistence.createQuery(ClusteredLog.class, excludeAuthority)
                                           .filter(ClusteredLogKeys.cvConfigId, cvConfigId)
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
  public void saveClusteredData(List<LogClusterDTO> logClusterDTOlist, String cvConfigId, Instant timestamp,
      String taskId, LogClusterLevel clusterLevel) {
    List<ClusteredLog> clusteredLogList = new ArrayList<>();
    logClusterDTOlist.forEach(logClusterDTO -> {
      ClusteredLog clusteredLog = logClusterDTO.toClusteredLog();
      clusteredLog.setClusterLevel(clusterLevel);
      clusteredLog.setCvConfigId(cvConfigId);
      clusteredLogList.add(clusteredLog);
    });
    hPersistence.save(clusteredLogList);

    logger.info("Saved {} clustered logs for config {} with clusterLevel {} and timestamp {} ", cvConfigId,
        clusterLevel, timestamp);
    learningEngineTaskService.markCompleted(taskId);
  }
}
