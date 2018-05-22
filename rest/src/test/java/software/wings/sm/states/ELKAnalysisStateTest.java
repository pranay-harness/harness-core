package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.sm.states.ElkAnalysisState.DEFAULT_TIME_FIELD;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTask.Status;
import software.wings.beans.ElkConfig;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.beans.artifact.Artifact;
import software.wings.common.Constants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.RiskLevel;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.impl.analysis.ElkValidationType;
import software.wings.service.impl.analysis.LogAnalysisExecutionData;
import software.wings.service.impl.analysis.LogAnalysisResponse;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionBaselineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.elk.ElkAnalysisService;
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
 * Created by rsingh on 10/9/17.
 */
public class ELKAnalysisStateTest extends WingsBaseTest {
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
  @Inject private AnalysisService analysisService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;
  @Inject private SettingsService settingsService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelegateService delegateService;
  @Inject private MainConfiguration configuration;
  @Inject private SecretManager secretManager;
  @Inject private ContinuousVerificationService continuousVerificationService;
  @Inject private WorkflowExecutionBaselineService workflowExecutionBaselineService;
  @Inject private FeatureFlagService featureFlagService;

  @Inject private WorkflowExecutionService workflowExecutionService;
  @Mock private PhaseElement phaseElement;
  @Mock private Environment environment;
  @Mock private Application application;
  @Mock private Artifact artifact;
  @Mock private StateExecutionInstance stateExecutionInstance;
  @Mock private QuartzScheduler jobScheduler;
  @Mock private ElkAnalysisService elkAnalysisService;
  private ElkAnalysisState elkAnalysisState;

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
    when(executionContext.getWorkflowExecutionName()).thenReturn("dummy workflow");
    when(executionContext.renderExpression(anyString())).then(returnsFirstArg());

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

