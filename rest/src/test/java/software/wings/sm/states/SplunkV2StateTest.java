package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTask.Status;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.beans.TaskType;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.metrics.RiskLevel;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.LogAnalysisExecutionData;
import software.wings.service.impl.analysis.LogAnalysisResponse;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.elk.ElkIndexTemplate;
import software.wings.service.impl.splunk.SplunkDataCollectionInfo;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by rsingh on 10/9/17.
 */
public class SplunkV2StateTest extends WingsBaseTest {
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String delegateTaskId;
  @Mock private ExecutionContext executionContext;

  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private BroadcasterFactory broadcasterFactory;
  @Inject private AnalysisService analysisService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;
  @Inject private SettingsService settingsService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelegateService delegateService;
  @Inject private MainConfiguration configuration;
  @Inject private SecretManager secretManager;
  @Mock private QuartzScheduler jobScheduler;
  @Mock private ElkAnalysisService elkAnalysisService;
  private SplunkV2State splunkState;

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
    wingsPersistence.save(
        WorkflowExecutionBuilder.aWorkflowExecution().withAppId(appId).withWorkflowId(workflowId).build());
    configuration.getPortal().setJwtExternalServiceSecret(accountId);
    MockitoAnnotations.initMocks(this);

    when(executionContext.getAppId()).thenReturn(appId);
    when(executionContext.getWorkflowExecutionId()).thenReturn(workflowExecutionId);
    when(executionContext.getStateExecutionInstanceId()).thenReturn(stateExecutionId);

    Broadcaster broadcaster = mock(Broadcaster.class);
    when(broadcaster.broadcast(anyObject())).thenReturn(null);
    when(broadcasterFactory.lookup(anyObject(), anyBoolean())).thenReturn(broadcaster);
    setInternalState(delegateService, "broadcasterFactory", broadcasterFactory);

    when(jobScheduler.scheduleJob(anyObject(), anyObject())).thenReturn(new Date());

