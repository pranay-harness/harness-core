package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTask.Status;
import software.wings.beans.Environment;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.metrics.MetricType;
import software.wings.metrics.RiskLevel;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates;
import software.wings.service.impl.newrelic.MetricAnalysisExecutionData;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.prometheus.PrometheusDataCollectionInfo;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.waitnotify.NotifyResponseData;

import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by rsingh on 3/22/18.
 */
public class PrometheusStateTest extends APMStateVerificationTestBase {
  @Inject private MetricDataAnalysisService metricDataAnalysisService;

  private PrometheusState prometheusState;
  private List<TimeSeries> timeSeriesToCollect = Lists.newArrayList(TimeSeries.builder()
                                                                        .txnName("Transaction_Name")
                                                                        .metricName("metric_name")
                                                                        .url("url_$hostName_$startTime_$endTime")
                                                                        .metricType(MetricType.RESP_TIME)
                                                                        .build());

  @Before
  public void setup() {
    setupCommon();
    MockitoAnnotations.initMocks(this);
    setupCommonMocks();

    prometheusState = new PrometheusState("PrometheusState");
    prometheusState.setTimeSeriesToAnalyze(timeSeriesToCollect);
    prometheusState.setTimeDuration("15");
    setInternalState(prometheusState, "appService", appService);
    setInternalState(prometheusState, "configuration", configuration);
    setInternalState(prometheusState, "metricAnalysisService", metricDataAnalysisService);
    setInternalState(prometheusState, "settingsService", settingsService);
    setInternalState(prometheusState, "waitNotifyEngine", waitNotifyEngine);
    setInternalState(prometheusState, "delegateService", delegateService);
    setInternalState(prometheusState, "jobScheduler", jobScheduler);
    setInternalState(prometheusState, "secretManager", secretManager);
    setInternalState(prometheusState, "workflowExecutionService", workflowExecutionService);
    setInternalState(prometheusState, "continuousVerificationService", continuousVerificationService);
    setInternalState(prometheusState, "workflowExecutionBaselineService", workflowExecutionBaselineService);
    setInternalState(prometheusState, "featureFlagService", featureFlagService);
  }

  @Test
  public void testDefaultComparsionStrategy() {
    PrometheusState splunkState = new PrometheusState("PrometheusState");
    assertEquals(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS, splunkState.getComparisonStrategy());
  }

