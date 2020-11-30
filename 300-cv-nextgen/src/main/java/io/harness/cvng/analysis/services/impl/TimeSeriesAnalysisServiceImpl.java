package io.harness.cvng.analysis.services.impl;

import static io.harness.cvng.CVConstants.SERVICE_BASE_URL;
import static io.harness.cvng.analysis.CVAnalysisConstants.ANALYSIS_RISK_RESULTS_LIMIT;
import static io.harness.cvng.analysis.CVAnalysisConstants.TIMESERIES_ANALYSIS_RESOURCE;
import static io.harness.cvng.analysis.CVAnalysisConstants.TIMESERIES_SAVE_ANALYSIS_PATH;
import static io.harness.cvng.analysis.CVAnalysisConstants.TIMESERIES_VERIFICATION_TASK_SAVE_ANALYSIS_PATH;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.cvng.analysis.beans.DeploymentTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.ServiceGuardTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.TimeSeriesAnomalies;
import io.harness.cvng.analysis.beans.TimeSeriesRecordDTO;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis;
import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.entities.LearningEngineTask.ExecutionStatus;
import io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType;
import io.harness.cvng.analysis.entities.TimeSeriesAnomalousPatterns;
import io.harness.cvng.analysis.entities.TimeSeriesAnomalousPatterns.TimeSeriesAnomalousPatternsKeys;
import io.harness.cvng.analysis.entities.TimeSeriesCanaryLearningEngineTask;
import io.harness.cvng.analysis.entities.TimeSeriesCanaryLearningEngineTask.DeploymentVerificationTaskInfo;
import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums;
import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums.TimeSeriesCumulativeSumsKeys;
import io.harness.cvng.analysis.entities.TimeSeriesLearningEngineTask;
import io.harness.cvng.analysis.entities.TimeSeriesLoadTestLearningEngineTask;
import io.harness.cvng.analysis.entities.TimeSeriesRiskSummary;
import io.harness.cvng.analysis.entities.TimeSeriesRiskSummary.TimeSeriesRiskSummaryKeys;
import io.harness.cvng.analysis.entities.TimeSeriesRiskSummary.TransactionMetricRisk;
import io.harness.cvng.analysis.entities.TimeSeriesShortTermHistory;
import io.harness.cvng.analysis.entities.TimeSeriesShortTermHistory.TimeSeriesShortTermHistoryKeys;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;
import io.harness.cvng.analysis.services.api.LearningEngineTaskService;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.core.beans.TimeSeriesMetricDefinition;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.TimeSeriesService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.dashboard.entities.Anomaly;
import io.harness.cvng.dashboard.services.api.AnomalyService;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.verificationjob.entities.CanaryVerificationJob;
import io.harness.cvng.verificationjob.entities.TestVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.AnalysisProgressLog;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ProgressLog;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.persistence.HPersistence;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.mongodb.morphia.query.Sort;

@Slf4j
public class TimeSeriesAnalysisServiceImpl implements TimeSeriesAnalysisService {
  @Inject private HPersistence hPersistence;
  @Inject private LearningEngineTaskService learningEngineTaskService;
  @Inject private TimeSeriesService timeSeriesService;
  @Inject private HeatMapService heatMapService;
  @Inject private CVConfigService cvConfigService;
  @Inject private AnomalyService anomalyService;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private DeploymentTimeSeriesAnalysisService deploymentTimeSeriesAnalysisService;

  @Override
  public List<String> scheduleServiceGuardAnalysis(AnalysisInput analysisInput) {
    TimeSeriesLearningEngineTask timeSeriesTask = createTimeSeriesLearningTask(analysisInput);
    learningEngineTaskService.createLearningEngineTasks(Arrays.asList(timeSeriesTask));
    // TODO: find a good way to return all taskIDs
    return Arrays.asList(timeSeriesTask.getUuid());
  }
  @Override
  public List<String> scheduleCanaryVerificationTaskAnalysis(AnalysisInput analysisInput) {
    TimeSeriesCanaryLearningEngineTask timeSeriesTask = createTimeSeriesCanaryLearningEngineTask(analysisInput);
    learningEngineTaskService.createLearningEngineTasks(Arrays.asList(timeSeriesTask));
    // TODO: find a good way to return all taskIDs
    return Arrays.asList(timeSeriesTask.getUuid());
  }

