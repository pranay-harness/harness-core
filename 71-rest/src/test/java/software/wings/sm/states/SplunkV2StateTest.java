package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.delegate.task.protocol.ResponseData;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTask.Status;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.beans.TaskType;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.LogAnalysisExecutionData;
import software.wings.service.impl.analysis.LogAnalysisResponse;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.splunk.SplunkDataCollectionInfo;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by rsingh on 10/9/17.
 */
public class SplunkV2StateTest extends APMStateVerificationTestBase {
  @Inject private AnalysisService analysisService;
  private SplunkV2State splunkState;

  @Before
  public void setup() {
    setupCommon();
    MockitoAnnotations.initMocks(this);
    setupCommonMocks();

    splunkState = new SplunkV2State("SplunkState");
    splunkState.setQuery("exception");
    splunkState.setTimeDuration("15");
    setInternalState(splunkState, "appService", appService);
    setInternalState(splunkState, "configuration", configuration);
    setInternalState(splunkState, "analysisService", analysisService);
    setInternalState(splunkState, "settingsService", settingsService);
    setInternalState(splunkState, "waitNotifyEngine", waitNotifyEngine);
    setInternalState(splunkState, "delegateService", delegateService);
    setInternalState(splunkState, "wingsPersistence", wingsPersistence);
    setInternalState(splunkState, "secretManager", secretManager);
    setInternalState(splunkState, "workflowExecutionService", workflowExecutionService);
    setInternalState(splunkState, "continuousVerificationService", continuousVerificationService);
    setInternalState(splunkState, "workflowExecutionBaselineService", workflowExecutionBaselineService);
    setInternalState(splunkState, "featureFlagService", featureFlagService);
    setInternalState(splunkState, "versionInfoManager", versionInfoManager);
  }

  @Test
  public void testDefaultComparsionStrategy() {
    SplunkV2State splunkState = new SplunkV2State("SplunkState");
    assertEquals(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS, splunkState.getComparisonStrategy());
  }

  @Test
  public void noTestNodes() {
    SplunkV2State spyState = spy(splunkState);
    doReturn(Collections.emptyMap()).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.emptyMap()).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.SUCCESS, response.getExecutionStatus());
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
    doReturn(Collections.singletonMap("some-host", DEFAULT_GROUP_NAME))
        .when(spyState)
        .getCanaryNewHostNames(executionContext);
    doReturn(Collections.emptyMap()).when(spyState).getLastExecutionNodes(executionContext);
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
  public void testTriggerCollection() throws ParseException {
    assertEquals(0, wingsPersistence.createQuery(DelegateTask.class).count());
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
        "No baseline was set for the workflow. Workflow running with auto baseline. No previous execution found. This will be the baseline run.",
        response.getErrorMessage());

    List<DelegateTask> tasks = wingsPersistence.createQuery(DelegateTask.class).asList();
    assertEquals(1, tasks.size());
    DelegateTask task = tasks.get(0);
    assertEquals(TaskType.SPLUNK_COLLECT_LOG_DATA.name(), task.getTaskType());

    final SplunkDataCollectionInfo expectedCollectionInfo =
        SplunkDataCollectionInfo.builder()
            .splunkConfig(splunkConfig)
            .accountId(accountId)
            .applicationId(appId)
            .stateExecutionId(stateExecutionId)
            .workflowId(workflowId)
            .workflowExecutionId(workflowExecutionId)
            .serviceId(serviceId)
            .query(splunkState.getQuery())
            .startMinute(0)
            .startMinute(0)
            .collectionTime(Integer.parseInt(splunkState.getTimeDuration()))
            .hosts(Collections.singleton("test"))
            .encryptedDataDetails(Collections.emptyList())
            .build();
    final SplunkDataCollectionInfo actualCollectionInfo = (SplunkDataCollectionInfo) task.getParameters()[0];
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

    LogAnalysisExecutionData logAnalysisExecutionData = LogAnalysisExecutionData.builder().build();
    LogAnalysisResponse logAnalysisResponse = LogAnalysisResponse.Builder.aLogAnalysisResponse()
                                                  .withExecutionStatus(ExecutionStatus.ERROR)
                                                  .withLogAnalysisExecutionData(logAnalysisExecutionData)
                                                  .build();
    Map<String, ResponseData> responseMap = new HashMap<>();
    responseMap.put("somekey", logAnalysisResponse);
    splunkState.handleAsyncResponse(executionContext, responseMap);

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
  public void handleAsyncSummaryFail() {
    LogAnalysisExecutionData logAnalysisExecutionData =
        LogAnalysisExecutionData.builder()
            .correlationId(UUID.randomUUID().toString())
            .stateExecutionInstanceId(stateExecutionId)
            .serverConfigId(UUID.randomUUID().toString())
            .query(splunkState.getQuery())
            .timeDuration(Integer.parseInt(splunkState.getTimeDuration()))
            .canaryNewHostNames(Sets.newHashSet("test1", "test2"))
            .lastExecutionNodes(Sets.newHashSet("control1", "control2", "control3"))
            .build();

    logAnalysisExecutionData.setErrorMsg(UUID.randomUUID().toString());

    LogAnalysisResponse response = LogAnalysisResponse.Builder.aLogAnalysisResponse()
                                       .withExecutionStatus(ExecutionStatus.ERROR)
                                       .withLogAnalysisExecutionData(logAnalysisExecutionData)
                                       .build();

    Map<String, ResponseData> responseMap = new HashMap<>();
    responseMap.put("somekey", response);

    ExecutionResponse executionResponse = splunkState.handleAsyncResponse(executionContext, responseMap);
    assertEquals(ExecutionStatus.ERROR, executionResponse.getExecutionStatus());
    assertEquals(logAnalysisExecutionData.getErrorMsg(), executionResponse.getErrorMessage());
    assertEquals(logAnalysisExecutionData, executionResponse.getStateExecutionData());
  }

  @Test
  public void handleAsyncSummaryPassNoData() {
    LogAnalysisExecutionData logAnalysisExecutionData =
        LogAnalysisExecutionData.builder()
            .correlationId(UUID.randomUUID().toString())
            .stateExecutionInstanceId(stateExecutionId)
            .serverConfigId(UUID.randomUUID().toString())
            .query(splunkState.getQuery())
            .timeDuration(Integer.parseInt(splunkState.getTimeDuration()))
            .canaryNewHostNames(Sets.newHashSet("test1", "test2"))
            .lastExecutionNodes(Sets.newHashSet("control1", "control2", "control3"))
            .build();

    logAnalysisExecutionData.setErrorMsg(UUID.randomUUID().toString());

    LogAnalysisResponse response = LogAnalysisResponse.Builder.aLogAnalysisResponse()
                                       .withExecutionStatus(ExecutionStatus.SUCCESS)
                                       .withLogAnalysisExecutionData(logAnalysisExecutionData)
                                       .build();

    Map<String, ResponseData> responseMap = new HashMap<>();
    responseMap.put("somekey", response);

    SplunkV2State spyState = spy(splunkState);
    doReturn(Collections.singletonMap("test", DEFAULT_GROUP_NAME))
        .when(spyState)
        .getCanaryNewHostNames(executionContext);
    doReturn(Collections.singletonMap("control", DEFAULT_GROUP_NAME))
        .when(spyState)
        .getLastExecutionNodes(executionContext);
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

  @Test
  public void testTimestampFormat() {
    SimpleDateFormat sdf = new SimpleDateFormat(ElkAnalysisState.DEFAULT_TIME_FORMAT);
    assertNotNull(sdf.parse("2013-10-07T12:13:27.001Z", new ParsePosition(0)));
  }
}
