package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTask.Status;
import software.wings.beans.Environment;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.beans.artifact.Artifact;
import software.wings.common.Constants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.MetricType;
import software.wings.metrics.RiskLevel;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates;
import software.wings.service.impl.newrelic.MetricAnalysisExecutionData;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.prometheus.PrometheusDataCollectionInfo;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionBaselineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by rsingh on 3/22/18.
 */
public class PrometheusStateTest extends WingsBaseTest {
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String delegateTaskId;
  @Mock private ExecutionContextImpl executionContext;

  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private BroadcasterFactory broadcasterFactory;
  @Mock private WorkflowStandardParams workflowStandardParams;
  @Inject private MetricDataAnalysisService metricDataAnalysisService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;
  @Inject private SettingsService settingsService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelegateService delegateService;
  @Inject private MainConfiguration configuration;
  @Inject private SecretManager secretManager;
  @Inject private ContinuousVerificationService continuousVerificationService;
  @Inject private WorkflowExecutionBaselineService workflowExecutionBaselineService;

  @Inject private WorkflowExecutionService workflowExecutionService;
  @Mock private PhaseElement phaseElement;
  @Mock private Environment environment;
  @Mock private Application application;
  @Mock private Artifact artifact;
  @Mock private StateExecutionInstance stateExecutionInstance;
  @Mock private QuartzScheduler jobScheduler;
  private PrometheusState prometheusState;
  private List<TimeSeries> timeSeriesToCollect = Lists.newArrayList(TimeSeries.builder()
                                                                        .txnName("Transaction_Name")
                                                                        .metricName("metric_name")
                                                                        .url("url_$hostName_$startTime_$endTime")
                                                                        .metricType(MetricType.RESP_TIME)
                                                                        .build());

  @Before
  public void setup() {
    accountId = UUID.randomUUID().toString();
    appId = UUID.randomUUID().toString();
    stateExecutionId = UUID.randomUUID().toString();
    workflowId = UUID.randomUUID().toString();
    workflowExecutionId = UUID.randomUUID().toString();
    serviceId = UUID.randomUUID().toString();
    delegateTaskId = UUID.randomUUID().toString();

    wingsPersistence.save(Application.Builder.anApplication().withUuid(appId).withAccountId(accountId).build());
    wingsPersistence.save(WorkflowExecutionBuilder.aWorkflowExecution()
                              .withAppId(appId)
                              .withWorkflowId(workflowId)
                              .withUuid(workflowExecutionId)
                              .withStartTs(1519200000000L)
                              .withName("dummy workflow")
                              .build());
    configuration.getPortal().setJwtExternalServiceSecret(accountId);
    MockitoAnnotations.initMocks(this);

    when(executionContext.getAppId()).thenReturn(appId);
    when(executionContext.getWorkflowExecutionId()).thenReturn(workflowExecutionId);
    when(executionContext.getStateExecutionInstanceId()).thenReturn(stateExecutionId);
    when(executionContext.getWorkflowId()).thenReturn(workflowId);
    when(executionContext.getWorkflowExecutionName()).thenReturn("dummy workflow");

    when(phaseElement.getServiceElement())
        .thenReturn(ServiceElement.Builder.aServiceElement().withName("dummy").withUuid("1").build());
    when(executionContext.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM)).thenReturn(phaseElement);
    when(environment.getName()).thenReturn("dummy env");
    when(executionContext.getEnv()).thenReturn(environment);
    when(application.getName()).thenReturn("dummuy app");
    when(executionContext.getApp()).thenReturn(application);
    when(artifact.getDisplayName()).thenReturn("dummy artifact");
    when(executionContext.getArtifactForService(anyString())).thenReturn(artifact);
    when(stateExecutionInstance.getStartTs()).thenReturn(1519200000000L);
    when(executionContext.getStateExecutionInstance()).thenReturn(stateExecutionInstance);

    Broadcaster broadcaster = mock(Broadcaster.class);
    when(broadcaster.broadcast(anyObject())).thenReturn(null);
    when(broadcasterFactory.lookup(anyObject(), anyBoolean())).thenReturn(broadcaster);
    setInternalState(delegateService, "broadcasterFactory", broadcasterFactory);