    splunkState = new SplunkV2State("SplunkState");
    splunkState.setQuery("exception");
    splunkState.setTimeDuration("15");
    setInternalState(splunkState, "appService", appService);
    setInternalState(splunkState, "configuration", configuration);
    setInternalState(splunkState, "analysisService", analysisService);
    setInternalState(splunkState, "settingsService", settingsService);
    setInternalState(splunkState, "waitNotifyEngine", waitNotifyEngine);
    setInternalState(splunkState, "delegateService", delegateService);
    setInternalState(splunkState, "jobScheduler", jobScheduler);
    setInternalState(splunkState, "secretManager", secretManager);
  }

  @Test
  public void testDefaultComparsionStrategy() {
    SplunkV2State splunkState = new SplunkV2State("SplunkState");
    assertEquals(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS, splunkState.getComparisonStrategy());
  }

  @Test
  public void noTestNodes() {
    SplunkV2State spyState = spy(splunkState);
    doReturn(Collections.emptySet()).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.emptySet()).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.FAILED, response.getExecutionStatus());
    assertEquals("Could not find hosts to analyze!", response.getErrorMessage());

    LogMLAnalysisSummary analysisSummary =
        analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SPLUNKV2);
    assertEquals(RiskLevel.NA, analysisSummary.getRiskLevel());
    assertEquals(splunkState.getQuery(), analysisSummary.getQuery());
    assertEquals(response.getErrorMessage(), analysisSummary.getAnalysisSummaryMessage());
    assertTrue(analysisSummary.getControlClusters().isEmpty());
    assertTrue(analysisSummary.getTestClusters().isEmpty());
    assertTrue(analysisSummary.getUnknownClusters().isEmpty());
  }

  @Test
  public void noControlNodesCompareWithCurrent() {
    splunkState.setComparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_CURRENT.name());
    SplunkV2State spyState = spy(splunkState);
    doReturn(Collections.singleton("some-host")).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.emptySet()).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.SUCCESS, response.getExecutionStatus());
    assertEquals("Skipping analysis due to lack of baseline hosts. Make sure you have at least two phases defined.",
        response.getErrorMessage());

    LogMLAnalysisSummary analysisSummary =
        analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SPLUNKV2);
    assertEquals(RiskLevel.NA, analysisSummary.getRiskLevel());
    assertEquals(splunkState.getQuery(), analysisSummary.getQuery());
    assertEquals(response.getErrorMessage(), analysisSummary.getAnalysisSummaryMessage());
    assertTrue(analysisSummary.getControlClusters().isEmpty());
    assertTrue(analysisSummary.getTestClusters().isEmpty());
    assertTrue(analysisSummary.getUnknownClusters().isEmpty());
  }

  @Test
  public void compareWithCurrentSameTestAndControlNodes() {
    splunkState.setComparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_CURRENT.name());
    SplunkV2State spyState = spy(splunkState);
    doReturn(Sets.newHashSet("some-host")).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Sets.newHashSet("some-host")).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.SUCCESS, response.getExecutionStatus());
    assertEquals("Skipping analysis due to lack of baseline hosts. Make sure you have at least two phases defined.",
        response.getErrorMessage());

    LogMLAnalysisSummary analysisSummary =
        analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SPLUNKV2);
    assertEquals(RiskLevel.NA, analysisSummary.getRiskLevel());
    assertEquals(splunkState.getQuery(), analysisSummary.getQuery());
    assertEquals(response.getErrorMessage(), analysisSummary.getAnalysisSummaryMessage());
    assertTrue(analysisSummary.getControlClusters().isEmpty());
    assertTrue(analysisSummary.getTestClusters().isEmpty());
    assertTrue(analysisSummary.getUnknownClusters().isEmpty());
  }

  @Test
  public void testTriggerCollection() {
    assertEquals(0, wingsPersistence.createQuery(DelegateTask.class).asList().size());
    SplunkConfig splunkConfig = SplunkConfig.builder()
                                    .accountId(accountId)
                                    .splunkUrl("splunk-url")
                                    .username("splunk-user")
                                    .password("splunk-pwd".toCharArray())
                                    .build();
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withName("splunk-config")
                                            .withValue(splunkConfig)
                                            .build();
    wingsPersistence.save(settingAttribute);
    splunkState.setAnalysisServerConfigId(settingAttribute.getUuid());
    SplunkV2State spyState = spy(splunkState);
    doReturn(Collections.singleton("test")).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.singleton("control")).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.RUNNING, response.getExecutionStatus());
    assertEquals("Log Verification running", response.getErrorMessage());

    List<DelegateTask> tasks = wingsPersistence.createQuery(DelegateTask.class).asList();
    assertEquals(1, tasks.size());
    DelegateTask task = tasks.get(0);
    assertEquals(TaskType.SPLUNK_COLLECT_LOG_DATA, task.getTaskType());

    final SplunkDataCollectionInfo expectedCollectionInfo =
        new SplunkDataCollectionInfo(splunkConfig, accountId, appId, stateExecutionId, workflowId, workflowExecutionId,
            serviceId, Sets.newHashSet(splunkState.getQuery().split(",")), 0, 0,
            Integer.parseInt(splunkState.getTimeDuration()), Collections.singleton("test"), Collections.emptyList());
    final SplunkDataCollectionInfo actualCollectionInfo = (SplunkDataCollectionInfo) task.getParameters()[0];
    expectedCollectionInfo.setStartTime(actualCollectionInfo.getStartTime());
    assertEquals(expectedCollectionInfo, actualCollectionInfo);
    assertEquals(accountId, task.getAccountId());
    assertEquals(Status.QUEUED, task.getStatus());
    assertEquals(appId, task.getAppId());
  }

  @Test
  public void handleAsyncSummaryFail() {
    LogAnalysisExecutionData logAnalysisExecutionData = new LogAnalysisExecutionData();
    logAnalysisExecutionData.setCorrelationId(UUID.randomUUID().toString());
    logAnalysisExecutionData.setStateExecutionInstanceId(stateExecutionId);
    logAnalysisExecutionData.setServerConfigId(UUID.randomUUID().toString());
    logAnalysisExecutionData.setQueries(Sets.newHashSet(splunkState.getQuery().split(",")));
    logAnalysisExecutionData.setTimeDuration(Integer.parseInt(splunkState.getTimeDuration()));
    logAnalysisExecutionData.setCanaryNewHostNames(Sets.newHashSet("test1", "test2"));
    logAnalysisExecutionData.setLastExecutionNodes(Sets.newHashSet("control1", "control2", "control3"));
    logAnalysisExecutionData.setErrorMsg(UUID.randomUUID().toString());

    LogAnalysisResponse response = LogAnalysisResponse.Builder.aLogAnalysisResponse()
                                       .withExecutionStatus(ExecutionStatus.FAILED)
                                       .withLogAnalysisExecutionData(logAnalysisExecutionData)
                                       .build();

    Map<String, NotifyResponseData> responseMap = new HashMap<>();
    responseMap.put("somekey", response);

    ExecutionResponse executionResponse = splunkState.handleAsyncResponse(executionContext, responseMap);
    assertEquals(ExecutionStatus.FAILED, executionResponse.getExecutionStatus());
    assertEquals(logAnalysisExecutionData.getErrorMsg(), executionResponse.getErrorMessage());
    assertEquals(logAnalysisExecutionData, executionResponse.getStateExecutionData());
  }

  @Test
  public void handleAsyncSummaryPassNoData() {
    LogAnalysisExecutionData logAnalysisExecutionData = new LogAnalysisExecutionData();
    logAnalysisExecutionData.setCorrelationId(UUID.randomUUID().toString());
    logAnalysisExecutionData.setStateExecutionInstanceId(stateExecutionId);
    logAnalysisExecutionData.setServerConfigId(UUID.randomUUID().toString());
    logAnalysisExecutionData.setQueries(Sets.newHashSet(splunkState.getQuery().split(",")));
    logAnalysisExecutionData.setTimeDuration(Integer.parseInt(splunkState.getTimeDuration()));
    logAnalysisExecutionData.setCanaryNewHostNames(Sets.newHashSet("test1", "test2"));
    logAnalysisExecutionData.setLastExecutionNodes(Sets.newHashSet("control1", "control2", "control3"));
    logAnalysisExecutionData.setErrorMsg(UUID.randomUUID().toString());

    LogAnalysisResponse response = LogAnalysisResponse.Builder.aLogAnalysisResponse()
                                       .withExecutionStatus(ExecutionStatus.SUCCESS)
                                       .withLogAnalysisExecutionData(logAnalysisExecutionData)
                                       .build();

    Map<String, NotifyResponseData> responseMap = new HashMap<>();
    responseMap.put("somekey", response);

    SplunkV2State spyState = spy(splunkState);
    doReturn(Collections.singleton("test")).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.singleton("control")).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse executionResponse = spyState.handleAsyncResponse(executionContext, responseMap);
    assertEquals(ExecutionStatus.SUCCESS, executionResponse.getExecutionStatus());
    assertEquals("No data found with given queries. Skipped Analysis", executionResponse.getErrorMessage());

    LogMLAnalysisSummary analysisSummary =
        analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SPLUNKV2);
    assertEquals(RiskLevel.NA, analysisSummary.getRiskLevel());
    assertEquals(splunkState.getQuery(), analysisSummary.getQuery());
    assertEquals(executionResponse.getErrorMessage(), analysisSummary.getAnalysisSummaryMessage());
    assertTrue(analysisSummary.getControlClusters().isEmpty());
    assertTrue(analysisSummary.getTestClusters().isEmpty());
    assertTrue(analysisSummary.getUnknownClusters().isEmpty());
  }

  @Test(expected = WingsException.class)
  public void elkTimeStampFieldFormatIndexNotFound() throws IOException {
    String analysisServerConfigID = UUID.randomUUID().toString();
    ElkAnalysisState elkAnalysisState = new ElkAnalysisState("Integration-test", StateType.ELK.name());
    elkAnalysisState.setIndices("filebeat-*");
    elkAnalysisState.setAnalysisServerConfigId(analysisServerConfigID);
    Reflect.on(elkAnalysisState).set("elkAnalysisService", elkAnalysisService);
    Map<String, ElkIndexTemplate> templates = new HashMap<>();

    when(elkAnalysisService.getIndices(accountId, analysisServerConfigID)).thenReturn(templates);
    elkAnalysisState.getTimestampFieldFormat(accountId, "@timestamp");
  }

  @Test(expected = WingsException.class)
  public void elkTimeStampFieldFormatIndexPropertyNotFound() throws IOException {
    String analysisServerConfigID = UUID.randomUUID().toString();
    ElkAnalysisState elkAnalysisState = new ElkAnalysisState("Integration-test", StateType.ELK.name());
    elkAnalysisState.setIndices("filebeat-*");
    elkAnalysisState.setAnalysisServerConfigId(analysisServerConfigID);
    Reflect.on(elkAnalysisState).set("elkAnalysisService", elkAnalysisService);
    Map<String, ElkIndexTemplate> templates = new HashMap<>();
    templates.put("filebeat-*", new ElkIndexTemplate());
    when(elkAnalysisService.getIndices(accountId, analysisServerConfigID)).thenReturn(templates);
    elkAnalysisState.getTimestampFieldFormat(accountId, "@timestamp");
  }

  @Test
  public void elkTimeStampFieldFormatIndexNoTimestamp() throws IOException {
    String analysisServerConfigID = UUID.randomUUID().toString();
    ElkAnalysisState elkAnalysisState = new ElkAnalysisState("Integration-test", StateType.ELK.name());
    elkAnalysisState.setIndices("filebeat-*");
    elkAnalysisState.setAnalysisServerConfigId(analysisServerConfigID);
    Reflect.on(elkAnalysisState).set("elkAnalysisService", elkAnalysisService);
    Map<String, ElkIndexTemplate> templates = new HashMap<>();
    ElkIndexTemplate elkIndexTemplate = new ElkIndexTemplate();
    elkIndexTemplate.setProperties(new HashMap<>());
    templates.put("filebeat-*", elkIndexTemplate);
    when(elkAnalysisService.getIndices(accountId, analysisServerConfigID)).thenReturn(templates);
    assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", elkAnalysisState.getTimestampFieldFormat(accountId, "@timestamp"));
  }

  @Test
  public void elkTimeStampFieldFormatIndexTimestampNoFormat() throws IOException {
    String analysisServerConfigID = UUID.randomUUID().toString();
    ElkAnalysisState elkAnalysisState = new ElkAnalysisState("Integration-test", StateType.ELK.name());
    elkAnalysisState.setIndices("filebeat-*");
    elkAnalysisState.setAnalysisServerConfigId(analysisServerConfigID);
    Reflect.on(elkAnalysisState).set("elkAnalysisService", elkAnalysisService);
    Map<String, ElkIndexTemplate> templates = new HashMap<>();
    ElkIndexTemplate elkIndexTemplate = new ElkIndexTemplate();
    elkIndexTemplate.setProperties(new HashMap<>());
    elkIndexTemplate.getProperties().put("@timestamp", new HashMap<>());
    ((Map) (elkIndexTemplate.getProperties().get("@timestamp"))).put("type", "date");
    templates.put("filebeat-*", elkIndexTemplate);
    when(elkAnalysisService.getIndices(accountId, analysisServerConfigID)).thenReturn(templates);
    assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", elkAnalysisState.getTimestampFieldFormat(accountId, "@timestamp"));
  }

  @Test
  public void elkTimeStampFieldFormatIndexWithTimestamp() throws IOException {
    String analysisServerConfigID = UUID.randomUUID().toString();
    ElkAnalysisState elkAnalysisState = new ElkAnalysisState("Integration-test", StateType.ELK.name());
    elkAnalysisState.setIndices("filebeat-*");
    elkAnalysisState.setAnalysisServerConfigId(analysisServerConfigID);
    Reflect.on(elkAnalysisState).set("elkAnalysisService", elkAnalysisService);
    Map<String, ElkIndexTemplate> templates = new HashMap<>();
    ElkIndexTemplate elkIndexTemplate = new ElkIndexTemplate();
    elkIndexTemplate.setProperties(new HashMap<>());
    elkIndexTemplate.getProperties().put("@timestamp", new HashMap<>());
    ((Map) (elkIndexTemplate.getProperties().get("@timestamp"))).put("format", "anytimestamp");
    templates.put("filebeat-*", elkIndexTemplate);
    when(elkAnalysisService.getIndices(accountId, analysisServerConfigID)).thenReturn(templates);
    assertEquals("anytimestamp", elkAnalysisState.getTimestampFieldFormat(accountId, "@timestamp"));
  }
}