  @Test
  public void noTestNodes() {
    PrometheusState spyState = spy(prometheusState);
    doReturn(Collections.emptyMap()).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.emptyMap()).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.SUCCESS, response.getExecutionStatus());
    assertEquals("Could not find nodes to analyze!", response.getErrorMessage());

    List<NewRelicMetricAnalysisRecord> metricAnalysisRecords =
        metricDataAnalysisService.getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId);
    assertEquals(1, metricAnalysisRecords.size());

    NewRelicMetricAnalysisRecord metricsAnalysis = metricAnalysisRecords.get(0);
    assertEquals(stateExecutionId, metricsAnalysis.getStateExecutionId());
    assertEquals(workflowExecutionId, metricsAnalysis.getWorkflowExecutionId());
    assertEquals(workflowId, metricsAnalysis.getWorkflowId());
    assertEquals(RiskLevel.NA, metricsAnalysis.getRiskLevel());
    assertEquals("Could not find nodes to analyze!", metricsAnalysis.getMessage());
    assertNull(metricsAnalysis.getMetricAnalyses());
  }

  @Test
  public void noControlNodesCompareWithCurrent() {
    prometheusState.setComparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_CURRENT.name());
    PrometheusState spyState = spy(prometheusState);
    doReturn(Collections.singletonMap("some-host", DEFAULT_GROUP_NAME))
        .when(spyState)
        .getCanaryNewHostNames(executionContext);
    doReturn(Collections.emptyMap()).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.SUCCESS, response.getExecutionStatus());
    assertEquals("Skipping analysis due to lack of baseline data (First time deployment or Last phase).",
        response.getErrorMessage());

    List<NewRelicMetricAnalysisRecord> metricAnalysisRecords =
        metricDataAnalysisService.getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId);
    assertEquals(1, metricAnalysisRecords.size());

    NewRelicMetricAnalysisRecord metricsAnalysis = metricAnalysisRecords.get(0);
    assertEquals(stateExecutionId, metricsAnalysis.getStateExecutionId());
    assertEquals(workflowExecutionId, metricsAnalysis.getWorkflowExecutionId());
    assertEquals(workflowId, metricsAnalysis.getWorkflowId());
    assertEquals(RiskLevel.NA, metricsAnalysis.getRiskLevel());
    assertEquals("Skipping analysis due to lack of baseline data (First time deployment or Last phase).",
        metricsAnalysis.getMessage());
    assertNull(metricsAnalysis.getMetricAnalyses());
  }

  @Test
  public void compareWithCurrentSameTestAndControlNodes() {
    prometheusState.setComparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_CURRENT.name());
    PrometheusState spyState = spy(prometheusState);
    doReturn(new HashMap<>(Collections.singletonMap("some-host", DEFAULT_GROUP_NAME)))
        .when(spyState)
        .getCanaryNewHostNames(executionContext);
    doReturn(new HashMap<>(Collections.singletonMap("some-host", DEFAULT_GROUP_NAME)))
        .when(spyState)
        .getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.SUCCESS, response.getExecutionStatus());
    assertEquals("Skipping analysis due to lack of baseline data (First time deployment or Last phase).",
        response.getErrorMessage());

    List<NewRelicMetricAnalysisRecord> metricAnalysisRecords =
        metricDataAnalysisService.getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId);
    assertEquals(1, metricAnalysisRecords.size());

    NewRelicMetricAnalysisRecord metricsAnalysis = metricAnalysisRecords.get(0);
    assertEquals(stateExecutionId, metricsAnalysis.getStateExecutionId());
    assertEquals(workflowExecutionId, metricsAnalysis.getWorkflowExecutionId());
    assertEquals(workflowId, metricsAnalysis.getWorkflowId());
    assertEquals(RiskLevel.NA, metricsAnalysis.getRiskLevel());
    assertEquals("Skipping analysis due to lack of baseline data (First time deployment or Last phase).",
        metricsAnalysis.getMessage());
    assertNull(metricsAnalysis.getMetricAnalyses());
  }

  @Test
  public void testTriggerCollection() throws ParseException {
    assertEquals(0, wingsPersistence.createQuery(DelegateTask.class).count());
    PrometheusConfig prometheusConfig = PrometheusConfig.builder().accountId(accountId).url("prometheus-url").build();
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withName("prometheus-config")
                                            .withValue(prometheusConfig)
                                            .build();
    wingsPersistence.save(settingAttribute);
    prometheusState.setAnalysisServerConfigId(settingAttribute.getUuid());
    PrometheusState spyState = spy(prometheusState);
    doReturn(Collections.singletonMap("test", DEFAULT_GROUP_NAME))
        .when(spyState)
        .getCanaryNewHostNames(executionContext);
    doReturn(Collections.singletonMap("control", DEFAULT_GROUP_NAME))
        .when(spyState)
        .getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);
    when(workflowStandardParams.getEnv())
        .thenReturn(Environment.Builder.anEnvironment().withUuid(UUID.randomUUID().toString()).build());
    when(executionContext.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.RUNNING, response.getExecutionStatus());
    assertEquals(
        "No baseline was set for the workflow. Workflow running with auto baseline. No previous execution found. This will be the baseline run",
        response.getErrorMessage());

    List<DelegateTask> tasks = wingsPersistence.createQuery(DelegateTask.class).asList();
    assertEquals(1, tasks.size());
    DelegateTask task = tasks.get(0);
    assertEquals(TaskType.PROMETHEUS_METRIC_DATA_COLLECTION_TASK.name(), task.getTaskType());

    PrometheusDataCollectionInfo expectedCollectionInfo =
        PrometheusDataCollectionInfo.builder()
            .prometheusConfig(prometheusConfig)
            .applicationId(appId)
            .stateExecutionId(stateExecutionId)
            .workflowId(workflowId)
            .workflowExecutionId(workflowExecutionId)
            .serviceId(serviceId)
            .startTime(0)
            .collectionTime(Integer.parseInt(prometheusState.getTimeDuration()))
            .timeSeriesToCollect(timeSeriesToCollect)
            .dataCollectionMinute(0)
            .hosts(Collections.singletonMap("test", DEFAULT_GROUP_NAME))
            .analysisComparisonStrategy(prometheusState.getComparisonStrategy())
            .build();

    final PrometheusDataCollectionInfo actualCollectionInfo = (PrometheusDataCollectionInfo) task.getParameters()[0];
    expectedCollectionInfo.setStartTime(actualCollectionInfo.getStartTime());
    assertEquals(expectedCollectionInfo, actualCollectionInfo);
    assertEquals(accountId, task.getAccountId());
    assertEquals(Status.QUEUED, task.getStatus());
    assertEquals(appId, task.getAppId());
    Map<Long,
        LinkedHashMap<String,
            LinkedHashMap<String,
                LinkedHashMap<String, LinkedHashMap<String, List<ContinuousVerificationExecutionMetaData>>>>>>
        cvExecutionMetaData =
            continuousVerificationService.getCVExecutionMetaData(accountId, 1519200000000L, 1519200000001L, user);
    assertNotNull(cvExecutionMetaData);
    ContinuousVerificationExecutionMetaData continuousVerificationExecutionMetaData1 =
        cvExecutionMetaData.get(1519171200000L)
            .get("dummy artifact")
            .get("dummy env/dummy workflow")
            .values()
            .iterator()
            .next()
            .get("BASIC")
            .get(0);
    assertEquals(continuousVerificationExecutionMetaData1.getAccountId(), accountId);
    assertEquals(continuousVerificationExecutionMetaData1.getArtifactName(), "dummy artifact");
    assertEquals(ExecutionStatus.RUNNING, continuousVerificationExecutionMetaData1.getExecutionStatus());

    // assert metric templates
    assertEquals(1, wingsPersistence.createQuery(TimeSeriesMetricTemplates.class).count());
    Map<String, TimeSeriesMetricDefinition> metricTemplates =
        metricDataAnalysisService.getMetricTemplates(appId, StateType.PROMETHEUS, stateExecutionId);
    assertEquals(1, metricTemplates.size());
    assertEquals("metric_name", metricTemplates.entrySet().iterator().next().getKey());
    assertNotNull(metricTemplates.get("metric_name"));
    assertEquals(
        TimeSeriesMetricDefinition.builder().metricName("metric_name").metricType(MetricType.RESP_TIME).build(),
        metricTemplates.get("metric_name"));

    MetricAnalysisExecutionData metricAnalysisExecutionData = MetricAnalysisExecutionData.builder().build();
    MetricDataAnalysisResponse metricDataAnalysisResponse =
        MetricDataAnalysisResponse.builder().stateExecutionData(metricAnalysisExecutionData).build();
    metricDataAnalysisResponse.setExecutionStatus(ExecutionStatus.FAILED);
    Map<String, NotifyResponseData> responseMap = new HashMap<>();
    responseMap.put("somekey", metricDataAnalysisResponse);
    prometheusState.handleAsyncResponse(executionContext, responseMap);

    cvExecutionMetaData =
        continuousVerificationService.getCVExecutionMetaData(accountId, 1519200000000L, 1519200000001L, user);
    continuousVerificationExecutionMetaData1 = cvExecutionMetaData.get(1519171200000L)
                                                   .get("dummy artifact")
                                                   .get("dummy env/dummy workflow")
                                                   .values()
                                                   .iterator()
                                                   .next()
                                                   .get("BASIC")
                                                   .get(0);
    assertEquals(ExecutionStatus.FAILED, continuousVerificationExecutionMetaData1.getExecutionStatus());
  }

  @Test
  public void testEmptyMetricDefinitions() throws ParseException {
    prometheusState.setTimeSeriesToAnalyze(Collections.emptyList());
    Map<String, String> validateFields = prometheusState.validateFields();
    assertEquals(1, validateFields.size());
    assertEquals("No metrics given to analyze.", validateFields.entrySet().iterator().next().getValue());
  }

  @Test
  public void testNoStartTimePlaceHolder() throws ParseException {
    prometheusState.setTimeSeriesToAnalyze(Lists.newArrayList(TimeSeries.builder()
                                                                  .txnName("Transaction_Name")
                                                                  .metricName("metric_name")
                                                                  .url("url_$hostName_$endTime")
                                                                  .metricType(MetricType.RESP_TIME)
                                                                  .build()));
    Map<String, String> validateFields = prometheusState.validateFields();
    assertEquals(1, validateFields.size());
    assertTrue(validateFields.containsKey("Invalid url for txn: Transaction_Name, metric : metric_name"));
    assertEquals("[$startTime] are not present in the url.",
        validateFields.get("Invalid url for txn: Transaction_Name, metric : metric_name"));
  }

  @Test
  public void testNoEndTimePlaceHolder() throws ParseException {
    prometheusState.setTimeSeriesToAnalyze(Lists.newArrayList(TimeSeries.builder()
                                                                  .txnName("Transaction_Name")
                                                                  .metricName("metric_name")
                                                                  .url("url_$hostName_$startTime")
                                                                  .metricType(MetricType.RESP_TIME)
                                                                  .build()));
    Map<String, String> validateFields = prometheusState.validateFields();
    assertEquals(1, validateFields.size());
    assertTrue(validateFields.containsKey("Invalid url for txn: Transaction_Name, metric : metric_name"));
    assertEquals("[$endTime] are not present in the url.",
        validateFields.get("Invalid url for txn: Transaction_Name, metric : metric_name"));
  }

  @Test
  public void testNoHostNamePlaceHolder() throws ParseException {
    prometheusState.setTimeSeriesToAnalyze(Lists.newArrayList(TimeSeries.builder()
                                                                  .txnName("Transaction_Name")
                                                                  .metricName("metric_name")
                                                                  .url("url_$startTime_$endTime")
                                                                  .metricType(MetricType.RESP_TIME)
                                                                  .build()));
    Map<String, String> validateFields = prometheusState.validateFields();
    assertEquals(1, validateFields.size());
    assertTrue(validateFields.containsKey("Invalid url for txn: Transaction_Name, metric : metric_name"));
    assertEquals("[$hostName] are not present in the url.",
        validateFields.get("Invalid url for txn: Transaction_Name, metric : metric_name"));
  }

  @Test
  public void testAllMissingPlaceHolder() throws ParseException {
    prometheusState.setTimeSeriesToAnalyze(Lists.newArrayList(TimeSeries.builder()
                                                                  .txnName("Transaction_Name")
                                                                  .metricName("metric_name")
                                                                  .url("url")
                                                                  .metricType(MetricType.RESP_TIME)
                                                                  .build()));
    Map<String, String> validateFields = prometheusState.validateFields();
    assertEquals(1, validateFields.size());
    assertTrue(validateFields.containsKey("Invalid url for txn: Transaction_Name, metric : metric_name"));
    assertEquals("[$hostName, $startTime, $endTime] are not present in the url.",
        validateFields.get("Invalid url for txn: Transaction_Name, metric : metric_name"));
  }

  @Test
  public void testDuplicatMetricNames() throws ParseException {
    prometheusState.setTimeSeriesToAnalyze(Lists.newArrayList(TimeSeries.builder()
                                                                  .txnName("Transaction_Name")
                                                                  .metricName("metric_name")
                                                                  .url("url_$hostName_$startTime_$endTime")
                                                                  .metricType(MetricType.RESP_TIME)
                                                                  .build(),
        TimeSeries.builder()
            .txnName("Transaction_Name")
            .metricName("metric_name")
            .url("url_$hostName_$startTime_$endTime")
            .metricType(MetricType.ERROR)
            .build()));
    Map<String, String> validateFields = prometheusState.validateFields();
    assertEquals(1, validateFields.size());
    assertTrue(validateFields.containsKey("Invalid metric type for txn: Transaction_Name, metric : metric_name"));
    assertEquals(
        "metric_name has been configured as RESP_TIME in previous transactions. Same metric name can not have different metric types.",
        validateFields.get("Invalid metric type for txn: Transaction_Name, metric : metric_name"));
  }

  @Test
  public void testMultipleErrors() throws ParseException {
    prometheusState.setTimeSeriesToAnalyze(Lists.newArrayList(TimeSeries.builder()
                                                                  .txnName("Transaction_Name1")
                                                                  .metricName("metric_name")
                                                                  .url("url_$hostName_$startTime_$endTime")
                                                                  .metricType(MetricType.RESP_TIME)
                                                                  .build(),
        TimeSeries.builder()
            .txnName("Transaction_Name1")
            .metricName("metric_name")
            .url("url")
            .metricType(MetricType.ERROR)
            .build(),
        TimeSeries.builder()
            .txnName("Transaction_Name2")
            .metricName("metric_name")
            .url("url_$hostName_$startTime_$endTime")
            .metricType(MetricType.THROUGHPUT)
            .build(),
        TimeSeries.builder()
            .txnName("Transaction_Name2")
            .metricName("metric_name2")
            .url("url_$hostName_$startTime_$endTime")
            .metricType(MetricType.THROUGHPUT)
            .build(),
        TimeSeries.builder()
            .txnName("Transaction_Name2")
            .metricName("metric_name2")
            .url("url")
            .metricType(MetricType.VALUE)
            .build()));
    Map<String, String> validateFields = prometheusState.validateFields();
    assertEquals(5, validateFields.size());

    String key = "Invalid url for txn: Transaction_Name1, metric : metric_name";
    assertTrue(validateFields.containsKey(key));
    assertEquals("[$hostName, $startTime, $endTime] are not present in the url.", validateFields.get(key));

    key = "Invalid url for txn: Transaction_Name2, metric : metric_name2";
    assertTrue(validateFields.containsKey(key));
    assertEquals("[$hostName, $startTime, $endTime] are not present in the url.", validateFields.get(key));

    key = "Invalid metric type for txn: Transaction_Name1, metric : metric_name";
    assertTrue(validateFields.containsKey(key));
    assertEquals(
        "metric_name has been configured as RESP_TIME in previous transactions. Same metric name can not have different metric types.",
        validateFields.get(key));

    key = "Invalid metric type for txn: Transaction_Name2, metric : metric_name";
    assertTrue(validateFields.containsKey(key));
    assertEquals(
        "metric_name has been configured as RESP_TIME in previous transactions. Same metric name can not have different metric types.",
        validateFields.get(key));

    key = "Invalid metric type for txn: Transaction_Name2, metric : metric_name2";
    assertTrue(validateFields.containsKey(key));
    assertEquals(
        "metric_name2 has been configured as THROUGHPUT in previous transactions. Same metric name can not have different metric types.",
        validateFields.get(key));
  }
}
