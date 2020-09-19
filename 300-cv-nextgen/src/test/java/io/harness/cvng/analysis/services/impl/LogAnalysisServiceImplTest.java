package io.harness.cvng.analysis.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO;
import io.harness.cvng.analysis.beans.LogAnalysisDTO;
import io.harness.cvng.analysis.beans.LogClusterDTO;
import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.analysis.entities.ClusteredLog;
import io.harness.cvng.analysis.entities.DeploymentLogAnalysis;
import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.entities.LearningEngineTask.ExecutionStatus;
import io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType;
import io.harness.cvng.analysis.entities.LogAnalysisCluster;
import io.harness.cvng.analysis.entities.LogAnalysisCluster.LogAnalysisClusterKeys;
import io.harness.cvng.analysis.entities.LogAnalysisResult;
import io.harness.cvng.analysis.entities.LogAnalysisResult.AnalysisResult;
import io.harness.cvng.analysis.entities.LogAnalysisResult.LogAnalysisResultKeys;
import io.harness.cvng.analysis.entities.LogCanaryAnalysisLearningEngineTask;
import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.cvng.analysis.services.api.LearningEngineTaskService;
import io.harness.cvng.analysis.services.api.LogAnalysisService;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class LogAnalysisServiceImplTest extends CvNextGenTest {
  private String cvConfigId;
  private String verificationTaskId;
  @Inject private HPersistence hPersistence;
  @Inject private LearningEngineTaskService learningEngineTaskService;
  @Inject private LogAnalysisService logAnalysisService;
  @Inject private CVConfigService cvConfigService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private DeploymentLogAnalysisService deploymentLogAnalysisService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    CVConfig cvConfig = createCVConfig();
    cvConfigService.save(cvConfig);
    cvConfigId = cvConfig.getUuid();
    verificationTaskId = verificationTaskService.getServiceGuardVerificationTaskId(cvConfig.getAccountId(), cvConfigId);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void scheduleLogAnalysisTask() {
    AnalysisInput input = AnalysisInput.builder()
                              .verificationTaskId(verificationTaskId)
                              .startTime(Instant.now().minus(10, ChronoUnit.MINUTES))
                              .endTime(Instant.now())
                              .build();
    String taskId = logAnalysisService.scheduleServiceGuardLogAnalysisTask(input);

    assertThat(taskId).isNotNull();

    LearningEngineTask task = hPersistence.get(LearningEngineTask.class, taskId);
    assertThat(task).isNotNull();
    assertThat(task.getVerificationTaskId()).isEqualTo(verificationTaskId);
    assertThat(Duration.between(task.getAnalysisStartTime(), input.getStartTime())).isZero();
    assertThat(Duration.between(task.getAnalysisEndTime(), input.getEndTime())).isZero();
    assertThat(task.getAnalysisType().name()).isEqualTo(LearningEngineTaskType.SERVICE_GUARD_LOG_ANALYSIS.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void getTestData() {
    Instant start = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    List<ClusteredLog> l2Logs = createClusteredLogRecords(start, end);
    hPersistence.save(l2Logs);

    List<LogClusterDTO> logClusterDTOList = logAnalysisService.getTestData(verificationTaskId, start, end);

    assertThat(logClusterDTOList).isNotNull();
    assertThat(logClusterDTOList.size()).isEqualTo(l2Logs.size());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void getFrequencyPattern_firstAnalysis() {
    Instant start = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    List<LogAnalysisCluster> patterns = logAnalysisService.getPreviousAnalysis(cvConfigId, start, end);

    assertThat(patterns).isNotNull();
    assertThat(patterns.isEmpty()).isTrue();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetFrequencyPattern_hasPreviousAnalysis() {
    Instant start = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    List<LogAnalysisCluster> analysisClusters = buildAnalysisClusters(12345l);

    analysisClusters.forEach(cluster -> {
      cluster.setCvConfigId(cvConfigId);
      cluster.setAnalysisStartTime(start);
      cluster.setAnalysisEndTime(end);
    });
    hPersistence.save(analysisClusters);

    List<LogAnalysisCluster> patterns = logAnalysisService.getPreviousAnalysis(cvConfigId, start, end);

    assertThat(patterns).isNotNull();
    assertThat(patterns.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testSaveAnalysis_serviceGuard() throws Exception {
    LearningEngineTaskService learningEngineTaskService = mock(LearningEngineTaskService.class);
    FieldUtils.writeField(logAnalysisService, "learningEngineTaskService", learningEngineTaskService, true);
    Instant start = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    LogAnalysisDTO analysisDTO = createAnalysisDTO(end);

    LogAnalysisCluster frequencyPattern =
        hPersistence.createQuery(LogAnalysisCluster.class).filter(LogAnalysisClusterKeys.cvConfigId, cvConfigId).get();
    assertThat(frequencyPattern).isNull();

    LogAnalysisResult result =
        hPersistence.createQuery(LogAnalysisResult.class).filter(LogAnalysisResultKeys.cvConfigId, cvConfigId).get();

    assertThat(result).isNull();

    logAnalysisService.saveAnalysis(cvConfigId, "taskId", start, end, analysisDTO);

    Mockito.verify(learningEngineTaskService).markCompleted("taskId");

    List<LogAnalysisCluster> analysisClusters = hPersistence.createQuery(LogAnalysisCluster.class)
                                                    .filter(LogAnalysisClusterKeys.cvConfigId, cvConfigId)
                                                    .asList();
    assertThat(analysisClusters).isNotNull();
    assertThat(analysisClusters.size()).isEqualTo(2);

    result =
        hPersistence.createQuery(LogAnalysisResult.class).filter(LogAnalysisResultKeys.cvConfigId, cvConfigId).get();

    assertThat(result).isNotNull();
    assertThat(result.getLogAnalysisResults().size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSaveAnalysis_deployment() {
    LogCanaryAnalysisLearningEngineTask task = LogCanaryAnalysisLearningEngineTask.builder()
                                                   .controlHosts(Sets.newHashSet("host1", "host2"))
                                                   .controlDataUrl("controlData")
                                                   .testDataUrl("testData")
                                                   .build();
    fillCommon(task, LearningEngineTaskType.CANARY_LOG_ANALYSIS);
    learningEngineTaskService.createLearningEngineTask(task);
    DeploymentLogAnalysisDTO deploymentLogAnalysisDTO = createDeploymentAnalysisDTO();
    logAnalysisService.saveAnalysis(task.getUuid(), deploymentLogAnalysisDTO);
    List<DeploymentLogAnalysis> deploymentLogAnalyses =
        deploymentLogAnalysisService.getAnalysisResults(verificationTaskId);
    assertThat(deploymentLogAnalyses).hasSize(1);
    assertThat(deploymentLogAnalyses.get(0).getClusters()).isEqualTo(deploymentLogAnalysisDTO.getClusters());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetTaskStatus() throws Exception {
    LearningEngineTaskService learningEngineTaskService = mock(LearningEngineTaskService.class);
    FieldUtils.writeField(logAnalysisService, "learningEngineTaskService", learningEngineTaskService, true);
    List<String> taskIds = new ArrayList<>();
    taskIds.add("task1");
    taskIds.add("task2");
    logAnalysisService.getTaskStatus(taskIds);

    Mockito.verify(learningEngineTaskService).getTaskStatus(anySet());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetAnalysisClusters() {
    List<LogAnalysisCluster> clusters = buildAnalysisClusters(1234l, 23456l);
    clusters.forEach(cluster -> cluster.setCvConfigId(cvConfigId));
    hPersistence.save(clusters);

    List<LogAnalysisCluster> clustersReturned =
        logAnalysisService.getAnalysisClusters(cvConfigId, new HashSet<>(Arrays.asList(1234l, 23456l)));

    assertThat(clustersReturned).containsExactlyInAnyOrderElementsOf(clusters);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetAnalysisResults() {
    Instant start = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    LogAnalysisResult result = LogAnalysisResult.builder()
                                   .cvConfigId(cvConfigId)
                                   .logAnalysisResults(getResults(1234l, 23456l))
                                   .analysisStartTime(start)
                                   .analysisEndTime(end)
                                   .build();
    hPersistence.save(result);
    LogAnalysisResult result2 = LogAnalysisResult.builder()
                                    .cvConfigId(cvConfigId)
                                    .logAnalysisResults(getResults(1234l, 23456l))
                                    .analysisStartTime(end)
                                    .analysisEndTime(end.plus(5, ChronoUnit.MINUTES))
                                    .build();
    hPersistence.save(result2);

    List<LogAnalysisResult> analysisResults = logAnalysisService.getAnalysisResults(
        cvConfigId, Arrays.asList(LogAnalysisResult.LogAnalysisTag.UNKNOWN), start, end);

    assertThat(analysisResults.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetAnalysisResults_validateOnlyUnknownIsReturned() {
    Instant start = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    LogAnalysisResult result = LogAnalysisResult.builder()
                                   .cvConfigId(cvConfigId)
                                   .logAnalysisResults(getResults(1235l))
                                   .analysisStartTime(start)
                                   .analysisEndTime(end)
                                   .build();
    hPersistence.save(result);
    LogAnalysisResult result2 = LogAnalysisResult.builder()
                                    .cvConfigId(cvConfigId)
                                    .logAnalysisResults(getResults(1234l, 23456l))
                                    .analysisStartTime(start)
                                    .analysisEndTime(end)
                                    .build();
    hPersistence.save(result2);

    List<LogAnalysisResult> analysisResults = logAnalysisService.getAnalysisResults(
        cvConfigId, Arrays.asList(LogAnalysisResult.LogAnalysisTag.UNKNOWN), start, end);

    assertThat(analysisResults.size()).isEqualTo(1);
  }

  private List<ClusteredLog> createClusteredLogRecords(Instant startTime, Instant endTime) {
    List<ClusteredLog> logRecords = new ArrayList<>();

    Instant timestamp = startTime;
    while (timestamp.isBefore(endTime)) {
      ClusteredLog record = ClusteredLog.builder()
                                .verificationTaskId(verificationTaskId)
                                .timestamp(timestamp)
                                .log("sample log record")
                                .clusterLabel("1")
                                .clusterCount(4)
                                .clusterLevel(LogClusterLevel.L2)
                                .build();
      logRecords.add(record);
      timestamp = timestamp.plus(1, ChronoUnit.MINUTES);
    }

    return logRecords;
  }
  //
  private LogAnalysisDTO createAnalysisDTO(Instant endTime) {
    List<LogAnalysisCluster> clusters = buildAnalysisClusters(1234l, 23456l);
    LogAnalysisResult result = LogAnalysisResult.builder().logAnalysisResults(getResults(12345l, 23456l)).build();
    return LogAnalysisDTO.builder()
        .cvConfigId(cvConfigId)
        .logClusters(clusters)
        .logAnalysisResults(result.getLogAnalysisResults())
        .analysisMinute(endTime.getEpochSecond() / 60)
        .build();
  }

  private DeploymentLogAnalysisDTO createDeploymentAnalysisDTO() {
    return DeploymentLogAnalysisDTO.builder()
        .clusters(Collections.singletonList(
            DeploymentLogAnalysisDTO.Cluster.builder().label(1).text("text").x(2).y(3).build()))
        .resultSummary(DeploymentLogAnalysisDTO.ResultSummary.builder()
                           .controlClusterLabels(Collections.singletonList(1))
                           .risk(2)
                           .score(.4)
                           .build())
        .build();
  }
  private List<AnalysisResult> getResults(long... labels) {
    List<AnalysisResult> results = new ArrayList<>();
    for (int i = 0; i < labels.length; i++) {
      AnalysisResult analysisResult =
          AnalysisResult.builder()
              .count(3)
              .label(labels[i])
              .tag(i % 2 == 0 ? LogAnalysisResult.LogAnalysisTag.KNOWN : LogAnalysisResult.LogAnalysisTag.UNKNOWN)
              .build();
      results.add(analysisResult);
    }
    return results;
  }

  private List<LogAnalysisCluster> buildAnalysisClusters(long... labels) {
    List<LogAnalysisCluster> clusters = new ArrayList<>();
    for (int i = 0; i < labels.length; i++) {
      LogAnalysisCluster cluster = LogAnalysisCluster.builder()
                                       .label(labels[i])
                                       .isEvicted(false)
                                       .text("exception message")
                                       .trend(LogAnalysisCluster.Trend.builder()
                                                  .count(Arrays.asList(1, 2, 3, 4))
                                                  .timestamp(Arrays.asList(12353453l, 132312l, 132213l))
                                                  .build())
                                       .build();
      clusters.add(cluster);
    }
    return clusters;
  }

  private CVConfig createCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    fillCommon(cvConfig);
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier(generateUuid());
    return cvConfig;
  }

  private void fillCommon(CVConfig cvConfig) {
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(generateUuid());
    cvConfig.setConnectorIdentifier(generateUuid());
    cvConfig.setServiceIdentifier(generateUuid());
    cvConfig.setEnvIdentifier(generateUuid());
    cvConfig.setProjectIdentifier(generateUuid());
    cvConfig.setGroupId(generateUuid());
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName(generateUuid());
  }

  private void fillCommon(LearningEngineTask learningEngineTask, LearningEngineTaskType analysisType) {
    learningEngineTask.setTaskStatus(ExecutionStatus.QUEUED);
    learningEngineTask.setVerificationTaskId(verificationTaskId);
    learningEngineTask.setAnalysisType(analysisType);
    learningEngineTask.setFailureUrl("failure-url");
    learningEngineTask.setAnalysisStartTime(Instant.now().minus(Duration.ofMinutes(10)));
    learningEngineTask.setAnalysisEndTime(Instant.now());
  }
}