  @Override
  public List<String> scheduleTestVerificationTaskAnalysis(AnalysisInput analysisInput) {
    TimeSeriesLoadTestLearningEngineTask timeSeriesTask = createTimeSeriesLoadTestLearningEngineTask(analysisInput);
    learningEngineTaskService.createLearningEngineTasks(Arrays.asList(timeSeriesTask));
    return Arrays.asList(timeSeriesTask.getUuid());
  }

  @Override
  public void logDeploymentVerificationProgress(AnalysisInput analysisInput, AnalysisStatus analysisStatus) {
    ProgressLog progressLog = AnalysisProgressLog.builder()
                                  .startTime(analysisInput.getStartTime())
                                  .endTime(analysisInput.getEndTime())
                                  .analysisStatus(analysisStatus)
                                  .isFinalState(true)
                                  .log("Time series analysis")
                                  .build();
    verificationJobInstanceService.logProgress(
        verificationTaskService.getVerificationJobInstanceId(analysisInput.getVerificationTaskId()), progressLog);
  }

  private TimeSeriesLoadTestLearningEngineTask createTimeSeriesLoadTestLearningEngineTask(AnalysisInput input) {
    String taskId = generateUuid();
    VerificationJobInstance verificationJobInstance = verificationJobInstanceService.getVerificationJobInstance(
        verificationTaskService.getVerificationJobInstanceId(input.getVerificationTaskId()));
    TestVerificationJob verificationJob = (TestVerificationJob) verificationJobInstance.getResolvedJob();
    Preconditions.checkNotNull(verificationJobInstance, "verificationJobInstance can not be null");
    VerificationJobInstance baseline = null;
    if (verificationJob.getBaselineVerificationJobInstanceId() != null) {
      baseline = verificationJobInstanceService.getVerificationJobInstance(
          verificationJob.getBaselineVerificationJobInstanceId());
    }
    TimeSeriesLoadTestLearningEngineTask timeSeriesLearningEngineTask =
        TimeSeriesLoadTestLearningEngineTask.builder()
            .controlDataUrl(baseline == null ? null : baselineDataUrl(input, verificationJobInstance, baseline))
            .testDataUrl(postDeploymentDataUrl(input, verificationJobInstance))
            .dataLength(
                (int) Duration.between(verificationJobInstance.getStartTime(), input.getStartTime()).toMinutes() + 1)
            .metricTemplateUrl(createMetricTemplateUrl(input))
            .tolerance(verificationJob.getSensitivity().getTolerance())
            .baselineStartTime(baseline == null ? null : baseline.getStartTime().toEpochMilli())
            .build();

    timeSeriesLearningEngineTask.setVerificationTaskId(input.getVerificationTaskId());
    timeSeriesLearningEngineTask.setAnalysisType(LearningEngineTaskType.TIME_SERIES_LOAD_TEST);
    timeSeriesLearningEngineTask.setAnalysisStartTime(input.getStartTime());
    timeSeriesLearningEngineTask.setAnalysisEndTime(input.getEndTime());
    timeSeriesLearningEngineTask.setAnalysisEndEpochMinute(
        TimeUnit.MILLISECONDS.toMinutes(input.getEndTime().toEpochMilli()));
    timeSeriesLearningEngineTask.setAnalysisSaveUrl(createVerificationTaskAnalysisSaveUrl(taskId));
    timeSeriesLearningEngineTask.setFailureUrl(learningEngineTaskService.createFailureUrl(taskId));
    timeSeriesLearningEngineTask.setUuid(taskId);

    return timeSeriesLearningEngineTask;
  }

