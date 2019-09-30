package io.harness.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rest.RestResponse.Builder.aRestResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import io.harness.VerificationBaseTest;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SortOrder.OrderType;
import io.harness.category.element.UnitTests;
import io.harness.entities.TimeSeriesAnomaliesRecord;
import io.harness.entities.TimeSeriesAnomaliesRecord.TimeSeriesAnomaliesRecordKeys;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.TimeSeriesDataRecord;
import software.wings.service.impl.analysis.MetricAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLHostSummary;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Slf4j
public class TimeSeriesAnalysisServiceTest extends VerificationBaseTest {
  private String cvConfigId;
  private String serviceId;
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowExecutionId;
  private Random randomizer;
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

    MockitoAnnotations.initMocks(this);
    when(managerClientHelper.callManagerWithRetry(any())).thenReturn(aRestResponse().withResource(false).build());

    FieldUtils.writeField(timeSeriesAnalysisService, "managerClientHelper", managerClientHelper, true);
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
  public void testSetTagInAnomRecords() {
    Map<String, Map<String, List<TimeSeriesMLHostSummary>>> anomMap = new HashMap<>();
    anomMap.put("txn1", new HashMap<>());
    MetricAnalysisRecord analysisRecord = TimeSeriesMLAnalysisRecord.builder().build();
    analysisRecord.setTransactions(new HashMap<>());
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
}
