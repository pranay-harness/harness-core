package io.harness.cvng.analysis.services.impl;

import static io.harness.cvng.CVConstants.SERVICE_BASE_URL;
import static io.harness.cvng.analysis.CVAnalysisConstants.LOG_ANALYSIS_RESOURCE;
import static io.harness.cvng.analysis.CVAnalysisConstants.LOG_ANALYSIS_SAVE_PATH;
import static io.harness.cvng.analysis.CVAnalysisConstants.PREVIOUS_LOG_ANALYSIS_PATH;
import static io.harness.cvng.analysis.CVAnalysisConstants.TEST_DATA_PATH;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;

import io.harness.cvng.analysis.beans.ExecutionStatus;
import io.harness.cvng.analysis.beans.LogAnalysisDTO;
import io.harness.cvng.analysis.beans.LogClusterDTO;
import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType;
import io.harness.cvng.analysis.entities.LogAnalysisCluster;
import io.harness.cvng.analysis.entities.LogAnalysisCluster.LogAnalysisClusterKeys;
import io.harness.cvng.analysis.entities.LogAnalysisRecord.LogAnalysisRecordKeys;
import io.harness.cvng.analysis.entities.LogAnalysisResult;
import io.harness.cvng.analysis.entities.ServiceGuardLogAnalysisTask;
import io.harness.cvng.analysis.services.api.LearningEngineTaskService;
import io.harness.cvng.analysis.services.api.LogAnalysisService;
import io.harness.cvng.analysis.services.api.LogClusterService;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.LogCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.core.utils.DateTimeUtils;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Slf4j
public class LogAnalysisServiceImpl implements LogAnalysisService {
  @Inject private HPersistence hPersistence;
  @Inject private LearningEngineTaskService learningEngineTaskService;
  @Inject private LogClusterService logClusterService;
  @Inject private HeatMapService heatMapService;
  @Inject private CVConfigService cvConfigService;
  @Inject private VerificationTaskService verificationTaskService;

  @Override
  public List<String> scheduleLogAnalysisTask(AnalysisInput input) {
    ServiceGuardLogAnalysisTask task = createLogAnalysisTask(input);
    logger.info("Scheduling ServiceGuardLogAnalysisTask {}", task);
    return learningEngineTaskService.createLearningEngineTasks(Arrays.asList(task));
  }

  private ServiceGuardLogAnalysisTask createLogAnalysisTask(AnalysisInput input) {
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
  public List<LogClusterDTO> getTestData(String cvConfigId, Instant analysisStartTime, Instant analysisEndTime) {
    return logClusterService.getClusteredLogData(cvConfigId, analysisStartTime, analysisEndTime, LogClusterLevel.L2);
  }

  @Override
  public List<LogAnalysisCluster> getPreviousAnalysis(
      String cvConfigId, Instant analysisStartTime, Instant analysisEndTime) {
    List<LogAnalysisCluster> analysisClusters = hPersistence.createQuery(LogAnalysisCluster.class)
                                                    .filter(LogAnalysisClusterKeys.cvConfigId, cvConfigId)
                                                    .filter(LogAnalysisClusterKeys.isEvicted, false)
                                                    .project(LogAnalysisClusterKeys.cvConfigId, true)
                                                    .project(LogAnalysisClusterKeys.analysisMinute, true)
                                                    .project(LogAnalysisClusterKeys.label, true)
                                                    .project(LogAnalysisClusterKeys.text, true)
                                                    .project(LogAnalysisClusterKeys.trend, true)
                                                    .asList();
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
    uriBuilder.addParameter(LogAnalysisRecordKeys.cvConfigId, input.getCvConfigId());
    uriBuilder.addParameter(LogAnalysisRecordKeys.analysisStartTime, input.getStartTime().toString());
    uriBuilder.addParameter(LogAnalysisRecordKeys.analysisEndTime, input.getEndTime().toString());
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
}
