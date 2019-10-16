package io.harness.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rest.RestResponse.Builder.aRestResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.common.VerificationConstants.CV_24x7_STATE_EXECUTION;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonParser;
import com.google.inject.Inject;

import io.harness.VerificationBaseTest;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder.OrderType;
import io.harness.category.element.UnitTests;
import io.harness.entities.TimeSeriesAnomaliesRecord;
import io.harness.entities.TimeSeriesAnomaliesRecord.TimeSeriesAnomaliesRecordKeys;
import io.harness.entities.TimeSeriesCumulativeSums;
import io.harness.exception.WingsException;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.serializer.JsonUtils;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import io.harness.time.Timestamp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.assertj.core.util.Lists;
import org.intellij.lang.annotations.Language;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.RiskLevel;
import software.wings.metrics.TimeSeriesDataRecord;
import software.wings.metrics.TimeSeriesDataRecord.TimeSeriesMetricRecordKeys;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.MetricAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLHostSummary;
import software.wings.service.impl.analysis.TimeSeriesMLScores;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.analysis.Version;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord.NewRelicMetricDataRecordKeys;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Slf4j
public class TimeSeriesAnalysisServiceTest extends VerificationBaseTest {
  private String cvConfigId;
  private String serviceId;
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowExecutionId;
  private Random randomizer;
  private int currentEpochMinute;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private TimeSeriesAnalysisService timeSeriesAnalysisService;
  @Mock private VerificationManagerClientHelper managerClientHelper;

  @Before
  public void setup() throws IllegalAccessException {
    long seed = System.currentTimeMillis();
    logger.info("seed: {}", seed);
    randomizer = new Random(seed);
    cvConfigId = generateUuid();
    serviceId = generateUuid();
    accountId = generateUuid();
    appId = generateUuid();
    stateExecutionId = generateUuid();
    workflowExecutionId = generateUuid();
    timeSeriesAnalysisService = spy(timeSeriesAnalysisService);
    currentEpochMinute = (int) TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    MockitoAnnotations.initMocks(this);
    when(managerClientHelper.callManagerWithRetry(any())).thenReturn(aRestResponse().withResource(false).build());

    FieldUtils.writeField(timeSeriesAnalysisService, "managerClientHelper", managerClientHelper, true);
  }
  @Test
  @Category(UnitTests.class)
  public void testSaveMetricDataIfMetricDataIsEmpty() {
    assertThat(
        timeSeriesAnalysisService.saveMetricData(accountId, appId, stateExecutionId, generateUuid(), Lists.emptyList()))
        .isFalse();
  }

  @Test
  @Category(UnitTests.class)
  public void testSaveMetricDataIfStateExecutionIdIsInvalid() throws IllegalAccessException {
    NewRelicMetricDataRecord newRelicMetricDataRecord = NewRelicMetricDataRecord.builder().build();
    LearningEngineService learningEngineService = mock(LearningEngineService.class);
    FieldUtils.writeField(timeSeriesAnalysisService, "learningEngineService", learningEngineService, true);
    when(learningEngineService.isStateValid(anyString(), anyString())).thenReturn(false);
    assertThat(timeSeriesAnalysisService.saveMetricData(
                   accountId, appId, stateExecutionId, generateUuid(), Lists.newArrayList(newRelicMetricDataRecord)))
        .isFalse();
  }

  @Test
  @Category(UnitTests.class)
  public void testSaveMetricDataForAddingValidUntil() throws IllegalAccessException {
    NewRelicMetricDataRecord newRelicMetricDataRecord =
        NewRelicMetricDataRecord.builder().cvConfigId(generateUuid()).build();
    LearningEngineService learningEngineService = mock(LearningEngineService.class);
    FieldUtils.writeField(timeSeriesAnalysisService, "learningEngineService", learningEngineService, true);
    when(learningEngineService.isStateValid(anyString(), anyString())).thenReturn(true);

    assertThat(timeSeriesAnalysisService.saveMetricData(
                   accountId, appId, stateExecutionId, generateUuid(), Lists.newArrayList(newRelicMetricDataRecord)))
        .isTrue();
    assertThat(newRelicMetricDataRecord.getValidUntil())
        .isBefore(Date.from(OffsetDateTime.now().plusMonths(2).toInstant()));
  }