    elkAnalysisState = new ElkAnalysisState("ElkAnalysisState");
    elkAnalysisState.setQuery("exception");
    elkAnalysisState.setTimeDuration("15");
    setInternalState(elkAnalysisState, "appService", appService);
    setInternalState(elkAnalysisState, "configuration", configuration);
    setInternalState(elkAnalysisState, "analysisService", analysisService);
    setInternalState(elkAnalysisState, "settingsService", settingsService);
    setInternalState(elkAnalysisState, "waitNotifyEngine", waitNotifyEngine);
    setInternalState(elkAnalysisState, "delegateService", delegateService);
    setInternalState(elkAnalysisState, "jobScheduler", jobScheduler);
    setInternalState(elkAnalysisState, "secretManager", secretManager);
    setInternalState(elkAnalysisState, "workflowExecutionService", workflowExecutionService);
    setInternalState(elkAnalysisState, "continuousVerificationService", continuousVerificationService);
    setInternalState(elkAnalysisState, "workflowExecutionBaselineService", workflowExecutionBaselineService);
    setInternalState(elkAnalysisState, "featureFlagService", featureFlagService);
  }

  @Test
  public void testDefaultComparsionStrategy() {
    ElkAnalysisState elkAnalysisState = new ElkAnalysisState("ElkAnalysisState");
    assertEquals(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS, elkAnalysisState.getComparisonStrategy());
  }

  @Test
  public void noTestNodes() {
    ElkAnalysisState spyState = spy(elkAnalysisState);
    doReturn(Collections.emptySet()).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.emptySet()).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.SUCCESS, response.getExecutionStatus());
    assertEquals("Could not find hosts to analyze!", response.getErrorMessage());

    LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.ELK);
    assertEquals(RiskLevel.NA, analysisSummary.getRiskLevel());
    assertEquals(elkAnalysisState.getQuery(), analysisSummary.getQuery());
    assertEquals(response.getErrorMessage(), analysisSummary.getAnalysisSummaryMessage());
    assertTrue(analysisSummary.getControlClusters().isEmpty());
    assertTrue(analysisSummary.getTestClusters().isEmpty());
    assertTrue(analysisSummary.getUnknownClusters().isEmpty());
  }

  @Test
  public void noControlNodesCompareWithCurrent() {
    elkAnalysisState.setComparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_CURRENT.name());
    ElkAnalysisState spyState = spy(elkAnalysisState);
    doReturn(Collections.singleton("some-host")).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.emptySet()).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.SUCCESS, response.getExecutionStatus());
    assertEquals("Skipping analysis due to lack of baseline hosts. Make sure you have at least two phases defined.",
        response.getErrorMessage());

    LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.ELK);
    assertEquals(RiskLevel.NA, analysisSummary.getRiskLevel());
    assertEquals(elkAnalysisState.getQuery(), analysisSummary.getQuery());
    assertEquals(response.getErrorMessage(), analysisSummary.getAnalysisSummaryMessage());
    assertTrue(analysisSummary.getControlClusters().isEmpty());
    assertTrue(analysisSummary.getTestClusters().isEmpty());
    assertTrue(analysisSummary.getUnknownClusters().isEmpty());
  }

  @Test
  public void compareWithCurrentSameTestAndControlNodes() {
    elkAnalysisState.setComparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_CURRENT.name());
    ElkAnalysisState spyState = spy(elkAnalysisState);
    doReturn(Sets.newHashSet("some-host")).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Sets.newHashSet("some-host")).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.SUCCESS, response.getExecutionStatus());
    assertEquals("Skipping analysis due to lack of baseline hosts. Make sure you have at least two phases defined.",
        response.getErrorMessage());

    LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.ELK);
    assertEquals(RiskLevel.NA, analysisSummary.getRiskLevel());
    assertEquals(elkAnalysisState.getQuery(), analysisSummary.getQuery());
    assertEquals(response.getErrorMessage(), analysisSummary.getAnalysisSummaryMessage());
    assertTrue(analysisSummary.getControlClusters().isEmpty());
    assertTrue(analysisSummary.getTestClusters().isEmpty());
    assertTrue(analysisSummary.getUnknownClusters().isEmpty());
  }

  @Test
  public void testTriggerCollection() throws ParseException {
    assertEquals(0, wingsPersistence.createQuery(DelegateTask.class).count());
    ElkConfig elkConfig = ElkConfig.builder()
                              .accountId(accountId)
                              .elkConnector(ElkConnector.ELASTIC_SEARCH_SERVER)
                              .elkUrl(UUID.randomUUID().toString())
                              .username(UUID.randomUUID().toString())
                              .password(UUID.randomUUID().toString().toCharArray())
                              .validationType(ElkValidationType.PASSWORD)
                              .kibanaVersion(String.valueOf(0))
                              .build();

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withName("elk-config")
                                            .withValue(elkConfig)
                                            .build();
    wingsPersistence.save(settingAttribute);
    elkAnalysisState.setAnalysisServerConfigId(settingAttribute.getUuid());

    String indices = UUID.randomUUID().toString();
    elkAnalysisState.setIndices(indices);

    String messageField = UUID.randomUUID().toString();
    elkAnalysisState.setMessageField(messageField);

    String timestampFieldFormat = UUID.randomUUID().toString();
    elkAnalysisState.setTimestampFormat(timestampFieldFormat);

    elkAnalysisState.setQueryType(ElkQueryType.MATCH.name());
    elkAnalysisState.setComparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_CURRENT.name());

    ElkAnalysisState spyState = spy(elkAnalysisState);
    doReturn(Collections.singleton("test")).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.singleton("control")).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);
    when(workflowStandardParams.getEnv())
        .thenReturn(Environment.Builder.anEnvironment().withUuid(UUID.randomUUID().toString()).build());
    when(executionContext.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.RUNNING, response.getExecutionStatus());
    assertEquals("Log Verification running.", response.getErrorMessage());

    List<DelegateTask> tasks = wingsPersistence.createQuery(DelegateTask.class).asList();
    assertEquals(1, tasks.size());
    DelegateTask task = tasks.get(0);
    assertEquals(TaskType.ELK_COLLECT_LOG_DATA.name(), task.getTaskType());

    final ElkDataCollectionInfo expectedCollectionInfo =
        ElkDataCollectionInfo.builder()
            .elkConfig(elkConfig)
            .indices(indices)
            .messageField(messageField)
            .timestampField(DEFAULT_TIME_FIELD)
            .timestampFieldFormat(timestampFieldFormat)
            .queryType(ElkQueryType.MATCH)
            .accountId(accountId)
            .applicationId(appId)
            .stateExecutionId(stateExecutionId)
            .workflowId(workflowId)
            .workflowExecutionId(workflowExecutionId)
            .serviceId(serviceId)
            .queries(Sets.newHashSet(elkAnalysisState.getQuery().split(",")))
            .startMinute(0)
            .startMinute(0)
            .collectionTime(Integer.parseInt(elkAnalysisState.getTimeDuration()))
            .hosts(Sets.newHashSet("test", "control"))
            .encryptedDataDetails(secretManager.getEncryptionDetails(elkConfig, null, null))
            .build();
    final ElkDataCollectionInfo actualCollectionInfo = (ElkDataCollectionInfo) task.getParameters()[0];
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

    LogAnalysisExecutionData logAnalysisExecutionData = LogAnalysisExecutionData.builder().build();
    LogAnalysisResponse logAnalysisResponse = LogAnalysisResponse.Builder.aLogAnalysisResponse()
                                                  .withExecutionStatus(ExecutionStatus.ERROR)
                                                  .withLogAnalysisExecutionData(logAnalysisExecutionData)
                                                  .build();
    Map<String, NotifyResponseData> responseMap = new HashMap<>();
    responseMap.put("somekey", logAnalysisResponse);
    elkAnalysisState.handleAsyncResponse(executionContext, responseMap);

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
}