  private TimeSeriesCanaryLearningEngineTask createTimeSeriesCanaryLearningEngineTask(AnalysisInput input) {
    String taskId = generateUuid();
    VerificationJobInstance verificationJobInstance = verificationJobInstanceService.getVerificationJobInstance(
        verificationTaskService.getVerificationJobInstanceId(input.getVerificationTaskId()));
    CanaryVerificationJob verificationJob = (CanaryVerificationJob) verificationJobInstance.getResolvedJob();
    Preconditions.checkNotNull(verificationJobInstance, "verificationJobInstance can not be null");
    TimeSeriesCanaryLearningEngineTask timeSeriesLearningEngineTask =
        TimeSeriesCanaryLearningEngineTask.builder()
            .preDeploymentDataUrl(preDeploymentDataUrl(input, verificationJobInstance, verificationJob))
            .postDeploymentDataUrl(postDeploymentDataUrl(input, verificationJobInstance))
            .dataLength(
                (int) Duration.between(verificationJobInstance.getStartTime(), input.getStartTime()).toMinutes() + 1)
            .metricTemplateUrl(createMetricTemplateUrl(input))
            .tolerance(verificationJob.getSensitivity().getTolerance())
            .build();

    timeSeriesLearningEngineTask.setVerificationTaskId(input.getVerificationTaskId());
    timeSeriesLearningEngineTask.setAnalysisType(LearningEngineTaskType.TIME_SERIES_CANARY);
    timeSeriesLearningEngineTask.setAnalysisStartTime(input.getStartTime());
    timeSeriesLearningEngineTask.setAnalysisEndTime(input.getEndTime());
    timeSeriesLearningEngineTask.setAnalysisEndEpochMinute(
        TimeUnit.MILLISECONDS.toMinutes(input.getEndTime().toEpochMilli()));
    timeSeriesLearningEngineTask.setAnalysisSaveUrl(createVerificationTaskAnalysisSaveUrl(taskId));
    timeSeriesLearningEngineTask.setFailureUrl(learningEngineTaskService.createFailureUrl(taskId));
    timeSeriesLearningEngineTask.setUuid(taskId);

    DeploymentVerificationTaskInfo deploymentVerificationTaskInfo =
        DeploymentVerificationTaskInfo.builder()
            .deploymentStartTime(verificationJobInstance.getDeploymentStartTime().toEpochMilli())
            .newHostsTrafficSplitPercentage(verificationJobInstance.getNewHostsTrafficSplitPercentage())
            .newVersionHosts(verificationJobInstance.getNewVersionHosts())
            .oldVersionHosts(verificationJobInstance.getOldVersionHosts())
            .build();
    timeSeriesLearningEngineTask.setDeploymentVerificationTaskInfo(deploymentVerificationTaskInfo);

    return timeSeriesLearningEngineTask;
  }