  @Test
  @Category(UnitTests.class)
  public void testSaveMetricDataForWorkflowAsNewRelicDataRecord() throws IllegalAccessException {
    NewRelicMetricDataRecord newRelicMetricDataRecord =
        NewRelicMetricDataRecord.builder().stateExecutionId(generateUuid()).cvConfigId(generateUuid()).build();
    LearningEngineService learningEngineService = mock(LearningEngineService.class);
    FieldUtils.writeField(timeSeriesAnalysisService, "learningEngineService", learningEngineService, true);
    when(learningEngineService.isStateValid(anyString(), anyString())).thenReturn(true);

    assertThat(
        timeSeriesAnalysisService.saveMetricData(accountId, appId, newRelicMetricDataRecord.getStateExecutionId(),
            generateUuid(), Lists.newArrayList(newRelicMetricDataRecord)))
        .isTrue();
    NewRelicMetricDataRecord savedRecord =
        wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
            .filter("stateExecutionId", newRelicMetricDataRecord.getStateExecutionId())
            .get();
    assertThat(savedRecord).isNotNull();
    TimeSeriesDataRecord timeSeriesDataRecord =
        wingsPersistence.createQuery(TimeSeriesDataRecord.class)
            .filter(TimeSeriesMetricRecordKeys.stateExecutionId, newRelicMetricDataRecord.getStateExecutionId())
            .get();
    assertThat(timeSeriesDataRecord).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testSaveMetricDataForServiceGuard() throws IllegalAccessException {
    NewRelicMetricDataRecord newRelicMetricDataRecord =
        NewRelicMetricDataRecord.builder()
            .stateExecutionId(CV_24x7_STATE_EXECUTION + "-" + generateUuid())
            .cvConfigId(generateUuid())
            .build();
    LearningEngineService learningEngineService = mock(LearningEngineService.class);
    FieldUtils.writeField(timeSeriesAnalysisService, "learningEngineService", learningEngineService, true);
    when(learningEngineService.isStateValid(anyString(), anyString())).thenReturn(true);

    assertThat(
        timeSeriesAnalysisService.saveMetricData(accountId, appId, newRelicMetricDataRecord.getStateExecutionId(),
            generateUuid(), Lists.newArrayList(newRelicMetricDataRecord)))
        .isTrue();
    NewRelicMetricDataRecord savedRecord =
        wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
            .filter("stateExecutionId", newRelicMetricDataRecord.getStateExecutionId())
            .get();
    assertThat(savedRecord).isNull();
    TimeSeriesDataRecord timeSeriesDataRecord =
        wingsPersistence.createQuery(TimeSeriesDataRecord.class)
            .filter(TimeSeriesMetricRecordKeys.stateExecutionId, newRelicMetricDataRecord.getStateExecutionId())
            .get();
    assertThat(timeSeriesDataRecord).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testSaveAnalysisRecords() {
    NewRelicMetricAnalysisRecord newRelicMetricAnalysisRecord =
        NewRelicMetricAnalysisRecord.builder().message("data not found").stateExecutionId(generateUuid()).build();
    timeSeriesAnalysisService.saveAnalysisRecords(newRelicMetricAnalysisRecord);
    NewRelicMetricAnalysisRecord savedRecord =
        wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class)
            .filter("stateExecutionId", newRelicMetricAnalysisRecord.getStateExecutionId())
            .get();
    assertThat(savedRecord).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetTimeSeriesMLScores() {
    String workflowId = generateUuid();
    TimeSeriesMLScores timeSeriesMLScores = TimeSeriesMLScores.builder()
                                                .analysisMinute(currentEpochMinute)
                                                .workflowExecutionId(workflowExecutionId)
                                                .appId(appId)
                                                .stateExecutionId(stateExecutionId)
                                                .stateType(StateType.NEW_RELIC)
                                                .workflowId(workflowId)
                                                .build();
    wingsPersistence.save(timeSeriesMLScores);
    List<String> workflowIds = Lists.newArrayList(workflowExecutionId);
    doReturn(workflowIds)
        .when(timeSeriesAnalysisService)
        .getLastSuccessfulWorkflowExecutionIds(anyString(), anyString(), anyString());
    List<TimeSeriesMLScores> returnedResults =
        timeSeriesAnalysisService.getTimeSeriesMLScores(appId, workflowId, currentEpochMinute, 1);
    assertThat(returnedResults).hasSize(1);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetRecordsIfNoRecordsAvailable() {
    Set<NewRelicMetricDataRecord> records =
        timeSeriesAnalysisService.getRecords(appId, stateExecutionId, "DEFAULT", Sets.newHashSet("test-host"), 10, 10);
    assertThat(records).isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetRecordsWhenRecordsAreAvailable() {
    NewRelicMetricDataRecord newRelicMetricDataRecord = NewRelicMetricDataRecord.builder()
                                                            .groupName("DEFAULT")
                                                            .stateExecutionId(stateExecutionId)
                                                            .appId(appId)
                                                            .stateType(StateType.NEW_RELIC)
                                                            .dataCollectionMinute(currentEpochMinute)
                                                            .host("test-host")
                                                            .build();
    wingsPersistence.save(newRelicMetricDataRecord);
    Set<NewRelicMetricDataRecord> records = timeSeriesAnalysisService.getRecords(
        appId, stateExecutionId, "DEFAULT", Sets.newHashSet("test-host"), currentEpochMinute, currentEpochMinute);
    assertThat(records).hasSize(1);
    assertThat(records.iterator().next().getHost()).isEqualTo("test-host");
  }

  @Test
  @Category(UnitTests.class)
  public void testGetRecordsAndExcludeHeartbeatRecord() {
    NewRelicMetricDataRecord newRelicMetricDataRecord = NewRelicMetricDataRecord.builder()
                                                            .groupName("DEFAULT")
                                                            .stateExecutionId(stateExecutionId)
                                                            .appId(appId)
                                                            .stateType(StateType.NEW_RELIC)
                                                            .dataCollectionMinute(currentEpochMinute)
                                                            .host("test-host")
                                                            .level(ClusterLevel.L1)
                                                            .build();
    wingsPersistence.save(newRelicMetricDataRecord);
    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .groupName("DEFAULT")
                              .stateExecutionId(stateExecutionId)
                              .appId(appId)
                              .stateType(StateType.NEW_RELIC)
                              .dataCollectionMinute(currentEpochMinute)
                              .host("test-host")
                              .level(ClusterLevel.H0)
                              .build());
    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .groupName("DEFAULT")
                              .stateExecutionId(stateExecutionId)
                              .appId(appId)
                              .stateType(StateType.NEW_RELIC)
                              .dataCollectionMinute(currentEpochMinute)
                              .host("test-host")
                              .level(ClusterLevel.HF)
                              .build());
    Set<NewRelicMetricDataRecord> records = timeSeriesAnalysisService.getRecords(
        appId, stateExecutionId, "DEFAULT", Sets.newHashSet("test-host"), currentEpochMinute, currentEpochMinute);
    assertThat(records).hasSize(1);
    assertThat(records.iterator().next().getLevel()).isEqualTo(ClusterLevel.L1);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetPreviousRecordsAndExcludeHeartbeatRecord() {
    NewRelicMetricDataRecord newRelicMetricDataRecord = NewRelicMetricDataRecord.builder()
                                                            .groupName("DEFAULT")
                                                            .workflowExecutionId(workflowExecutionId)
                                                            .appId(appId)
                                                            .stateType(StateType.NEW_RELIC)
                                                            .dataCollectionMinute(currentEpochMinute)
                                                            .host("test-host")
                                                            .level(ClusterLevel.L1)
                                                            .build();
    wingsPersistence.save(newRelicMetricDataRecord);
    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .groupName("DEFAULT")
                              .workflowExecutionId(workflowExecutionId)
                              .appId(appId)
                              .stateType(StateType.NEW_RELIC)
                              .dataCollectionMinute(currentEpochMinute)
                              .host("test-host")
                              .level(ClusterLevel.H0)
                              .build());
    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .groupName("DEFAULT")
                              .workflowExecutionId(workflowExecutionId)
                              .appId(appId)
                              .stateType(StateType.NEW_RELIC)
                              .dataCollectionMinute(currentEpochMinute)
                              .host("test-host")
                              .level(ClusterLevel.HF)
                              .build());
    Set<NewRelicMetricDataRecord> records = timeSeriesAnalysisService.getPreviousSuccessfulRecords(
        appId, workflowExecutionId, "DEFAULT", currentEpochMinute, currentEpochMinute);
    assertThat(records).hasSize(1);
    assertThat(records.iterator().next().getLevel()).isEqualTo(ClusterLevel.L1);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetPreviousRecordsIfWorkflowExecutionIdIsEmpty() {
    NewRelicMetricDataRecord newRelicMetricDataRecord = NewRelicMetricDataRecord.builder()
                                                            .groupName("DEFAULT")
                                                            .workflowExecutionId(workflowExecutionId)
                                                            .appId(appId)
                                                            .stateType(StateType.NEW_RELIC)
                                                            .dataCollectionMinute(currentEpochMinute)
                                                            .host("test-host")
                                                            .level(ClusterLevel.L1)
                                                            .build();
    wingsPersistence.save(newRelicMetricDataRecord);
    Set<NewRelicMetricDataRecord> records =
        timeSeriesAnalysisService.getPreviousSuccessfulRecords(appId, null, "DEFAULT", currentEpochMinute, 10);
    assertThat(records).isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMaxControlMinuteWithData() {
    String workflowId = generateUuid();
    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .groupName("DEFAULT")
                              .workflowId(workflowId)
                              .stateExecutionId(stateExecutionId)
                              .workflowExecutionId(workflowExecutionId)
                              .appId(appId)
                              .serviceId(serviceId)
                              .stateType(StateType.NEW_RELIC)
                              .dataCollectionMinute(currentEpochMinute)
                              .host("test-host")
                              .level(ClusterLevel.L1)
                              .build());
    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .groupName("DEFAULT")
                              .workflowId(workflowId)
                              .stateExecutionId(stateExecutionId)
                              .workflowExecutionId(workflowExecutionId)
                              .appId(appId)
                              .serviceId(serviceId)
                              .stateType(StateType.NEW_RELIC)
                              .dataCollectionMinute(currentEpochMinute - 2)
                              .host("test-host")
                              .level(ClusterLevel.L1)
                              .build());
    int maxControlMinuteWithData = timeSeriesAnalysisService.getMaxControlMinuteWithData(
        StateType.NEW_RELIC, appId, serviceId, workflowId, workflowExecutionId, "DEFAULT");
    assertThat(maxControlMinuteWithData).isEqualTo(currentEpochMinute);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMaxControlMinuteWithNoData() {
    String workflowId = generateUuid();
    int maxControlMinuteWithData = timeSeriesAnalysisService.getMaxControlMinuteWithData(
        StateType.NEW_RELIC, appId, serviceId, workflowId, workflowExecutionId, "DEFAULT");
    assertThat(maxControlMinuteWithData).isEqualTo(-1);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMinControlMinuteWithData() {
    String workflowId = generateUuid();
    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .groupName("DEFAULT")
                              .workflowId(workflowId)
                              .stateExecutionId(stateExecutionId)
                              .workflowExecutionId(workflowExecutionId)
                              .appId(appId)
                              .serviceId(serviceId)
                              .stateType(StateType.NEW_RELIC)
                              .dataCollectionMinute(currentEpochMinute)
                              .host("test-host")
                              .level(ClusterLevel.L1)
                              .build());
    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .groupName("DEFAULT")
                              .workflowId(workflowId)
                              .stateExecutionId(stateExecutionId)
                              .workflowExecutionId(workflowExecutionId)
                              .appId(appId)
                              .serviceId(serviceId)
                              .stateType(StateType.NEW_RELIC)
                              .dataCollectionMinute(currentEpochMinute - 2)
                              .host("test-host")
                              .level(ClusterLevel.L1)
                              .build());
    int maxControlMinuteWithData = timeSeriesAnalysisService.getMinControlMinuteWithData(
        StateType.NEW_RELIC, appId, serviceId, workflowId, workflowExecutionId, "DEFAULT");
    assertThat(maxControlMinuteWithData).isEqualTo(currentEpochMinute - 2);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMinControlMinuteWithNoData() {
    String workflowId = generateUuid();
    int maxControlMinuteWithData = timeSeriesAnalysisService.getMinControlMinuteWithData(
        StateType.NEW_RELIC, appId, serviceId, workflowId, workflowExecutionId, "DEFAULT");
    assertThat(maxControlMinuteWithData).isEqualTo(-1);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetPreviousAnalysis() {
    TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord = TimeSeriesMLAnalysisRecord.builder().build();
    timeSeriesMLAnalysisRecord.setAppId(appId);
    timeSeriesMLAnalysisRecord.setTag("tag");
    timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
    timeSeriesMLAnalysisRecord.setAnalysisMinute(currentEpochMinute);
    wingsPersistence.save(timeSeriesMLAnalysisRecord);
    TimeSeriesMLAnalysisRecord result =
        timeSeriesAnalysisService.getPreviousAnalysis(appId, cvConfigId, currentEpochMinute, "tag");
    assertThat(result).isNotNull();
  }
  @Test
  @Category(UnitTests.class)
  public void testGetCVMetricRecords() {
    int numOfHosts = 5;
    int numOfTxns = 40;
    int numOfMinutes = 200;

    List<String> hosts = new ArrayList<>();
    for (int i = 0; i < numOfHosts; i++) {
      hosts.add("host-" + i);
    }

    List<String> txns = new ArrayList<>();
    for (int i = 0; i < numOfTxns; i++) {
      txns.add("txn-" + i);
    }

    List<NewRelicMetricDataRecord> metricDataRecords = new ArrayList<>();
    Map<String, Double> values = new HashMap<>();
    values.put("m1", 1.0);
    hosts.forEach(host -> txns.forEach(txn -> {
      for (int k = 0; k < numOfMinutes; k++) {
        metricDataRecords.add(NewRelicMetricDataRecord.builder()
                                  .cvConfigId(cvConfigId)
                                  .serviceId(serviceId)
                                  .stateType(StateType.NEW_RELIC)
                                  .name(txn)
                                  .timeStamp(k * 1000)
                                  .dataCollectionMinute(k)
                                  .host(host)
                                  .values(values)
                                  .build());
      }
    }));
    final List<TimeSeriesDataRecord> dataRecords =
        TimeSeriesDataRecord.getTimeSeriesDataRecordsFromNewRelicDataRecords(metricDataRecords);
    dataRecords.forEach(dataRecord -> dataRecord.compress());
    wingsPersistence.save(dataRecords);

    assertThat(wingsPersistence.createQuery(TimeSeriesDataRecord.class, excludeAuthority).asList().size())
        .isEqualTo(numOfHosts * numOfMinutes);

    int analysisStartMinute = randomizer.nextInt(100);
    int analysisEndMinute = analysisStartMinute + randomizer.nextInt(102);
    logger.info("start {} end {}", analysisStartMinute, analysisEndMinute);
    final Set<NewRelicMetricDataRecord> metricRecords =
        timeSeriesAnalysisService.getMetricRecords(cvConfigId, analysisStartMinute, analysisEndMinute, null, accountId);
    int numOfMinutesAsked = analysisEndMinute - analysisStartMinute + 1;
    assertThat(metricRecords.size()).isEqualTo(numOfMinutesAsked * numOfTxns * numOfHosts);

    metricRecords.forEach(metricRecord -> metricRecord.setUuid(null));
    Set<NewRelicMetricDataRecord> expectedRecords = new HashSet<>();
    hosts.forEach(host -> txns.forEach(txn -> {
      for (int k = analysisStartMinute; k <= analysisEndMinute; k++) {
        expectedRecords.add(NewRelicMetricDataRecord.builder()
                                .cvConfigId(cvConfigId)
                                .serviceId(serviceId)
                                .stateType(StateType.NEW_RELIC)
                                .name(txn)
                                .timeStamp(k * 1000)
                                .dataCollectionMinute(k)
                                .host(host)
                                .values(values)
                                .build());
      }
    }));
    assertThat(metricRecords).isEqualTo(expectedRecords);
  }

  @Test
  @Category(UnitTests.class)
  public void testCompressTimeSeriesMetricRecords() {
    int numOfTxns = 5;
    int numOfMetrics = 40;

    TreeBasedTable<String, String, Double> values = TreeBasedTable.create();
    TreeBasedTable<String, String, String> deeplinkMetadata = TreeBasedTable.create();

    List<String> txns = new ArrayList<>();
    for (int i = 0; i < numOfTxns; i++) {
      for (int j = 0; j < numOfMetrics; j++) {
        values.put("txn-" + i, "metric-" + j, randomizer.nextDouble());
        deeplinkMetadata.put("txn-" + i, "metric-" + j, generateUuid());
      }
    }

    final TimeSeriesDataRecord timeSeriesDataRecord = TimeSeriesDataRecord.builder()
                                                          .cvConfigId(cvConfigId)
                                                          .serviceId(serviceId)
                                                          .stateType(StateType.NEW_RELIC)
                                                          .timeStamp(1000)
                                                          .dataCollectionMinute(100)
                                                          .host(generateUuid())
                                                          .values(values)
                                                          .deeplinkMetadata(deeplinkMetadata)
                                                          .build();

    timeSeriesDataRecord.compress();

    final String recordId = wingsPersistence.save(timeSeriesDataRecord);

    TimeSeriesDataRecord savedRecord = wingsPersistence.get(TimeSeriesDataRecord.class, recordId);

    assertThat(savedRecord.getValues()).isEqualTo(TreeBasedTable.create());
    assertThat(savedRecord.getDeeplinkMetadata()).isEqualTo(TreeBasedTable.create());
    assertThat(savedRecord.getValuesBytes()).isNotNull();

    savedRecord.decompress();

    assertThat(savedRecord.getValues()).isNotNull();
    assertThat(savedRecord.getDeeplinkMetadata()).isNotNull();
    assertThat(savedRecord.getValuesBytes()).isNull();

    assertThat(savedRecord.getValues()).isEqualTo(values);
    assertThat(savedRecord.getDeeplinkMetadata()).isEqualTo(deeplinkMetadata);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetHeartBeat() {
    for (int i = 0; i < 10; i++) {
      wingsPersistence.save(NewRelicMetricDataRecord.builder()
                                .stateType(StateType.NEW_RELIC)
                                .appId(appId)
                                .stateExecutionId(stateExecutionId)
                                .workflowExecutionId(workflowExecutionId)
                                .serviceId(serviceId)
                                .groupName(NewRelicMetricDataRecord.DEFAULT_GROUP_NAME)
                                .dataCollectionMinute(i)
                                .level(ClusterLevel.HF)
                                .build());
    }

    NewRelicMetricDataRecord heartBeat = timeSeriesAnalysisService.getHeartBeat(StateType.NEW_RELIC, appId,
        stateExecutionId, workflowExecutionId, serviceId, NewRelicMetricDataRecord.DEFAULT_GROUP_NAME, OrderType.ASC);
    assertThat(heartBeat.getDataCollectionMinute()).isEqualTo(0);
    heartBeat = timeSeriesAnalysisService.getHeartBeat(StateType.NEW_RELIC, appId, stateExecutionId,
        workflowExecutionId, serviceId, NewRelicMetricDataRecord.DEFAULT_GROUP_NAME, OrderType.DESC);
    assertThat(heartBeat.getDataCollectionMinute()).isEqualTo(9);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetLastCVAnalysisMinuteIfEmpty() {
    assertThat(timeSeriesAnalysisService.getLastCVAnalysisMinute(appId, cvConfigId)).isEqualTo(-1);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetLastCVAnalysisMinuteIfAvailable() {
    TimeSeriesMLAnalysisRecord analysisRecord = TimeSeriesMLAnalysisRecord.builder().build();
    analysisRecord.setTransactions(new HashMap<>());
    analysisRecord.setAppId(appId);
    analysisRecord.setCvConfigId(cvConfigId);
    analysisRecord.setStateType(StateType.NEW_RELIC);
    analysisRecord.setAnalysisMinute(currentEpochMinute);
    analysisRecord.setTag("testTag");
    wingsPersistence.save(analysisRecord);
    analysisRecord = TimeSeriesMLAnalysisRecord.builder().build();
    analysisRecord.setTransactions(new HashMap<>());
    analysisRecord.setAppId(appId);
    analysisRecord.setCvConfigId(cvConfigId);
    analysisRecord.setStateType(StateType.NEW_RELIC);
    analysisRecord.setAnalysisMinute(currentEpochMinute - 1);
    analysisRecord.setTag("testTag");
    assertThat(timeSeriesAnalysisService.getLastCVAnalysisMinute(appId, cvConfigId)).isEqualTo(currentEpochMinute);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMaxCVCollectionMinuteIfEmpty() {
    assertThat(timeSeriesAnalysisService.getMaxCVCollectionMinute(appId, cvConfigId, accountId)).isEqualTo(-1);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMaxCVCollectionMinuteIfAvailable() {
    TimeSeriesDataRecord analysisRecord =
        TimeSeriesDataRecord.builder().dataCollectionMinute(currentEpochMinute).cvConfigId(cvConfigId).build();
    wingsPersistence.save(analysisRecord);
    wingsPersistence.save(
        TimeSeriesDataRecord.builder().dataCollectionMinute(currentEpochMinute - 1).cvConfigId(cvConfigId).build());
    assertThat(timeSeriesAnalysisService.getMaxCVCollectionMinute(appId, cvConfigId, accountId))
        .isEqualTo(currentEpochMinute);
  }

  @Test
  @Category(UnitTests.class)
  public void testSetTagInAnomRecords() {
    Map<String, Map<String, List<TimeSeriesMLHostSummary>>> anomMap = new HashMap<>();
    anomMap.put("txn1", new HashMap<>());
    MetricAnalysisRecord analysisRecord = TimeSeriesMLAnalysisRecord.builder().build();
    analysisRecord.setTransactions(new HashMap<>());
    analysisRecord.setMessage("test message");
    analysisRecord.setAppId(appId);
    analysisRecord.setStateExecutionId(stateExecutionId);
    analysisRecord.setStateType(StateType.NEW_RELIC);
    analysisRecord.setAnalysisMinute(12345);
    analysisRecord.setAnomalies(anomMap);
    analysisRecord.setTag("testTag");

    LearningEngineAnalysisTask task =
        LearningEngineAnalysisTask.builder().executionStatus(ExecutionStatus.RUNNING).cluster_level(2).build();
    task.setUuid("taskID1");
    wingsPersistence.save(task);

    CVConfiguration cvConfiguration = new CVConfiguration();
    cvConfiguration.setStateType(StateType.NEW_RELIC);
    cvConfiguration.setServiceId(serviceId);
    cvConfiguration.setEnvId(generateUuid());
    cvConfiguration.setAccountId(accountId);

    cvConfiguration.setUuid(cvConfigId);
    wingsPersistence.save(cvConfiguration);

    timeSeriesAnalysisService.saveAnalysisRecordsML(accountId, StateType.NEW_RELIC, appId, stateExecutionId,
        workflowExecutionId, "default", 12345, "taskID1", null, cvConfigId, analysisRecord, "testTag");

    TimeSeriesAnomaliesRecord anomaliesRecord = wingsPersistence.createQuery(TimeSeriesAnomaliesRecord.class)
                                                    .filter(TimeSeriesAnomaliesRecordKeys.cvConfigId, cvConfigId)
                                                    .get();

    assertThat(anomaliesRecord).isNotNull();
    assertThat(anomaliesRecord.getTag()).isEqualTo("testTag");

    task = wingsPersistence.get(LearningEngineAnalysisTask.class, "taskID1");
    assertThat(task.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetCumulativeSumsWithTimeRange() {
    String cvConfigId = generateUuid();
    String appId = generateUuid();
    String tag = "tag";
    int currentMinute = (int) TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    IntStream.range(0, 10).forEach(minute
        -> wingsPersistence.save(TimeSeriesCumulativeSums.builder()
                                     .cvConfigId(cvConfigId)
                                     .tag(tag)
                                     .analysisMinute(currentMinute + minute)
                                     .build()));

    assertThat(timeSeriesAnalysisService
                   .getCumulativeSumsForRange(appId, cvConfigId, currentMinute - 2, currentMinute - 1, tag)
                   .isEmpty());
    assertThat(timeSeriesAnalysisService.getCumulativeSumsForRange(appId, cvConfigId, currentMinute, currentMinute, tag)
                   .size())
        .isEqualTo(1);
    assertThat(
        timeSeriesAnalysisService.getCumulativeSumsForRange(appId, cvConfigId, currentMinute, currentMinute + 7, tag)
            .size())
        .isEqualTo(8);
    assertThat(timeSeriesAnalysisService
                   .getCumulativeSumsForRange(appId, cvConfigId, currentMinute - 1, currentMinute + 11, tag)
                   .size())
        .isEqualTo(10);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetCumulativeSumsWithTag() {
    String cvConfigId = generateUuid();
    String appId = generateUuid();
    int currentMinute = (int) TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    wingsPersistence.save(
        TimeSeriesCumulativeSums.builder().cvConfigId(cvConfigId).tag("tag1").analysisMinute(currentMinute).build());
    wingsPersistence.save(
        TimeSeriesCumulativeSums.builder().cvConfigId(cvConfigId).tag("tag2").analysisMinute(currentMinute).build());

    Set<TimeSeriesCumulativeSums> result = timeSeriesAnalysisService.getCumulativeSumsForRange(
        appId, cvConfigId, currentMinute, currentMinute, "test-tag");
    assertThat(result.isEmpty());
    result =
        timeSeriesAnalysisService.getCumulativeSumsForRange(appId, cvConfigId, currentMinute, currentMinute, "tag1");
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.iterator().next().getTag()).isEqualTo("tag1");
  }
  @Test
  @Category(UnitTests.class)
  public void testGetCumulativeSumsForRangeWhenNoCumulativeSumAvailable() {
    Set<TimeSeriesCumulativeSums> timeSeriesCumulativeSums =
        timeSeriesAnalysisService.getCumulativeSumsForRange(generateUuid(), generateUuid(), 10, 20, "test-tag");
    assertThat(timeSeriesCumulativeSums.isEmpty());
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void testGetCumulativeSumsForRangeInCaseOfInvalidParams() {
    timeSeriesAnalysisService.getCumulativeSumsForRange(generateUuid(), null, 10, 20, "test-tag");
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void testGetCumulativeSumsForRangeInCaseOfInvalidTimeRange() {
    timeSeriesAnalysisService.getCumulativeSumsForRange(generateUuid(), generateUuid(), 21, 20, "test-tag");
  }

  @Test
  @Category(UnitTests.class)
  public void testGetLastDataCollectedMinuteWhenRecordDoesNotExist() {
    long minute = timeSeriesAnalysisService.getLastDataCollectedMinute(appId, stateExecutionId, StateType.APP_DYNAMICS);
    assertThat(minute).isEqualTo(-1);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetLastDataCollectedMinuteWhenRecordDoesExists() {
    int dataCollectionMinute = (int) TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());

    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .dataCollectionMinute(dataCollectionMinute)
                              .stateExecutionId(stateExecutionId)
                              .appId(appId)
                              .host("test-host")
                              .workflowId(UUID.randomUUID().toString())
                              .workflowExecutionId(workflowExecutionId)
                              .stateType(StateType.APP_DYNAMICS)
                              .build());

    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .dataCollectionMinute(dataCollectionMinute + 1)
                              .stateExecutionId(stateExecutionId)
                              .appId(appId)
                              .host("test-host")
                              .workflowId(UUID.randomUUID().toString())
                              .workflowExecutionId(workflowExecutionId)
                              .stateType(StateType.APP_DYNAMICS)
                              .build());
    long minute = timeSeriesAnalysisService.getLastDataCollectedMinute(appId, stateExecutionId, StateType.APP_DYNAMICS);
    assertThat(minute).isEqualTo(dataCollectionMinute + 1);
  }
  @Test
  @Category(UnitTests.class)
  public void testGetLastSuccessfulWorkflowExecutionIdWithDataInCaseOfNoData() {
    List<String> lastSuccessfulWorkflowExecutionIds = Lists.newArrayList(generateUuid(), generateUuid());
    NewRelicMetricDataRecord newRelicMetricDataRecord =
        NewRelicMetricDataRecord.builder()
            .level(ClusterLevel.HF)
            .stateType(StateType.NEW_RELIC)
            .serviceId(serviceId)
            .workflowExecutionId(lastSuccessfulWorkflowExecutionIds.get(0))
            .stateExecutionId(generateUuid())
            .cvConfigId(generateUuid())
            .build();
    wingsPersistence.save(newRelicMetricDataRecord);

    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .level(ClusterLevel.H0)
                              .stateType(StateType.NEW_RELIC)
                              .serviceId(serviceId)
                              .workflowExecutionId(lastSuccessfulWorkflowExecutionIds.get(1))
                              .stateExecutionId(generateUuid())
                              .cvConfigId(generateUuid())
                              .build());
    doReturn(lastSuccessfulWorkflowExecutionIds)
        .when(timeSeriesAnalysisService)
        .getLastSuccessfulWorkflowExecutionIds(anyString(), anyString(), anyString());
    String workflowId = generateUuid();
    String workflowExecutionIdsWithData = timeSeriesAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
        StateType.NEW_RELIC, appId, workflowId, serviceId);
    assertThat(workflowExecutionIdsWithData).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetLastSuccessfulWorkflowExecutionIdWithDataInCaseData() {
    List<String> lastSuccessfulWorkflowExecutionIds = Lists.newArrayList(generateUuid(), generateUuid());
    NewRelicMetricDataRecord newRelicMetricDataRecord =
        NewRelicMetricDataRecord.builder()
            .level(ClusterLevel.L1)
            .stateType(StateType.NEW_RELIC)
            .serviceId(serviceId)
            .workflowExecutionId(lastSuccessfulWorkflowExecutionIds.get(0))
            .stateExecutionId(generateUuid())
            .cvConfigId(generateUuid())
            .build();
    wingsPersistence.save(newRelicMetricDataRecord);

    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .level(ClusterLevel.H0)
                              .stateType(StateType.NEW_RELIC)
                              .serviceId(serviceId)
                              .workflowExecutionId(lastSuccessfulWorkflowExecutionIds.get(1))
                              .stateExecutionId(generateUuid())
                              .cvConfigId(generateUuid())
                              .build());
    doReturn(lastSuccessfulWorkflowExecutionIds)
        .when(timeSeriesAnalysisService)
        .getLastSuccessfulWorkflowExecutionIds(anyString(), anyString(), anyString());
    String workflowId = generateUuid();
    String workflowExecutionIdsWithData = timeSeriesAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
        StateType.NEW_RELIC, appId, workflowId, serviceId);
    assertThat(workflowExecutionIdsWithData).isEqualTo(lastSuccessfulWorkflowExecutionIds.get(0));
  }

  @Test
  @Category(UnitTests.class)
  public void testGetLastSuccessfulWorkflowExecutionIdWithDataIfNoRecords() {
    List<String> lastSuccessfulWorkflowExecutionIds = Lists.newArrayList(generateUuid(), generateUuid());
    doReturn(lastSuccessfulWorkflowExecutionIds)
        .when(timeSeriesAnalysisService)
        .getLastSuccessfulWorkflowExecutionIds(anyString(), anyString(), anyString());
    String workflowId = generateUuid();
    String workflowExecutionIdsWithData = timeSeriesAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
        StateType.NEW_RELIC, appId, workflowId, serviceId);
    assertThat(workflowExecutionIdsWithData).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetricTemplateWithCategorizedThresholdsWhenOnlyDefaultThresholdsAreDefinedForNewRelic() {
    Map<String, Map<String, TimeSeriesMetricDefinition>> metricTemplateWithCategorizedThresholds =
        timeSeriesAnalysisService.getMetricTemplateWithCategorizedThresholds(appId, StateType.NEW_RELIC,
            stateExecutionId, serviceId, cvConfigId, NewRelicMetricDataRecord.DEFAULT_GROUP_NAME, Version.PROD);
    // if this becomes unmaintainable. Please remove this test and find some other way to test this method.
    @Language("JSON")
    String expectedJson = "{\n"
        + "  \"default\": {\n"
        + "    \"averageResponseTime\": {\n"
        + "      \"metricName\": \"averageResponseTime\",\n"
        + "      \"metricType\": \"RESP_TIME\",\n"
        + "      \"categorizedThresholds\": {\n"
        + "        \"DEFAULT\": [\n"
        + "          {\n"
        + "            \"thresholdType\": \"ALERT_WHEN_HIGHER\",\n"
        + "            \"comparisonType\": \"RATIO\",\n"
        + "            \"ml\": 0.2\n"
        + "          },\n"
        + "          {\n"
        + "            \"thresholdType\": \"ALERT_WHEN_HIGHER\",\n"
        + "            \"comparisonType\": \"DELTA\",\n"
        + "            \"ml\": 20\n"
        + "          }\n"
        + "        ]\n"
        + "      },\n"
        + "      \"thresholds\": [\n"
        + "        {\n"
        + "          \"thresholdType\": \"ALERT_WHEN_HIGHER\",\n"
        + "          \"comparisonType\": \"RATIO\",\n"
        + "          \"ml\": 0.2\n"
        + "        },\n"
        + "        {\n"
        + "          \"thresholdType\": \"ALERT_WHEN_HIGHER\",\n"
        + "          \"comparisonType\": \"DELTA\",\n"
        + "          \"ml\": 20\n"
        + "        }\n"
        + "      ]\n"
        + "    },\n"
        + "    \"requestsPerMinute\": {\n"
        + "      \"metricName\": \"requestsPerMinute\",\n"
        + "      \"metricType\": \"THROUGHPUT\",\n"
        + "      \"categorizedThresholds\": {\n"
        + "        \"DEFAULT\": [\n"
        + "          {\n"
        + "            \"thresholdType\": \"ALERT_WHEN_LOWER\",\n"
        + "            \"comparisonType\": \"RATIO\",\n"
        + "            \"ml\": 0.2\n"
        + "          },\n"
        + "          {\n"
        + "            \"thresholdType\": \"ALERT_WHEN_LOWER\",\n"
        + "            \"comparisonType\": \"DELTA\",\n"
        + "            \"ml\": 20\n"
        + "          }\n"
        + "        ]\n"
        + "      },\n"
        + "      \"thresholds\": [\n"
        + "        {\n"
        + "          \"thresholdType\": \"ALERT_WHEN_LOWER\",\n"
        + "          \"comparisonType\": \"RATIO\",\n"
        + "          \"ml\": 0.2\n"
        + "        },\n"
        + "        {\n"
        + "          \"thresholdType\": \"ALERT_WHEN_LOWER\",\n"
        + "          \"comparisonType\": \"DELTA\",\n"
        + "          \"ml\": 20\n"
        + "        }\n"
        + "      ]\n"
        + "    },\n"
        + "    \"error\": {\n"
        + "      \"metricName\": \"error\",\n"
        + "      \"metricType\": \"ERROR\",\n"
        + "      \"categorizedThresholds\": {\n"
        + "        \"DEFAULT\": [\n"
        + "          {\n"
        + "            \"thresholdType\": \"ALERT_WHEN_HIGHER\",\n"
        + "            \"comparisonType\": \"RATIO\",\n"
        + "            \"ml\": 0.01\n"
        + "          },\n"
        + "          {\n"
        + "            \"thresholdType\": \"ALERT_WHEN_HIGHER\",\n"
        + "            \"comparisonType\": \"DELTA\",\n"
        + "            \"ml\": 0.01\n"
        + "          }\n"
        + "        ]\n"
        + "      },\n"
        + "      \"thresholds\": [\n"
        + "        {\n"
        + "          \"thresholdType\": \"ALERT_WHEN_HIGHER\",\n"
        + "          \"comparisonType\": \"RATIO\",\n"
        + "          \"ml\": 0.01\n"
        + "        },\n"
        + "        {\n"
        + "          \"thresholdType\": \"ALERT_WHEN_HIGHER\",\n"
        + "          \"comparisonType\": \"DELTA\",\n"
        + "          \"ml\": 0.01\n"
        + "        }\n"
        + "      ]\n"
        + "    },\n"
        + "    \"apdexScore\": {\n"
        + "      \"metricName\": \"apdexScore\",\n"
        + "      \"metricType\": \"APDEX\",\n"
        + "      \"categorizedThresholds\": {\n"
        + "        \"DEFAULT\": [\n"
        + "          {\n"
        + "            \"thresholdType\": \"ALERT_WHEN_LOWER\",\n"
        + "            \"comparisonType\": \"RATIO\",\n"
        + "            \"ml\": 0.2\n"
        + "          },\n"
        + "          {\n"
        + "            \"thresholdType\": \"ALERT_WHEN_LOWER\",\n"
        + "            \"comparisonType\": \"DELTA\",\n"
        + "            \"ml\": 0.2\n"
        + "          }\n"
        + "        ]\n"
        + "      },\n"
        + "      \"thresholds\": [\n"
        + "        {\n"
        + "          \"thresholdType\": \"ALERT_WHEN_LOWER\",\n"
        + "          \"comparisonType\": \"RATIO\",\n"
        + "          \"ml\": 0.2\n"
        + "        },\n"
        + "        {\n"
        + "          \"thresholdType\": \"ALERT_WHEN_LOWER\",\n"
        + "          \"comparisonType\": \"DELTA\",\n"
        + "          \"ml\": 0.2\n"
        + "        }\n"
        + "      ]\n"
        + "    }\n"
        + "  }\n"
        + "}";

    JsonParser parser = new JsonParser();
    // if this becomes unmaintainable. Please remove this test and find some other way to test this method.
    assertThat(parser.parse(JsonUtils.asJson(metricTemplateWithCategorizedThresholds)))
        .isEqualTo(parser.parse(expectedJson));
  }
  @Test
  @Category(UnitTests.class)
  public void testSaveMetricTemplates() {
    TimeSeriesMetricDefinition timeSeriesMetricDefinition =
        TimeSeriesMetricDefinition.builder().metricName("test").build();
    Map<String, TimeSeriesMetricDefinition> timeSeriesMetricDefinitionMap = new HashMap<>();
    timeSeriesMetricDefinitionMap.put(timeSeriesMetricDefinition.getMetricName(), timeSeriesMetricDefinition);
    timeSeriesAnalysisService.saveMetricTemplates(
        appId, StateType.NEW_RELIC, stateExecutionId, timeSeriesMetricDefinitionMap);
    TimeSeriesMetricTemplates timeSeriesMetricTemplates = wingsPersistence.createQuery(TimeSeriesMetricTemplates.class)
                                                              .filter("stateExecutionId", stateExecutionId)
                                                              .get();
    assertThat(timeSeriesMetricTemplates.getMetricTemplates()).isEqualTo(timeSeriesMetricDefinitionMap);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetricTemplates() {
    TimeSeriesMetricDefinition timeSeriesMetricDefinition =
        TimeSeriesMetricDefinition.builder().metricName("test").build();
    Map<String, TimeSeriesMetricDefinition> timeSeriesMetricDefinitionMap = new HashMap<>();
    timeSeriesMetricDefinitionMap.put(timeSeriesMetricDefinition.getMetricName(), timeSeriesMetricDefinition);
    TimeSeriesMetricTemplates timeSeriesMetricTemplates = TimeSeriesMetricTemplates.builder()
                                                              .stateExecutionId(stateExecutionId)
                                                              .cvConfigId(cvConfigId)
                                                              .stateType(StateType.NEW_RELIC)
                                                              .metricTemplates(timeSeriesMetricDefinitionMap)
                                                              .build();

    wingsPersistence.save(timeSeriesMetricTemplates);
    Map<String, TimeSeriesMetricDefinition> timeSeriesMetricDefinitionResult =
        timeSeriesAnalysisService.getMetricTemplates(accountId, StateType.NEW_RELIC, stateExecutionId, cvConfigId);

    assertThat(timeSeriesMetricDefinitionResult).isEqualTo(timeSeriesMetricDefinitionMap);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetricGroups() {
    TimeSeriesMlAnalysisGroupInfo timeSeriesMlAnalysisGroupInfo =
        TimeSeriesMlAnalysisGroupInfo.builder()
            .groupName("default")
            .mlAnalysisType(TimeSeriesMlAnalysisType.PREDICTIVE)
            .dependencyPath("path")
            .build();
    Map<String, TimeSeriesMlAnalysisGroupInfo> groupMap = new HashMap<>();
    groupMap.put(timeSeriesMlAnalysisGroupInfo.getGroupName(), timeSeriesMlAnalysisGroupInfo);
    TimeSeriesMetricGroup timeSeriesMetricGroup = TimeSeriesMetricGroup.builder()
                                                      .stateExecutionId(stateExecutionId)
                                                      .stateType(StateType.NEW_RELIC)
                                                      .appId(appId)
                                                      .groups(groupMap)
                                                      .build();
    wingsPersistence.save(timeSeriesMetricGroup);
    Map<String, TimeSeriesMlAnalysisGroupInfo> results =
        timeSeriesAnalysisService.getMetricGroups(appId, stateExecutionId);
    assertThat(results).isEqualTo(groupMap);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetricGroupsIfNoTimeSeriesMetricGroup() {
    Map<String, TimeSeriesMlAnalysisGroupInfo> defaultMap =
        new ImmutableMap.Builder<String, TimeSeriesMlAnalysisGroupInfo>()
            .put(DEFAULT_GROUP_NAME,
                TimeSeriesMlAnalysisGroupInfo.builder()
                    .groupName(DEFAULT_GROUP_NAME)
                    .dependencyPath(DEFAULT_GROUP_NAME)
                    .mlAnalysisType(TimeSeriesMlAnalysisType.COMPARATIVE)
                    .build())
            .build();
    Map<String, TimeSeriesMlAnalysisGroupInfo> results =
        timeSeriesAnalysisService.getMetricGroups(appId, stateExecutionId);
    assertThat(results).isEqualTo(defaultMap);
  }

  @Test
  @Category(UnitTests.class)
  public void testBumpCollectionMinuteToProcess() {
    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .stateType(StateType.NEW_RELIC)
                              .level(ClusterLevel.H0)
                              .stateExecutionId(stateExecutionId)
                              .appId(appId)
                              .dataCollectionMinute(currentEpochMinute)
                              .build());
    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .stateType(StateType.NEW_RELIC)
                              .level(ClusterLevel.H0)
                              .stateExecutionId(stateExecutionId)
                              .appId(appId)
                              .dataCollectionMinute(currentEpochMinute - 1)
                              .build());
    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .stateType(StateType.NEW_RELIC)
                              .level(ClusterLevel.H0)
                              .stateExecutionId(stateExecutionId)
                              .appId(appId)
                              .dataCollectionMinute(currentEpochMinute - 10)
                              .build());
    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .stateType(StateType.NEW_RELIC)
                              .level(ClusterLevel.H0)
                              .stateExecutionId(stateExecutionId)
                              .appId(appId)
                              .dataCollectionMinute(currentEpochMinute + 1)
                              .build());
    timeSeriesAnalysisService.bumpCollectionMinuteToProcess(
        appId, stateExecutionId, workflowExecutionId, DEFAULT_GROUP_NAME, currentEpochMinute, accountId, false);
    PageRequest<NewRelicMetricDataRecord> pageRequest =
        aPageRequest()
            .withLimit(UNLIMITED)
            .addFilter(NewRelicMetricDataRecordKeys.stateExecutionId, Operator.EQ, stateExecutionId)
            .addFilter(NewRelicMetricDataRecordKeys.groupName, Operator.EQ, DEFAULT_GROUP_NAME)
            .addFilter(NewRelicMetricDataRecordKeys.dataCollectionMinute, Operator.LT_EQ, currentEpochMinute)
            .addOrder(NewRelicMetricDataRecordKeys.dataCollectionMinute, OrderType.DESC)
            .build();
    final PageResponse<NewRelicMetricDataRecord> dataRecords =
        wingsPersistence.query(NewRelicMetricDataRecord.class, pageRequest, excludeAuthority);
    List<NewRelicMetricDataRecord> newRelicMetricDataRecords = dataRecords.getResponse();
    assertThat(newRelicMetricDataRecords.size()).isEqualTo(3);
    newRelicMetricDataRecords.forEach(record -> assertThat(record.getLevel()).isEqualTo(ClusterLevel.HF));
  }
  @Test
  @Category(UnitTests.class)
  public void testGetAnalysisMinuteForLastHeartbeatRecord() {
    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .stateType(StateType.NEW_RELIC)
                              .serviceId(serviceId)
                              .level(ClusterLevel.L1)
                              .stateExecutionId(stateExecutionId)
                              .appId(appId)
                              .dataCollectionMinute(currentEpochMinute)
                              .build());
    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .stateType(StateType.NEW_RELIC)
                              .serviceId(serviceId)
                              .level(ClusterLevel.HF)
                              .stateExecutionId(stateExecutionId)
                              .appId(appId)
                              .dataCollectionMinute(currentEpochMinute - 1)
                              .build());
    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .stateType(StateType.NEW_RELIC)
                              .serviceId(serviceId)
                              .level(ClusterLevel.H0)
                              .stateExecutionId(stateExecutionId)
                              .appId(appId)
                              .dataCollectionMinute(currentEpochMinute - 10)
                              .build());
    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .stateType(StateType.NEW_RELIC)
                              .serviceId(serviceId)
                              .level(ClusterLevel.H0)
                              .stateExecutionId(stateExecutionId)
                              .appId(appId)
                              .dataCollectionMinute(currentEpochMinute + 1)
                              .build());
    NewRelicMetricDataRecord newRelicMetricDataRecord = timeSeriesAnalysisService.getAnalysisMinute(
        StateType.NEW_RELIC, appId, stateExecutionId, workflowExecutionId, serviceId, DEFAULT_GROUP_NAME);
    assertThat(newRelicMetricDataRecord.getLevel()).isEqualTo(ClusterLevel.H0);
    assertThat(newRelicMetricDataRecord.getDataCollectionMinute()).isEqualTo(currentEpochMinute + 1);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetAnalysisMinuteIfNoHeartbeatAvailable() {
    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .stateType(StateType.NEW_RELIC)
                              .serviceId(serviceId)
                              .level(ClusterLevel.L1)
                              .stateExecutionId(stateExecutionId)
                              .appId(appId)
                              .dataCollectionMinute(currentEpochMinute)
                              .build());
    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .stateType(StateType.NEW_RELIC)
                              .serviceId(serviceId)
                              .level(ClusterLevel.HF)
                              .stateExecutionId(stateExecutionId)
                              .appId(appId)
                              .dataCollectionMinute(currentEpochMinute - 1)
                              .build());

    NewRelicMetricDataRecord newRelicMetricDataRecord = timeSeriesAnalysisService.getAnalysisMinute(
        StateType.NEW_RELIC, appId, stateExecutionId, workflowExecutionId, serviceId, DEFAULT_GROUP_NAME);
    assertThat(newRelicMetricDataRecord).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetHistoricalAnalysis() {
    TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord = TimeSeriesMLAnalysisRecord.builder().build();
    timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
    timeSeriesMLAnalysisRecord.setAnalysisMinute((int) (currentEpochMinute - TimeUnit.DAYS.toMinutes(7)));
    timeSeriesMLAnalysisRecord.setTag("tag");
    timeSeriesMLAnalysisRecord.setStateExecutionId(stateExecutionId);
    timeSeriesMLAnalysisRecord.setAppId(appId);
    wingsPersistence.save(timeSeriesMLAnalysisRecord);
    List<TimeSeriesMLAnalysisRecord> records = timeSeriesAnalysisService.getHistoricalAnalysis(
        accountId, appId, serviceId, cvConfigId, currentEpochMinute, "tag");
    assertThat(records.size()).isEqualTo(1);
    assertThat(records.get(0).getAnalysisMinute()).isEqualTo(timeSeriesMLAnalysisRecord.getAnalysisMinute());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetPreviousAnomaliesWithFiltering() {
    Map<String, Map<String, List<TimeSeriesMLHostSummary>>> anomalies = new HashMap<>();
    List<Double> testData = new ArrayList<>();
    testData.add(1.0);
    testData.add(2.0);
    TimeSeriesMLHostSummary timeSeriesMLHostSummary =
        TimeSeriesMLHostSummary.builder().test_data(testData).risk(RiskLevel.HIGH.getRisk()).build();
    Map<String, List<TimeSeriesMLHostSummary>> summaryMap = new HashMap<>();
    summaryMap.put("metric", Lists.newArrayList(timeSeriesMLHostSummary));
    anomalies.put("txn", summaryMap);
    TimeSeriesAnomaliesRecord timeSeriesAnomaliesRecord =
        TimeSeriesAnomaliesRecord.builder().cvConfigId(cvConfigId).tag("tag").anomalies(anomalies).build();
    timeSeriesAnomaliesRecord.compressAnomalies();
    wingsPersistence.save(timeSeriesAnomaliesRecord);
    Map<String, List<String>> metrics = new HashMap<>();
    metrics.put("txn", Lists.newArrayList("metric"));
    TimeSeriesAnomaliesRecord result =
        timeSeriesAnalysisService.getPreviousAnomalies(appId, cvConfigId, metrics, "tag");
    assertThat(result).isNotNull();
    assertThat(result.getAnomalies()).isEqualTo(anomalies);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetPreviousAnomaliesWhenNoData() {
    Map<String, List<String>> metrics = new HashMap<>();
    metrics.put("txn", Lists.newArrayList("metric"));
    TimeSeriesAnomaliesRecord result =
        timeSeriesAnalysisService.getPreviousAnomalies(appId, cvConfigId, metrics, "tag");
    assertThat(result).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetPreviousAnomaliesWhenMetricsIsEmpty() {
    Map<String, Map<String, List<TimeSeriesMLHostSummary>>> anomalies = new HashMap<>();
    List<Double> testData = new ArrayList<>();
    testData.add(1.0);
    testData.add(2.0);
    TimeSeriesMLHostSummary timeSeriesMLHostSummary =
        TimeSeriesMLHostSummary.builder().test_data(testData).risk(RiskLevel.HIGH.getRisk()).build();
    Map<String, List<TimeSeriesMLHostSummary>> summaryMap = new HashMap<>();
    summaryMap.put("metric", Lists.newArrayList(timeSeriesMLHostSummary));
    anomalies.put("txn", summaryMap);
    TimeSeriesAnomaliesRecord timeSeriesAnomaliesRecord =
        TimeSeriesAnomaliesRecord.builder().cvConfigId(cvConfigId).tag("tag").anomalies(anomalies).build();
    timeSeriesAnomaliesRecord.compressAnomalies();
    wingsPersistence.save(timeSeriesAnomaliesRecord);
    TimeSeriesAnomaliesRecord result =
        timeSeriesAnalysisService.getPreviousAnomalies(appId, cvConfigId, new HashMap<>(), "tag");
    assertThat(result.getAnomalies()).isEqualTo(anomalies);
  }
}
