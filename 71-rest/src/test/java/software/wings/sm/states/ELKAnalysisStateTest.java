package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.sm.states.ElkAnalysisState.DEFAULT_TIME_FIELD;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTask.Status;
import software.wings.beans.ElkConfig;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.impl.analysis.ElkValidationType;
import software.wings.service.impl.analysis.LogAnalysisExecutionData;
import software.wings.service.impl.analysis.LogAnalysisResponse;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.elk.ElkAnalysisService;
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
 * Created by rsingh on 10/9/17.
 */
public class ELKAnalysisStateTest extends APMStateVerificationTestBase {
  @Inject private AnalysisService analysisService;

  @Mock private ElkAnalysisService elkAnalysisService;
  private ElkAnalysisState elkAnalysisState;

  @Before
  public void setup() {
    setupCommon();
    MockitoAnnotations.initMocks(this);
    setupCommonMocks();

    elkAnalysisState = new ElkAnalysisState("ElkAnalysisState");
    elkAnalysisState.setQuery("exception");
    elkAnalysisState.setTimeDuration("15");
    setInternalState(elkAnalysisState, "appService", appService);
    setInternalState(elkAnalysisState, "configuration", configuration);
    setInternalState(elkAnalysisState, "analysisService", analysisService);
    setInternalState(elkAnalysisState, "settingsService", settingsService);
    setInternalState(elkAnalysisState, "waitNotifyEngine", waitNotifyEngine);
    setInternalState(elkAnalysisState, "delegateService", delegateService);
    setInternalState(elkAnalysisState, "wingsPersistence", wingsPersistence);
    setInternalState(elkAnalysisState, "secretManager", secretManager);
    setInternalState(elkAnalysisState, "workflowExecutionService", workflowExecutionService);
    setInternalState(elkAnalysisState, "continuousVerificationService", continuousVerificationService);
    setInternalState(elkAnalysisState, "workflowExecutionBaselineService", workflowExecutionBaselineService);
    setInternalState(elkAnalysisState, "featureFlagService", featureFlagService);
    setInternalState(elkAnalysisState, "versionInfoManager", versionInfoManager);
  }

  @Test
  public void testDefaultComparsionStrategy() {
    ElkAnalysisState elkAnalysisState = new ElkAnalysisState("ElkAnalysisState");
    assertEquals(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS, elkAnalysisState.getComparisonStrategy());
  }

  @Test
  public void noTestNodes() {
    ElkAnalysisState spyState = spy(elkAnalysisState);
    doReturn(Collections.emptyMap()).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.emptyMap()).when(spyState).getLastExecutionNodes(executionContext);
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
            .query(elkAnalysisState.getQuery())
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
    Map<String, NotifyResponseData> responseMap = new HashMap<>();
    responseMap.put("somekey", logAnalysisResponse);
    elkAnalysisState.handleAsyncResponse(executionContext, responseMap);

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
