package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.dynatrace.DynaTraceDataCollectionInfo;
import software.wings.service.impl.dynatrace.DynaTraceTimeSeries;
import software.wings.service.impl.newrelic.MetricAnalysisExecutionData;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.waitnotify.NotifyResponseData;

import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by rsingh on 3/22/18.
 */
public class DynatraceStateTest extends APMStateVerificationTestBase {
  @Inject private MetricDataAnalysisService metricDataAnalysisService;

  private DynatraceState dynatraceState;
  private List<String> serviceMethods = Lists.newArrayList(UUID.randomUUID().toString(), UUID.randomUUID().toString());

  @Before
  public void setup() {
    setupCommon();
    MockitoAnnotations.initMocks(this);
    setupCommonMocks();

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
    for (int i = 1; i <= 7; i++) {
      assertEquals(DEFAULT_GROUP_NAME,
          dynatraceState.getLastExecutionNodes(executionContext).get(DynatraceState.CONTROL_HOST_NAME + i));
    }

    assertEquals(Collections.singletonMap(DynatraceState.TEST_HOST_NAME, DEFAULT_GROUP_NAME),
        dynatraceState.getCanaryNewHostNames(executionContext));
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

    MetricAnalysisExecutionData metricAnalysisExecutionData = MetricAnalysisExecutionData.builder().build();
    MetricDataAnalysisResponse metricDataAnalysisResponse =
        MetricDataAnalysisResponse.builder().stateExecutionData(metricAnalysisExecutionData).build();
    metricDataAnalysisResponse.setExecutionStatus(ExecutionStatus.FAILED);
    Map<String, NotifyResponseData> responseMap = new HashMap<>();
    responseMap.put("somekey", metricDataAnalysisResponse);
    dynatraceState.handleAsyncResponse(executionContext, responseMap);

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
}
