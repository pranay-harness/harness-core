package io.harness.cvng.analysis.services.impl;

import static io.harness.cvng.CVConstants.MONGO_QUERY_TIMEOUT_SEC;
import static io.harness.cvng.CVConstants.SERVICE_BASE_URL;
import static io.harness.cvng.analysis.CVAnalysisConstants.DEPLOYMENT_LOG_ANALYSIS_SAVE_PATH;
import static io.harness.cvng.analysis.CVAnalysisConstants.LOG_ANALYSIS_RESOURCE;
import static io.harness.cvng.analysis.CVAnalysisConstants.LOG_ANALYSIS_SAVE_PATH;
import static io.harness.cvng.analysis.CVAnalysisConstants.PREVIOUS_LOG_ANALYSIS_PATH;
import static io.harness.cvng.analysis.CVAnalysisConstants.TEST_DATA_PATH;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO;
import io.harness.cvng.analysis.beans.LogAnalysisDTO;
import io.harness.cvng.analysis.beans.LogClusterDTO;
import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.analysis.entities.DeploymentLogAnalysis;
import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.entities.LearningEngineTask.ExecutionStatus;
import io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType;
import io.harness.cvng.analysis.entities.LogAnalysisCluster;
import io.harness.cvng.analysis.entities.LogAnalysisCluster.LogAnalysisClusterKeys;
import io.harness.cvng.analysis.entities.LogAnalysisRecord.LogAnalysisRecordKeys;
import io.harness.cvng.analysis.entities.LogAnalysisResult;
import io.harness.cvng.analysis.entities.LogAnalysisResult.AnalysisResult.AnalysisResultKeys;
import io.harness.cvng.analysis.entities.LogAnalysisResult.LogAnalysisResultKeys;
import io.harness.cvng.analysis.entities.LogAnalysisResult.LogAnalysisTag;
import io.harness.cvng.analysis.entities.LogCanaryAnalysisLearningEngineTask;
import io.harness.cvng.analysis.entities.ServiceGuardLogAnalysisTask;
import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.cvng.analysis.services.api.LearningEngineTaskService;
import io.harness.cvng.analysis.services.api.LogAnalysisService;
import io.harness.cvng.analysis.services.api.LogClusterService;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.LogCVConfig;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.HostRecordService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.core.utils.DateTimeUtils;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ProgressLog;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.mongodb.morphia.query.FindOptions;

