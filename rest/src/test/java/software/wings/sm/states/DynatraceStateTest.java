package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.beans.artifact.Artifact;
import software.wings.common.Constants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.service.impl.dynatrace.DynaTraceDataCollectionInfo;
import software.wings.service.impl.dynatrace.DynaTraceTimeSeries;
import software.wings.service.impl.newrelic.MetricAnalysisExecutionData;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
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
import software.wings.sm.WorkflowStandardParams;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by rsingh on 3/22/18.
 */
public class DynatraceStateTest extends WingsBaseTest {
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
  @Inject private FeatureFlagService featureFlagService;

  @Mock private PhaseElement phaseElement;
  @Mock private Environment environment;
  @Mock private Application application;
  @Mock private Artifact artifact;
  @Mock private StateExecutionInstance stateExecutionInstance;
  @Mock private QuartzScheduler jobScheduler;
  private DynatraceState dynatraceState;
  private List<String> serviceMethods = Lists.newArrayList(UUID.randomUUID().toString(), UUID.randomUUID().toString());

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

    dynatraceState = new DynatraceState("DynatraceState");
    String serviceMethodsString = serviceMethods.get(0) + "\n" + serviceMethods.get(1);
    dynatraceState.setServiceMethods(serviceMethodsString);
    dynatraceState.setTimeDuration("15");
    setInternalState(dynatraceState, "appService", appService);
    setInternalState(dynatraceState, "configuration", configuration);
    setInternalState(dynatraceState, "metricAnalysisService", metricDataAnalysisService);
    setInternalState(dynatraceState, "settingsService", settingsService);
    setInternalState(dynatraceState, "waitNotifyEngine", waitNotifyEngine);
    setInternalState(dynatraceState, "delegateService", delegateService);
    setInternalState(dynatraceState, "jobScheduler", jobScheduler);
    setInternalState(dynatraceState, "secretManager", secretManager);
    setInternalState(dynatraceState, "workflowExecutionService", workflowExecutionService);
    setInternalState(dynatraceState, "continuousVerificationService", continuousVerificationService);
    setInternalState(dynatraceState, "workflowExecutionBaselineService", workflowExecutionBaselineService);
    setInternalState(dynatraceState, "featureFlagService", featureFlagService);
  }

  @Test
  public void testDefaultComparsionStrategy() {
    DynatraceState dynatraceState = new DynatraceState("DynatraceState");
    assertEquals(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS, dynatraceState.getComparisonStrategy());
  }

  @Test
  public void compareTestAndControl() {
    DynatraceState dynatraceState = new DynatraceState("DynatraceState");
    assertEquals(
        Sets.newHashSet(DynatraceState.CONTROL_HOST_NAME), dynatraceState.getLastExecutionNodes(executionContext));
    assertEquals(
        Sets.newHashSet(DynatraceState.TEST_HOST_NAME), dynatraceState.getCanaryNewHostNames(executionContext));
  }

  @Test
  public void testTriggerCollection() throws ParseException {
    assertEquals(0, wingsPersistence.createQuery(DelegateTask.class).count());
    DynaTraceConfig dynaTraceConfig = DynaTraceConfig.builder()
                                          .accountId(accountId)
                                          .dynaTraceUrl("dynatrace-url")
                                          .apiToken(UUID.randomUUID().toString().toCharArray())
                                          .build();
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withName("prometheus-config")
                                            .withValue(dynaTraceConfig)
                                            .build();
    wingsPersistence.save(settingAttribute);
    dynatraceState.setAnalysisServerConfigId(settingAttribute.getUuid());
    DynatraceState spyState = spy(dynatraceState);
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
    assertEquals(TaskType.DYNA_TRACE_METRIC_DATA_COLLECTION_TASK.name(), task.getTaskType());

    DynaTraceDataCollectionInfo expectedCollectionInfo =
        DynaTraceDataCollectionInfo.builder()
            .dynaTraceConfig(dynaTraceConfig)
            .applicationId(appId)
            .stateExecutionId(stateExecutionId)
            .workflowId(workflowId)
            .workflowExecutionId(workflowExecutionId)
            .serviceId(serviceId)
            .startTime(0)
            .collectionTime(Integer.parseInt(dynatraceState.getTimeDuration()))
            .timeSeriesDefinitions(Lists.newArrayList(DynaTraceTimeSeries.values()))
            .serviceMethods(new HashSet<>(serviceMethods))
            .dataCollectionMinute(0)
            .encryptedDataDetails(
                secretManager.getEncryptionDetails((DynaTraceConfig) settingAttribute.getValue(), null, null))
            .analysisComparisonStrategy(dynatraceState.getComparisonStrategy())
            .build();

    final DynaTraceDataCollectionInfo actualCollectionInfo = (DynaTraceDataCollectionInfo) task.getParameters()[0];
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

    MetricAnalysisExecutionData metricAnalysisExecutionData = MetricAnalysisExecutionData.builder().build();
    MetricDataAnalysisResponse metricDataAnalysisResponse =
        MetricDataAnalysisResponse.builder().stateExecutionData(metricAnalysisExecutionData).build();
    metricDataAnalysisResponse.setExecutionStatus(ExecutionStatus.FAILED);
    Map<String, NotifyResponseData> responseMap = new HashMap<>();
    responseMap.put("somekey", metricDataAnalysisResponse);
    dynatraceState.handleAsyncResponse(executionContext, responseMap);

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
