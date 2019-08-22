package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.Sets;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.Status;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import software.wings.beans.AccountType;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.beans.TaskType;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.splunk.SplunkDataCollectionInfo;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.verification.CVActivityLogService.Logger;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;
import software.wings.verification.VerificationDataAnalysisResponse;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by rsingh on 10/9/17.
 */
public class SplunkV2StateTest extends APMStateVerificationTestBase {
  private SplunkV2State splunkState;

  @Before
  public void setup() throws IllegalAccessException {
    setupCommon();
    MockitoAnnotations.initMocks(this);
    setupCommonMocks();

    AppService appService = mock(AppService.class);
    when(appService.getAccountIdByAppId(anyString())).thenReturn(generateUuid());
    when(appService.get(anyString()))
        .thenReturn(Application.Builder.anApplication().name(generateUuid()).accountId(accountId).build());

    AccountService accountService = mock(AccountService.class);
    when(accountService.getAccountType(anyString())).thenReturn(Optional.of(AccountType.PAID));

    splunkState = new SplunkV2State("SplunkState");
    splunkState.setQuery("exception");
    splunkState.setTimeDuration("15");
    setupCommonFields(splunkState);
    FieldUtils.writeField(splunkState, "appService", appService, true);
    FieldUtils.writeField(splunkState, "accountService", accountService, true);
  }

  @Test
  @Category(UnitTests.class)
  public void testDefaultComparsionStrategy() {
    SplunkV2State splunkState = new SplunkV2State("SplunkState");
    assertEquals(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS, splunkState.getComparisonStrategy());
  }