  private String createVerificationTaskAnalysisSaveUrl(String taskId) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(
        SERVICE_BASE_URL + "/" + TIMESERIES_ANALYSIS_RESOURCE + TIMESERIES_VERIFICATION_TASK_SAVE_ANALYSIS_PATH);
    uriBuilder.addParameter("taskId", taskId);
    return getUriString(uriBuilder);
  }

  private TimeSeriesLearningEngineTask createTimeSeriesLearningTask(AnalysisInput input) {
    String taskId = generateUuid();
    int length = (int) Duration
                     .between(input.getStartTime().truncatedTo(ChronoUnit.SECONDS),
                         input.getEndTime().truncatedTo(ChronoUnit.SECONDS))
                     .toMinutes();

    TimeSeriesLearningEngineTask timeSeriesLearningEngineTask =
        TimeSeriesLearningEngineTask.builder()
            .cumulativeSumsUrl(createCumulativeSumsUrl(input))
            .dataLength(length)
            .keyTransactions(null)
            .metricTemplateUrl(createMetricTemplateUrl(input))
            .previousAnalysisUrl(createPreviousAnalysisUrl(input))
            .previousAnomaliesUrl(createAnomaliesUrl(input))
            .testDataUrl(createTestDataUrl(input))
            .build();
    timeSeriesLearningEngineTask.setVerificationTaskId(input.getVerificationTaskId());
    timeSeriesLearningEngineTask.setAnalysisType(LearningEngineTaskType.SERVICE_GUARD_TIME_SERIES);
    timeSeriesLearningEngineTask.setAnalysisStartTime(input.getStartTime());
    timeSeriesLearningEngineTask.setAnalysisEndTime(input.getEndTime());
    timeSeriesLearningEngineTask.setAnalysisEndEpochMinute(
        TimeUnit.MILLISECONDS.toMinutes(input.getEndTime().toEpochMilli()));
    timeSeriesLearningEngineTask.setAnalysisSaveUrl(createAnalysisSaveUrl(taskId));
    timeSeriesLearningEngineTask.setFailureUrl(learningEngineTaskService.createFailureUrl(taskId));
    timeSeriesLearningEngineTask.setUuid(taskId);

    return timeSeriesLearningEngineTask;
  }
  private String createAnalysisSaveUrl(String taskId) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SERVICE_BASE_URL + "/" + TIMESERIES_ANALYSIS_RESOURCE + TIMESERIES_SAVE_ANALYSIS_PATH);
    uriBuilder.addParameter("taskId", taskId);
    return getUriString(uriBuilder);
  }

  private String createPreviousAnalysisUrl(AnalysisInput input) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(
        SERVICE_BASE_URL + "/" + TIMESERIES_ANALYSIS_RESOURCE + "/timeseries-serviceguard-shortterm-history");
    uriBuilder.addParameter("verificationTaskId", input.getVerificationTaskId());
    return getUriString(uriBuilder);
  }

  private String createCumulativeSumsUrl(AnalysisInput input) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(
        SERVICE_BASE_URL + "/" + TIMESERIES_ANALYSIS_RESOURCE + "/timeseries-serviceguard-cumulative-sums");
    uriBuilder.addParameter("verificationTaskId", input.getVerificationTaskId());
    uriBuilder.addParameter("analysisStartTime", input.getStartTime().toString());
    uriBuilder.addParameter("analysisEndTime", input.getEndTime().toString());
    return getUriString(uriBuilder);
  }

  private String baselineDataUrl(AnalysisInput input, VerificationJobInstance verificationJobInstance,
      VerificationJobInstance baselineVerificationJobInstance) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SERVICE_BASE_URL + "/" + TIMESERIES_ANALYSIS_RESOURCE + "/time-series-data");
    String baselineVerificationTaskId =
        verificationTaskService.findBaselineVerificationTaskId(input.getVerificationTaskId(), verificationJobInstance);
    uriBuilder.addParameter("verificationTaskId", baselineVerificationTaskId);
    uriBuilder.addParameter("startTime", Long.toString(baselineVerificationJobInstance.getStartTime().toEpochMilli()));
    uriBuilder.addParameter("endTime", Long.toString(baselineVerificationJobInstance.getEndTime().toEpochMilli()));
    return getUriString(uriBuilder);
  }
  private String postDeploymentDataUrl(AnalysisInput input, VerificationJobInstance verificationJobInstance) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SERVICE_BASE_URL + "/" + TIMESERIES_ANALYSIS_RESOURCE + "/time-series-data");
    uriBuilder.addParameter("verificationTaskId", input.getVerificationTaskId());
    uriBuilder.addParameter("startTime", Long.toString(verificationJobInstance.getStartTime().toEpochMilli()));
    uriBuilder.addParameter("endTime", Long.toString(input.getEndTime().toEpochMilli()));
    return getUriString(uriBuilder);
  }

  private String preDeploymentDataUrl(
      AnalysisInput input, VerificationJobInstance verificationJobInstance, VerificationJob verificationJob) {
    Optional<TimeRange> preDeploymentTimeRange =
        verificationJob.getPreActivityTimeRange(verificationJobInstance.getDeploymentStartTime());
    Preconditions.checkState(preDeploymentTimeRange.isPresent(),
        "Pre-deployment time range is empty for canary analysis task. This should not happen");
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SERVICE_BASE_URL + "/" + TIMESERIES_ANALYSIS_RESOURCE + "/time-series-data");
    uriBuilder.addParameter("verificationTaskId", input.getVerificationTaskId());
    uriBuilder.addParameter("startTime", Long.toString(preDeploymentTimeRange.get().getStartTime().toEpochMilli()));
    uriBuilder.addParameter("endTime", Long.toString(preDeploymentTimeRange.get().getEndTime().toEpochMilli()));
    return getUriString(uriBuilder);
  }

  private String createAnomaliesUrl(AnalysisInput input) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(
        SERVICE_BASE_URL + "/" + TIMESERIES_ANALYSIS_RESOURCE + "/timeseries-serviceguard-previous-anomalies");
    uriBuilder.addParameter("verificationTaskId", input.getVerificationTaskId());
    return getUriString(uriBuilder);
  }

  private String createTestDataUrl(AnalysisInput input) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SERVICE_BASE_URL + "/" + TIMESERIES_ANALYSIS_RESOURCE + "/time-series-data");
    uriBuilder.addParameter("verificationTaskId", input.getVerificationTaskId());
    uriBuilder.addParameter("startTime", Long.toString(input.getStartTime().toEpochMilli()));
    uriBuilder.addParameter("endTime", Long.toString(input.getEndTime().toEpochMilli()));
    return getUriString(uriBuilder);
  }

  private String createMetricTemplateUrl(AnalysisInput input) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(
        SERVICE_BASE_URL + "/" + TIMESERIES_ANALYSIS_RESOURCE + "/timeseries-serviceguard-metric-template");
    uriBuilder.addParameter("verificationTaskId", input.getVerificationTaskId());
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
  public Map<String, ExecutionStatus> getTaskStatus(String verificationTaskId, Set<String> taskIds) {
    return learningEngineTaskService.getTaskStatus(taskIds);
  }

  @Override
  public Map<String, Map<String, TimeSeriesCumulativeSums.MetricSum>> getCumulativeSums(
      String verificationTaskId, Instant startTime, Instant endTime) {
    log.info(
        "Fetching cumulative sums for config: {}, startTime: {}, endTime: {}", verificationTaskId, startTime, endTime);
    TimeSeriesCumulativeSums cumulativeSums =
        hPersistence.createQuery(TimeSeriesCumulativeSums.class)
            .filter(TimeSeriesCumulativeSumsKeys.verificationTaskId, verificationTaskId)
            .filter(TimeSeriesCumulativeSumsKeys.analysisStartTime, startTime)
            .filter(TimeSeriesCumulativeSumsKeys.analysisEndTime, endTime)
            .get();

    if (cumulativeSums != null) {
      return cumulativeSums.convertToMap();
    }
    return Collections.emptyMap();
  }

  @Override
  public Map<String, Map<String, List<TimeSeriesAnomalies>>> getLongTermAnomalies(String verificationTaskId) {
    log.info("Fetching longterm anomalies for config: {}", verificationTaskId);
    TimeSeriesAnomalousPatterns anomalousPatterns =
        hPersistence.createQuery(TimeSeriesAnomalousPatterns.class)
            .filter(TimeSeriesAnomalousPatternsKeys.verificationTaskId, verificationTaskId)
            .get();
    if (anomalousPatterns != null) {
      return anomalousPatterns.convertToMap();
    }
    return Collections.emptyMap();
  }

  @Override
  public Map<String, Map<String, List<Double>>> getShortTermHistory(String verificationTaskId) {
    log.info("Fetching short term history for config: {}", verificationTaskId);
    TimeSeriesShortTermHistory shortTermHistory =
        hPersistence.createQuery(TimeSeriesShortTermHistory.class)
            .filter(TimeSeriesShortTermHistoryKeys.verificationTaskId, verificationTaskId)
            .get();
    if (shortTermHistory != null) {
      return shortTermHistory.convertToMap();
    }
    return Collections.emptyMap();
  }

  @Override
  public void saveAnalysis(String taskId, DeploymentTimeSeriesAnalysisDTO analysis) {
    LearningEngineTask learningEngineTask = learningEngineTaskService.get(taskId);
    Preconditions.checkNotNull(learningEngineTask, "Needs to be a valid LE task.");
    learningEngineTaskService.markCompleted(taskId);

    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis =
        DeploymentTimeSeriesAnalysis.builder()
            .startTime(learningEngineTask.getAnalysisStartTime())
            .endTime(learningEngineTask.getAnalysisEndTime())
            .verificationTaskId(learningEngineTask.getVerificationTaskId())
            .hostSummaries(analysis.getHostSummaries())
            .transactionMetricSummaries(analysis.getTransactionMetricSummaries())
            .risk(analysis.getRisk())
            .score(analysis.getScore())
            .build();
    deploymentTimeSeriesAnalysisService.save(deploymentTimeSeriesAnalysis);
  }

  @Override
  public List<TransactionMetricRisk> getTopTimeSeriesTransactionMetricRisk(
      List<String> verificationTaskIds, Instant startTime, Instant endTime) {
    List<TimeSeriesRiskSummary> timeSeriesRiskSummaries =
        hPersistence.createQuery(TimeSeriesRiskSummary.class, excludeAuthority)
            .field(TimeSeriesRiskSummaryKeys.verificationTaskId)
            .in(verificationTaskIds)
            .field(TimeSeriesRiskSummaryKeys.analysisEndTime)
            .greaterThanOrEq(startTime)
            .field(TimeSeriesRiskSummaryKeys.analysisEndTime)
            .lessThan(endTime)
            .order(TimeSeriesRiskSummaryKeys.analysisEndTime)
            .asList();
    List<TransactionMetricRisk> transactionMetricRisks =
        timeSeriesRiskSummaries.stream()
            .map(timeSeriesRiskSummary -> timeSeriesRiskSummary.getTransactionMetricRiskList())
            .flatMap(List::stream)
            .collect(Collectors.toList());
    Collections.sort(transactionMetricRisks, Comparator.comparingInt(TransactionMetricRisk::getMetricRisk));
    return transactionMetricRisks.subList(
        Math.max(0, transactionMetricRisks.size() - ANALYSIS_RISK_RESULTS_LIMIT), transactionMetricRisks.size());
  }

  @Override
  public void saveAnalysis(String taskId, ServiceGuardTimeSeriesAnalysisDTO analysis) {
    LearningEngineTask learningEngineTask = learningEngineTaskService.get(taskId);
    TimeSeriesLearningEngineTask task = (TimeSeriesLearningEngineTask) learningEngineTask;
    Preconditions.checkNotNull(learningEngineTask, "Needs to be a valid LE task.");
    analysis.setVerificationTaskId(learningEngineTask.getVerificationTaskId());
    Instant dataStartTime = learningEngineTask.getAnalysisStartTime();
    Instant endTime = learningEngineTask.getAnalysisEndTime();
    Instant startTime = endTime.minus(Duration.ofMinutes(task.getWindowSize()));
    analysis.setAnalysisStartTime(startTime);
    analysis.setAnalysisEndTime(endTime);
    String cvConfigId = verificationTaskService.getCVConfigId(learningEngineTask.getVerificationTaskId());
    TimeSeriesShortTermHistory shortTermHistory = buildShortTermHistory(analysis);
    TimeSeriesCumulativeSums cumulativeSums = buildCumulativeSums(analysis, startTime, endTime);
    TimeSeriesRiskSummary riskSummary = buildRiskSummary(analysis, startTime, endTime);

    saveShortTermHistory(shortTermHistory);
    saveAnomalousPatterns(analysis, learningEngineTask.getVerificationTaskId());
    hPersistence.save(riskSummary);
    hPersistence.save(cumulativeSums);
    log.info("Saving analysis for config: {}", cvConfigId);
    learningEngineTaskService.markCompleted(taskId);
    CVConfig cvConfig = cvConfigService.get(cvConfigId);
    if (cvConfig != null) {
      double risk = getOverallRisk(analysis);
      heatMapService.updateRiskScore(cvConfig.getAccountId(), cvConfig.getOrgIdentifier(),
          cvConfig.getProjectIdentifier(), cvConfig.getServiceIdentifier(), cvConfig.getEnvIdentifier(), cvConfig,
          cvConfig.getCategory(), startTime, risk);

      handleAnomalyOpenOrClose(cvConfig.getAccountId(), cvConfigId, startTime, endTime, risk, riskSummary);
      timeSeriesService.updateRiskScores(cvConfigId, riskSummary);
    }
  }

  private double getOverallRisk(ServiceGuardTimeSeriesAnalysisDTO analysis) {
    return analysis.getOverallMetricScores().values().stream().mapToDouble(score -> score).max().orElse(0.0);
  }

  private void handleAnomalyOpenOrClose(String accountId, String cvConfigId, Instant startTime, Instant endTime,
      double overallRisk, TimeSeriesRiskSummary timeSeriesRiskSummary) {
    if (overallRisk <= 0.25) {
      anomalyService.closeAnomaly(accountId, cvConfigId, endTime);
    } else {
      if (timeSeriesRiskSummary != null) {
        List<TransactionMetricRisk> metricRisks = timeSeriesRiskSummary.getTransactionMetricRiskList();
        List<Anomaly.AnomalousMetric> anomalousMetrics = new ArrayList<>();
        metricRisks.forEach(metricRisk -> {
          if (metricRisk.getMetricRisk() > 0) {
            anomalousMetrics.add(Anomaly.AnomalousMetric.builder()
                                     .groupName(metricRisk.getTransactionName())
                                     .metricName(metricRisk.getMetricName())
                                     .riskScore(metricRisk.getMetricScore())
                                     .build());
          }
        });
        anomalyService.openAnomaly(accountId, cvConfigId, endTime, anomalousMetrics);
      }
    }
  }

  private void saveAnomalousPatterns(ServiceGuardTimeSeriesAnalysisDTO analysis, String verificationTaskId) {
    TimeSeriesAnomalousPatterns patternsToSave = buildAnomalies(analysis);
    // change the filter to verificationTaskId
    TimeSeriesAnomalousPatterns patternsFromDB =
        hPersistence.createQuery(TimeSeriesAnomalousPatterns.class)
            .filter(TimeSeriesAnomalousPatternsKeys.verificationTaskId, verificationTaskId)
            .get();

    if (patternsFromDB != null) {
      patternsToSave.setUuid(patternsFromDB.getUuid());
    }
    hPersistence.save(patternsToSave);
  }

  private void saveShortTermHistory(TimeSeriesShortTermHistory shortTermHistory) {
    TimeSeriesShortTermHistory historyFromDB =
        hPersistence.createQuery(TimeSeriesShortTermHistory.class)
            .filter(TimeSeriesShortTermHistoryKeys.verificationTaskId, shortTermHistory.getVerificationTaskId())
            .get();
    if (historyFromDB != null) {
      shortTermHistory.setUuid(historyFromDB.getUuid());
    }
    hPersistence.save(shortTermHistory);
  }

  @Override
  public List<TimeSeriesMetricDefinition> getMetricTemplate(String verificationTaskId) {
    return timeSeriesService.getTimeSeriesMetricDefinitions(verificationTaskService.getCVConfigId(verificationTaskId));
  }

  @Override
  public List<TimeSeriesRecordDTO> getTimeSeriesRecordDTOs(
      String verificationTaskId, Instant startTime, Instant endTime) {
    return timeSeriesService.getTimeSeriesRecordDTOs(verificationTaskId, startTime, endTime);
  }

  private TimeSeriesRiskSummary buildRiskSummary(
      ServiceGuardTimeSeriesAnalysisDTO analysisDTO, Instant startTime, Instant endTime) {
    List<TransactionMetricRisk> metricRiskList = new ArrayList<>();
    analysisDTO.getTxnMetricAnalysisData().forEach((txnName, metricMap) -> {
      metricMap.forEach((metricName, metricData) -> {
        TransactionMetricRisk metricRisk = TransactionMetricRisk.builder()
                                               .transactionName(txnName)
                                               .metricName(metricName)
                                               .metricRisk(metricData.getRisk())
                                               .metricScore(metricData.getScore())
                                               .lastSeenTime(metricData.getLastSeenTime())
                                               .longTermPattern(metricData.isLongTermPattern())
                                               .build();
        metricRiskList.add(metricRisk);
      });
    });
    return TimeSeriesRiskSummary.builder()
        .verificationTaskId(analysisDTO.getVerificationTaskId())
        .analysisStartTime(startTime)
        .analysisEndTime(endTime)
        .transactionMetricRiskList(metricRiskList)
        .overallRisk(getOverallRisk(analysisDTO))
        .build();
  }
  private TimeSeriesCumulativeSums buildCumulativeSums(
      ServiceGuardTimeSeriesAnalysisDTO analysisDTO, Instant startTime, Instant endTime) {
    Map<String, Map<String, TimeSeriesCumulativeSums.MetricSum>> cumulativeSumsMap = new HashMap<>();
    analysisDTO.getTxnMetricAnalysisData().forEach((txnName, metricMap) -> {
      cumulativeSumsMap.put(txnName, new HashMap<>());
      metricMap.forEach((metricName, metricSums) -> {
        TimeSeriesCumulativeSums.MetricSum sums = metricSums.getCumulativeSums();
        if (sums != null) {
          sums.setMetricName(metricName);
          cumulativeSumsMap.get(txnName).put(metricName, sums);
        }
      });
    });

    List<TimeSeriesCumulativeSums.TransactionMetricSums> transactionMetricSums =
        TimeSeriesCumulativeSums.convertMapToTransactionMetricSums(cumulativeSumsMap);

    return TimeSeriesCumulativeSums.builder()
        .verificationTaskId(analysisDTO.getVerificationTaskId())
        .transactionMetricSums(transactionMetricSums)
        .analysisStartTime(startTime)
        .analysisEndTime(endTime)
        .build();
  }

  private TimeSeriesShortTermHistory buildShortTermHistory(ServiceGuardTimeSeriesAnalysisDTO analysisDTO) {
    Map<String, Map<String, List<Double>>> shortTermHistoryMap = new HashMap<>();
    analysisDTO.getTxnMetricAnalysisData().forEach((txnName, metricMap) -> {
      shortTermHistoryMap.put(txnName, new HashMap<>());
      metricMap.forEach((metricName, txnMetricData) -> {
        shortTermHistoryMap.get(txnName).put(metricName, txnMetricData.getShortTermHistory());
      });
    });

    return TimeSeriesShortTermHistory.builder()
        .verificationTaskId(analysisDTO.getVerificationTaskId())
        .transactionMetricHistories(TimeSeriesShortTermHistory.convertFromMap(shortTermHistoryMap))
        .build();
  }

  private TimeSeriesAnomalousPatterns buildAnomalies(ServiceGuardTimeSeriesAnalysisDTO analysisDTO) {
    Map<String, Map<String, List<TimeSeriesAnomalies>>> anomaliesMap = new HashMap<>();
    analysisDTO.getTxnMetricAnalysisData().forEach((txnName, metricMap) -> {
      anomaliesMap.put(txnName, new HashMap<>());
      metricMap.forEach((metricName, txnMetricData) -> {
        anomaliesMap.get(txnName).put(metricName, txnMetricData.getAnomalousPatterns());
      });
    });

    return TimeSeriesAnomalousPatterns.builder()
        .verificationTaskId(analysisDTO.getVerificationTaskId())
        .anomalies(TimeSeriesAnomalousPatterns.convertFromMap(anomaliesMap))
        .build();
  }

  @Override
  public TimeSeriesRiskSummary getLatestTimeSeriesRiskSummary(
      String verificationTaskId, Instant startTime, Instant endTime) {
    return hPersistence.createQuery(TimeSeriesRiskSummary.class, excludeAuthority)
        .filter(TimeSeriesRiskSummaryKeys.verificationTaskId, verificationTaskId)
        .field(TimeSeriesRiskSummaryKeys.analysisEndTime)
        .greaterThanOrEq(startTime)
        .field(TimeSeriesRiskSummaryKeys.analysisEndTime)
        .lessThanOrEq(endTime)
        .order(Sort.descending(TimeSeriesRiskSummaryKeys.analysisEndTime))
        .get();
  }
}
