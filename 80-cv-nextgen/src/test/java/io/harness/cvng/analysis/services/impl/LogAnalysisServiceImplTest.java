package io.harness.cvng.analysis.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anySet;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cvng.CVNextGenBaseTest;
import io.harness.cvng.analysis.beans.LogAnalysisCluster;
import io.harness.cvng.analysis.beans.LogAnalysisDTO;
import io.harness.cvng.analysis.beans.LogClusterDTO;
import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.analysis.entities.ClusteredLog;
import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType;
import io.harness.cvng.analysis.entities.LogAnalysisFrequencyPattern;
import io.harness.cvng.analysis.entities.LogAnalysisFrequencyPattern.FrequencyPattern;
import io.harness.cvng.analysis.entities.LogAnalysisRecord;
import io.harness.cvng.analysis.entities.LogAnalysisRecord.LogAnalysisRecordKeys;
import io.harness.cvng.analysis.services.api.LearningEngineTaskService;
import io.harness.cvng.analysis.services.api.LogAnalysisService;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LogAnalysisServiceImplTest extends CVNextGenBaseTest {
  private String cvConfigId;
  @Inject private HPersistence hPersistence;
  @Mock private LearningEngineTaskService learningEngineTaskService;
  @Inject private LogAnalysisService logAnalysisService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    cvConfigId = generateUuid();
    // FieldUtils.writeField(logAnalysisService, "cvConfigService", cvConfigService, true);
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    cvConfig.setAccountId(generateUuid());
    cvConfig.setServiceIdentifier(generateUuid());
    cvConfig.setEnvIdentifier(generateUuid());
    cvConfig.setUuid(cvConfigId);
    hPersistence.save(cvConfig);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void scheduleLogAnalysisTask() {
    AnalysisInput input = AnalysisInput.builder()
                              .cvConfigId(cvConfigId)
                              .startTime(Instant.now().minus(10, ChronoUnit.MINUTES))
                              .endTime(Instant.now())
                              .build();
    List<String> taskIds = logAnalysisService.scheduleLogAnalysisTask(input);

    assertThat(taskIds).isNotNull();

    assertThat(taskIds.size()).isEqualTo(1);

    LearningEngineTask task = hPersistence.get(LearningEngineTask.class, taskIds.get(0));
    assertThat(task).isNotNull();
    assertThat(task.getCvConfigId()).isEqualTo(cvConfigId);
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

    List<LogClusterDTO> logClusterDTOList = logAnalysisService.getTestData(cvConfigId, start, end);

    assertThat(logClusterDTOList).isNotNull();
    assertThat(logClusterDTOList.size()).isEqualTo(l2Logs.size());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void getFrequencyPattern_firstAnalysis() {
    Instant start = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    List<FrequencyPattern> patterns = logAnalysisService.getFrequencyPattern(cvConfigId, start, end);

    assertThat(patterns).isNotNull();
    assertThat(patterns.isEmpty()).isTrue();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void getFrequencyPattern_hasPreviousAnalysis() {
    Instant start = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    List<FrequencyPattern> frequencyPatterns = createAnalysisDTO(end).getFrequencyPatterns();

    LogAnalysisFrequencyPattern patternsInDb = LogAnalysisFrequencyPattern.builder()
                                                   .cvConfigId(cvConfigId)
                                                   .analysisStartTime(start)
                                                   .analysisEndTime(end)
                                                   .frequencyPatterns(frequencyPatterns)
                                                   .build();
    hPersistence.save(patternsInDb);

    List<FrequencyPattern> patterns = logAnalysisService.getFrequencyPattern(cvConfigId, start, end);

    assertThat(patterns).isNotNull();
    assertThat(patterns.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void saveAnalysis() throws Exception {
    FieldUtils.writeField(logAnalysisService, "learningEngineTaskService", learningEngineTaskService, true);
    Instant start = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    LogAnalysisDTO analysisDTO = createAnalysisDTO(end);

    LogAnalysisFrequencyPattern frequencyPattern = hPersistence.createQuery(LogAnalysisFrequencyPattern.class)
                                                       .filter(LogAnalysisRecordKeys.cvConfigId, cvConfigId)
                                                       .get();
    assertThat(frequencyPattern).isNull();

    LogAnalysisRecord record =
        hPersistence.createQuery(LogAnalysisRecord.class).filter(LogAnalysisRecordKeys.cvConfigId, cvConfigId).get();

    assertThat(record).isNull();

    logAnalysisService.saveAnalysis(cvConfigId, "taskId", start, end, analysisDTO);

    Mockito.verify(learningEngineTaskService).markCompleted("taskId");

    frequencyPattern = hPersistence.createQuery(LogAnalysisFrequencyPattern.class)
                           .filter(LogAnalysisRecordKeys.cvConfigId, cvConfigId)
                           .get();
    assertThat(frequencyPattern).isNotNull();

    record =
        hPersistence.createQuery(LogAnalysisRecord.class).filter(LogAnalysisRecordKeys.cvConfigId, cvConfigId).get();

    assertThat(record).isNotNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetTaskStatus() throws Exception {
    FieldUtils.writeField(logAnalysisService, "learningEngineTaskService", learningEngineTaskService, true);
    List<String> taskIds = new ArrayList<>();
    taskIds.add("task1");
    taskIds.add("task2");
    logAnalysisService.getTaskStatus(taskIds);

    Mockito.verify(learningEngineTaskService).getTaskStatus(anySet());
  }

  private List<ClusteredLog> createClusteredLogRecords(Instant startTime, Instant endTime) {
    List<ClusteredLog> logRecords = new ArrayList<>();

    Instant timestamp = startTime;
    while (timestamp.isBefore(endTime)) {
      ClusteredLog record = ClusteredLog.builder()
                                .cvConfigId(cvConfigId)
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

  private LogAnalysisDTO createAnalysisDTO(Instant endTime) {
    return LogAnalysisDTO.builder()
        .cvConfigId(cvConfigId)
        .testClusters(buildAnalysisClusters())
        .controlClusters(buildAnalysisClusters())
        .unknownClusters(buildAnalysisClusters())
        .analysisMinute(endTime.getEpochSecond() / 60)
        .frequencyPatterns(getFrequencePatterns())
        .build();
  }

  private List<LogAnalysisCluster> buildAnalysisClusters() {
    LogAnalysisCluster cluster =
        LogAnalysisCluster.builder().host("host1").clusterLabel("1").text("exception message").build();

    return Arrays.asList(cluster);
  }

  private List<FrequencyPattern> getFrequencePatterns() {
    FrequencyPattern.Pattern pattern = FrequencyPattern.Pattern.builder()
                                           .sequence(Arrays.asList(1, 2, 3, 4))
                                           .timestamps(Arrays.asList(12353453l, 132312l, 132213l))
                                           .build();
    FrequencyPattern frequencyPattern =
        FrequencyPattern.builder().patterns(Arrays.asList(pattern)).label(12).text("exception message").build();
    return Arrays.asList(frequencyPattern);
  }
}