    when(jobScheduler.scheduleJob(anyObject(), anyObject())).thenReturn(new Date());

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
  }

  @Test
  public void testDefaultComparsionStrategy() {
    PrometheusState splunkState = new PrometheusState("PrometheusState");
    assertEquals(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS, splunkState.getComparisonStrategy());
  }

  @Test
  public void noTestNodes() {
    PrometheusState spyState = spy(prometheusState);
    doReturn(Collections.emptySet()).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.emptySet()).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.SUCCESS, response.getExecutionStatus());
    assertEquals("Could not find nodes to analyze!", response.getErrorMessage());

    NewRelicMetricAnalysisRecord metricsAnalysis =
        metricDataAnalysisService.getMetricsAnalysis(stateExecutionId, workflowExecutionId);
    assertEquals(stateExecutionId, metricsAnalysis.getStateExecutionId());
    assertEquals(workflowExecutionId, metricsAnalysis.getWorkflowExecutionId());
    assertEquals(workflowId, metricsAnalysis.getWorkflowId());
    assertEquals(RiskLevel.NA, metricsAnalysis.getRiskLevel());
    assertEquals("No data available", metricsAnalysis.getMessage());
    assertNull(metricsAnalysis.getMetricAnalyses());
  }

  @Test
  public void noControlNodesCompareWithCurrent() {
    prometheusState.setComparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_CURRENT.name());
    PrometheusState spyState = spy(prometheusState);
    doReturn(Collections.singleton("some-host")).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.emptySet()).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.SUCCESS, response.getExecutionStatus());
    assertEquals("Skipping analysis due to lack of baseline data (First time deployment or Last phase).",
        response.getErrorMessage());

    NewRelicMetricAnalysisRecord metricsAnalysis =
        metricDataAnalysisService.getMetricsAnalysis(stateExecutionId, workflowExecutionId);
    assertEquals(stateExecutionId, metricsAnalysis.getStateExecutionId());
    assertEquals(workflowExecutionId, metricsAnalysis.getWorkflowExecutionId());
    assertEquals(workflowId, metricsAnalysis.getWorkflowId());
    assertEquals(RiskLevel.NA, metricsAnalysis.getRiskLevel());
    assertEquals("No data available", metricsAnalysis.getMessage());
    assertNull(metricsAnalysis.getMetricAnalyses());
  }

  @Test
  public void compareWithCurrentSameTestAndControlNodes() {
    prometheusState.setComparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_CURRENT.name());
    PrometheusState spyState = spy(prometheusState);
    doReturn(Sets.newHashSet("some-host")).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Sets.newHashSet("some-host")).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.SUCCESS, response.getExecutionStatus());
    assertEquals("Skipping analysis due to lack of baseline data (First time deployment or Last phase).",
        response.getErrorMessage());

    NewRelicMetricAnalysisRecord metricsAnalysis =
        metricDataAnalysisService.getMetricsAnalysis(stateExecutionId, workflowExecutionId);
    assertEquals(stateExecutionId, metricsAnalysis.getStateExecutionId());
    assertEquals(workflowExecutionId, metricsAnalysis.getWorkflowExecutionId());
    assertEquals(workflowId, metricsAnalysis.getWorkflowId());
    assertEquals(RiskLevel.NA, metricsAnalysis.getRiskLevel());
    assertEquals("No data available", metricsAnalysis.getMessage());
    assertNull(metricsAnalysis.getMetricAnalyses());
  }

  @Test
  public void testTriggerCollection() throws ParseException {
    assertEquals(0, wingsPersistence.createQuery(DelegateTask.class).asList().size());
    PrometheusConfig prometheusConfig = PrometheusConfig.builder().accountId(accountId).url("prometheus-url").build();
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withName("prometheus-config")
                                            .withValue(prometheusConfig)
                                            .build();
    wingsPersistence.save(settingAttribute);
    prometheusState.setAnalysisServerConfigId(settingAttribute.getUuid());
    PrometheusState spyState = spy(prometheusState);
    doReturn(Collections.singleton("test")).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.singleton("control")).when(spyState).getLastExecutionNodes(executionContext);
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
            .hosts(Sets.newHashSet("test"))
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
            continuousVerificationService.getCVExecutionMetaData(accountId, 1519200000000L, 1519200000001L);
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
    assertEquals(1, wingsPersistence.createQuery(TimeSeriesMetricTemplates.class).asList().size());
    Map<String, TimeSeriesMetricDefinition> metricTemplates =
        metricDataAnalysisService.getMetricTemplates(StateType.PROMETHEUS, stateExecutionId);
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
        continuousVerificationService.getCVExecutionMetaData(accountId, 1519200000000L, 1519200000001L);
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