import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LogAnalysisServiceImpl implements LogAnalysisService {
  @Inject private HPersistence hPersistence;
  @Inject private LearningEngineTaskService learningEngineTaskService;
  @Inject private LogClusterService logClusterService;
  @Inject private HeatMapService heatMapService;
  @Inject private CVConfigService cvConfigService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private HostRecordService hostRecordService;
  @Inject private DeploymentLogAnalysisService deploymentLogAnalysisService;

  @Override
  public String scheduleServiceGuardLogAnalysisTask(AnalysisInput input) {
    ServiceGuardLogAnalysisTask task = createServiceGuardLogAnalysisTask(input);
    logger.info("Scheduling ServiceGuardLogAnalysisTask {}", task);
    return learningEngineTaskService.createLearningEngineTask(task);
  }

  private ServiceGuardLogAnalysisTask createServiceGuardLogAnalysisTask(AnalysisInput input) {
    String taskId = generateUuid();
    String cvConfigId = verificationTaskService.getCVConfigId(input.getVerificationTaskId());
    LogCVConfig cvConfig = (LogCVConfig) cvConfigService.get(cvConfigId);
    ServiceGuardLogAnalysisTask task = ServiceGuardLogAnalysisTask.builder()
                                           .frequencyPatternUrl(createFrequencyPatternUrl(input))
                                           .testDataUrl(createTestDataUrl(input))
                                           .build();

    if (input.getEndTime().isAfter(cvConfig.getBaseline().getStartTime())
        && input.getEndTime().isBefore(cvConfig.getBaseline().getEndTime())) {
      task.setBaselineWindow(true);
    }
    task.setAnalysisType(LearningEngineTaskType.SERVICE_GUARD_LOG_ANALYSIS);
    task.setAnalysisStartTime(input.getStartTime());
    task.setCvConfigId(input.getCvConfigId());
    task.setVerificationTaskId(input.getVerificationTaskId());
    task.setFailureUrl(learningEngineTaskService.createFailureUrl(taskId));
    task.setAnalysisEndTime(input.getEndTime());
    task.setAnalysisEndEpochMinute(DateTimeUtils.instantToEpochMinute(input.getEndTime()));
    task.setAnalysisSaveUrl(createAnalysisSaveUrl(input, taskId));
    task.setUuid(taskId);
    return task;
  }

  @Override
  public String scheduleDeploymentLogAnalysisTask(AnalysisInput analysisInput) {
    LogCanaryAnalysisLearningEngineTask task = createDeploymentLogAnalysisTask(analysisInput);
    logger.info("Scheduling LogCanaryAnalysisLearningEngineTask {}", task);
    return learningEngineTaskService.createLearningEngineTask(task);
  }

  private LogCanaryAnalysisLearningEngineTask createDeploymentLogAnalysisTask(AnalysisInput input) {
    String taskId = generateUuid();
    VerificationTask verificationTask = verificationTaskService.get(input.getVerificationTaskId());
    TimeRange preDeploymentTimeRange =
        verificationJobInstanceService.getPreDeploymentTimeRange(verificationTask.getVerificationJobInstanceId());
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationTask.getVerificationJobInstanceId());
    LogCanaryAnalysisLearningEngineTask task =
        LogCanaryAnalysisLearningEngineTask.builder()
            .controlDataUrl(createDeploymentDataUrl(input.getVerificationTaskId(),
                preDeploymentTimeRange.getStartTime(), preDeploymentTimeRange.getEndTime()))
            .testDataUrl(createDeploymentDataUrl(
                input.getVerificationTaskId(), verificationJobInstance.getStartTime(), input.getEndTime()))
            .controlHosts(getControlHosts(input.getVerificationTaskId(), preDeploymentTimeRange))
            .build();
    task.setAnalysisType(LearningEngineTaskType.CANARY_LOG_ANALYSIS);
    task.setAnalysisStartTime(input.getStartTime());
    task.setVerificationTaskId(input.getVerificationTaskId());
    task.setFailureUrl(learningEngineTaskService.createFailureUrl(taskId));
    task.setAnalysisEndTime(input.getEndTime());
    task.setAnalysisEndEpochMinute(DateTimeUtils.instantToEpochMinute(input.getEndTime()));
    task.setAnalysisSaveUrl(createDeploymentAnalysisSaveUrl(taskId));
    task.setUuid(taskId);
    return task;
  }
  private Set<String> getControlHosts(String verificationTaskId, TimeRange preDeploymentTimeRange) {
    return hostRecordService.get(
        verificationTaskId, preDeploymentTimeRange.getStartTime(), preDeploymentTimeRange.getEndTime());
  }
  private String createDeploymentDataUrl(String verificationTaskId, Instant startTime, Instant endTime) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SERVICE_BASE_URL + "/" + LOG_ANALYSIS_RESOURCE + "/" + TEST_DATA_PATH);
    uriBuilder.addParameter("verificationTaskId", verificationTaskId);
    uriBuilder.addParameter(LogAnalysisRecordKeys.analysisStartTime, String.valueOf(startTime.toEpochMilli()));
    uriBuilder.addParameter(LogAnalysisRecordKeys.analysisEndTime, String.valueOf(endTime.toEpochMilli()));
    return getUriString(uriBuilder);
  }

  @Override
  public List<LogClusterDTO> getTestData(
      String verificationTaskId, Instant analysisStartTime, Instant analysisEndTime) {
    return logClusterService.getClusteredLogData(
        verificationTaskId, analysisStartTime, analysisEndTime, LogClusterLevel.L2);
  }

  @Override
  public List<LogAnalysisCluster> getPreviousAnalysis(
      String cvConfigId, Instant analysisStartTime, Instant analysisEndTime) {
    List<LogAnalysisCluster> analysisClusters =
        hPersistence.createQuery(LogAnalysisCluster.class)
            .filter(LogAnalysisClusterKeys.cvConfigId, cvConfigId)
            .filter(LogAnalysisClusterKeys.isEvicted, false)
            .project(LogAnalysisClusterKeys.cvConfigId, true)
            .project(LogAnalysisClusterKeys.analysisMinute, true)
            .project(LogAnalysisClusterKeys.label, true)
            .project(LogAnalysisClusterKeys.text, true)
            .project(LogAnalysisClusterKeys.frequencyTrend, true)
            .asList(new FindOptions().maxTime(MONGO_QUERY_TIMEOUT_SEC, TimeUnit.SECONDS));
    if (isNotEmpty(analysisClusters)) {
      return analysisClusters;
    }
    return new ArrayList<>();
  }

  @Override
  public void saveAnalysis(String cvConfigId, String taskId, Instant analysisStartTime, Instant analysisEndTime,
      LogAnalysisDTO analysisBody) {
    logger.info("Saving service guard log analysis for config {} and taskId {}", cvConfigId, taskId);

    List<LogAnalysisCluster> analysisClusters =
        analysisBody.toAnalysisClusters(cvConfigId, analysisStartTime, analysisEndTime);
    LogAnalysisResult analysisResult = analysisBody.toAnalysisResult(cvConfigId, analysisStartTime, analysisEndTime);
    logger.info("Saving {} analyzed log clusters for config {}, start time {} and end time {}", analysisClusters.size(),
        cvConfigId, analysisStartTime, analysisEndTime);
    // first delete the existing records which have not been evicted.
    hPersistence.delete(hPersistence.createQuery(LogAnalysisCluster.class)
                            .filter(LogAnalysisClusterKeys.cvConfigId, cvConfigId)
                            .filter(LogAnalysisClusterKeys.isEvicted, false));
    // next, save the new records.
    hPersistence.save(analysisClusters);

    hPersistence.save(analysisResult);

    learningEngineTaskService.markCompleted(taskId);

    CVConfig cvConfig = cvConfigService.get(cvConfigId);

    heatMapService.updateRiskScore(cvConfig.getAccountId(), cvConfig.getProjectIdentifier(),
        cvConfig.getServiceIdentifier(), cvConfig.getEnvIdentifier(), cvConfig.getCategory(), analysisEndTime,
        analysisBody.getScore());
  }

  @Override
  public void saveAnalysis(String learningEngineTaskId, DeploymentLogAnalysisDTO deploymentLogAnalysisDTO) {
    LearningEngineTask learningEngineTask = learningEngineTaskService.get(learningEngineTaskId);
    Preconditions.checkNotNull(learningEngineTask, "Needs to be a valid LE task.");
    learningEngineTaskService.markCompleted(learningEngineTaskId);

    DeploymentLogAnalysis deploymentLogAnalysis = DeploymentLogAnalysis.builder()
                                                      .startTime(learningEngineTask.getAnalysisStartTime())
                                                      .endTime(learningEngineTask.getAnalysisEndTime())
                                                      .verificationTaskId(learningEngineTask.getVerificationTaskId())
                                                      .hostSummaries(deploymentLogAnalysisDTO.getHostSummaries())
                                                      .resultSummary(deploymentLogAnalysisDTO.getResultSummary())
                                                      .clusters(deploymentLogAnalysisDTO.getClusters())
                                                      .build();
    deploymentLogAnalysisService.save(deploymentLogAnalysis);
  }

  @Override
  public void logDeploymentVerificationProgress(AnalysisInput inputs, AnalysisStatus finalStatus) {
    ProgressLog progressLog =
        ProgressLog.builder()
            .startTime(inputs.getStartTime())
            .endTime(inputs.getEndTime())
            .analysisStatus(finalStatus)
            .isFinalState(
                true) // TODO: analysis is the final state now. We need to change it once feedback is implemented
            .log("Log analysis")
            .build();
    verificationJobInstanceService.logProgress(
        verificationTaskService.getVerificationJobInstanceId(inputs.getVerificationTaskId()), progressLog);
  }

  private String createDeploymentAnalysisSaveUrl(String taskId) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SERVICE_BASE_URL + "/" + LOG_ANALYSIS_RESOURCE + "/" + DEPLOYMENT_LOG_ANALYSIS_SAVE_PATH);
    uriBuilder.addParameter("taskId", taskId);
    return getUriString(uriBuilder);
  }

  private String createAnalysisSaveUrl(AnalysisInput input, String taskId) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SERVICE_BASE_URL + "/" + LOG_ANALYSIS_RESOURCE + "/" + LOG_ANALYSIS_SAVE_PATH);
    uriBuilder.addParameter("taskId", taskId);
    uriBuilder.addParameter(LogAnalysisRecordKeys.cvConfigId, input.getCvConfigId());
    uriBuilder.addParameter(LogAnalysisRecordKeys.analysisStartTime, input.getStartTime().toString());
    uriBuilder.addParameter(LogAnalysisRecordKeys.analysisEndTime, input.getEndTime().toString());
    return getUriString(uriBuilder);
  }

  private String createFrequencyPatternUrl(AnalysisInput input) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SERVICE_BASE_URL + "/" + LOG_ANALYSIS_RESOURCE + "/" + PREVIOUS_LOG_ANALYSIS_PATH);
    uriBuilder.addParameter(LogAnalysisRecordKeys.cvConfigId, input.getCvConfigId());
    uriBuilder.addParameter(
        LogAnalysisRecordKeys.analysisStartTime, input.getStartTime().minus(5, ChronoUnit.MINUTES).toString());
    uriBuilder.addParameter(
        LogAnalysisRecordKeys.analysisEndTime, input.getEndTime().minus(5, ChronoUnit.MINUTES).toString());
    return getUriString(uriBuilder);
  }

  private String createTestDataUrl(AnalysisInput input) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SERVICE_BASE_URL + "/" + LOG_ANALYSIS_RESOURCE + "/" + TEST_DATA_PATH);
    uriBuilder.addParameter("verificationTaskId", input.getVerificationTaskId());
    uriBuilder.addParameter(
        LogAnalysisRecordKeys.analysisStartTime, String.valueOf(input.getStartTime().toEpochMilli()));
    uriBuilder.addParameter(LogAnalysisRecordKeys.analysisEndTime, String.valueOf(input.getEndTime().toEpochMilli()));
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
  public Map<String, ExecutionStatus> getTaskStatus(List<String> taskIds) {
    return learningEngineTaskService.getTaskStatus(new HashSet<>(taskIds));
  }

  @Override
  public List<LogAnalysisCluster> getAnalysisClusters(String cvConfigId, Set<Long> labels) {
    return hPersistence.createQuery(LogAnalysisCluster.class, excludeAuthority)
        .filter(LogAnalysisClusterKeys.cvConfigId, cvConfigId)
        .field(LogAnalysisClusterKeys.label)
        .in(labels)
        .asList(new FindOptions().maxTime(MONGO_QUERY_TIMEOUT_SEC, TimeUnit.SECONDS));
  }

  @Override
  public List<LogAnalysisResult> getAnalysisResults(
      String cvConfigId, List<LogAnalysisTag> tags, Instant startTime, Instant endTime) {
    return hPersistence.createQuery(LogAnalysisResult.class, excludeAuthority)
        .filter(LogAnalysisResultKeys.cvConfigId, cvConfigId)
        .field(LogAnalysisResultKeys.analysisStartTime)
        .greaterThanOrEq(startTime)
        .field(LogAnalysisResultKeys.analysisEndTime)
        .lessThanOrEq(endTime)
        .field(LogAnalysisResultKeys.logAnalysisResults + "." + AnalysisResultKeys.tag)
        .in(tags)
        .asList(new FindOptions().maxTime(MONGO_QUERY_TIMEOUT_SEC, TimeUnit.SECONDS));
  }
}