  @Test
  @Category(UnitTests.class)
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
    assertThat(analysisSummary.getControlClusters().isEmpty()).isTrue();
    assertThat(analysisSummary.getTestClusters().isEmpty()).isTrue();
    assertThat(analysisSummary.getUnknownClusters().isEmpty()).isTrue();
  }

  @Test
  @Category(UnitTests.class)
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
    assertThat(analysisSummary.getControlClusters().isEmpty()).isTrue();
    assertThat(analysisSummary.getTestClusters().isEmpty()).isTrue();
    assertThat(analysisSummary.getUnknownClusters().isEmpty()).isTrue();
  }

  @Test
  @Category(UnitTests.class)
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
    assertThat(analysisSummary.getControlClusters().isEmpty()).isTrue();
    assertThat(analysisSummary.getTestClusters().isEmpty()).isTrue();
    assertThat(analysisSummary.getUnknownClusters().isEmpty()).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testTriggerCollection() throws ParseException, IllegalAccessException {
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
    Logger activityLogger = mock(Logger.class);
    when(cvActivityLogService.getLoggerByStateExecutionId(anyString())).thenReturn(activityLogger);
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
        .thenReturn(Environment.Builder.anEnvironment().uuid(UUID.randomUUID().toString()).build());
    when(executionContext.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.RUNNING, response.getExecutionStatus());
    assertEquals(
        "No baseline was set for the workflow. Workflow running with auto baseline. No previous execution found. This will be the baseline run.",
        response.getErrorMessage());

    List<DelegateTask> tasks = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority).asList();
    assertThat(tasks).hasSize(1);
    DelegateTask task = tasks.get(0);
    assertEquals(TaskType.SPLUNK_COLLECT_LOG_DATA.name(), task.getData().getTaskType());
    verify(activityLogger).info(contains("Triggered data collection"), anyLong(), anyLong());
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
    final SplunkDataCollectionInfo actualCollectionInfo = (SplunkDataCollectionInfo) task.getData().getParameters()[0];
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
    assertThat(cvExecutionMetaData).isNotNull();
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

    VerificationStateAnalysisExecutionData logAnalysisExecutionData =
        VerificationStateAnalysisExecutionData.builder().build();
    VerificationDataAnalysisResponse logAnalysisResponse =
        VerificationDataAnalysisResponse.builder().stateExecutionData(logAnalysisExecutionData).build();
    logAnalysisResponse.setExecutionStatus(ExecutionStatus.ERROR);
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
    assertEquals(ExecutionStatus.ERROR, continuousVerificationExecutionMetaData1.getExecutionStatus());
  }

  @Test
  @Category(UnitTests.class)
  public void handleAsyncSummaryFail() {
    VerificationStateAnalysisExecutionData logAnalysisExecutionData =
        VerificationStateAnalysisExecutionData.builder()
            .correlationId(UUID.randomUUID().toString())
            .stateExecutionInstanceId(stateExecutionId)
            .serverConfigId(UUID.randomUUID().toString())
            .query(splunkState.getQuery())
            .timeDuration(Integer.parseInt(splunkState.getTimeDuration()))
            .canaryNewHostNames(Sets.newHashSet("test1", "test2"))
            .lastExecutionNodes(Sets.newHashSet("control1", "control2", "control3"))
            .build();

    logAnalysisExecutionData.setErrorMsg(UUID.randomUUID().toString());

    VerificationDataAnalysisResponse response =
        VerificationDataAnalysisResponse.builder().stateExecutionData(logAnalysisExecutionData).build();
    response.setExecutionStatus(ExecutionStatus.ERROR);

    Map<String, ResponseData> responseMap = new HashMap<>();
    responseMap.put("somekey", response);

    ExecutionResponse executionResponse = splunkState.handleAsyncResponse(executionContext, responseMap);
    assertEquals(ExecutionStatus.ERROR, executionResponse.getExecutionStatus());
    assertEquals(logAnalysisExecutionData.getErrorMsg(), executionResponse.getErrorMessage());
    assertEquals(logAnalysisExecutionData, executionResponse.getStateExecutionData());
  }

  @Test
  @Category(UnitTests.class)
  public void handleAsyncSummaryPassNoData() {
    doReturn("exception").when(executionContext).renderExpression(anyString());
    VerificationStateAnalysisExecutionData logAnalysisExecutionData =
        VerificationStateAnalysisExecutionData.builder()
            .correlationId(UUID.randomUUID().toString())
            .stateExecutionInstanceId(stateExecutionId)
            .serverConfigId(UUID.randomUUID().toString())
            .query(splunkState.getQuery())
            .timeDuration(Integer.parseInt(splunkState.getTimeDuration()))
            .canaryNewHostNames(Sets.newHashSet("test1", "test2"))
            .lastExecutionNodes(Sets.newHashSet("control1", "control2", "control3"))
            .build();

    logAnalysisExecutionData.setErrorMsg(UUID.randomUUID().toString());

    VerificationDataAnalysisResponse response =
        VerificationDataAnalysisResponse.builder().stateExecutionData(logAnalysisExecutionData).build();
    response.setExecutionStatus(ExecutionStatus.SUCCESS);

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

    wingsPersistence.save(AnalysisContext.builder()
                              .stateExecutionId(executionContext.getStateExecutionInstanceId())
                              .appId(appId)
                              .query(splunkState.getQuery())
                              .build());
    ExecutionResponse executionResponse = spyState.handleAsyncResponse(executionContext, responseMap);
    assertEquals(ExecutionStatus.SUCCESS, executionResponse.getExecutionStatus());
    assertEquals("No data found with given queries. Skipped Analysis", executionResponse.getErrorMessage());

    LogMLAnalysisSummary analysisSummary =
        analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SPLUNKV2);
    assertEquals(RiskLevel.NA, analysisSummary.getRiskLevel());
    assertEquals(splunkState.getQuery(), analysisSummary.getQuery());
    assertEquals(executionResponse.getErrorMessage(), analysisSummary.getAnalysisSummaryMessage());
    assertThat(analysisSummary.getControlClusters().isEmpty()).isTrue();
    assertThat(analysisSummary.getTestClusters().isEmpty()).isTrue();
    assertThat(analysisSummary.getUnknownClusters().isEmpty()).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testTimestampFormat() {
    SimpleDateFormat sdf = new SimpleDateFormat(ElkAnalysisState.DEFAULT_TIME_FORMAT);
    assertThat(sdf.parse("2013-10-07T12:13:27.001Z", new ParsePosition(0))).isNotNull();
  }
}
