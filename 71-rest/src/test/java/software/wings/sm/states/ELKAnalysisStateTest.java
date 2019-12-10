package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.sm.states.ElkAnalysisState.DEFAULT_TIME_FIELD;

import com.google.common.collect.Sets;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.Status;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.rule.OwnerRule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.AccountType;
import software.wings.beans.Application;
import software.wings.beans.ElkConfig;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.impl.analysis.ElkValidationType;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.service.impl.elk.ElkDataCollectionInfoV2;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.service.intfc.verification.CVActivityLogService.Logger;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;
import software.wings.verification.VerificationDataAnalysisResponse;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.text.ParseException;
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
public class ELKAnalysisStateTest extends APMStateVerificationTestBase {
  @Mock private ElkAnalysisService elkAnalysisService;
  private ElkAnalysisState elkAnalysisState;
  @Mock private Logger activityLogger;

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

    elkAnalysisState = new ElkAnalysisState("ElkAnalysisState");
    elkAnalysisState.setQuery("exception");
    elkAnalysisState.setTimeDuration("15");
    setupCommonFields(elkAnalysisState);
    FieldUtils.writeField(elkAnalysisState, "elkAnalysisService", elkAnalysisService, true);
    FieldUtils.writeField(elkAnalysisState, "accountService", accountService, true);
    when(cvActivityLogService.getLoggerByStateExecutionId(anyString())).thenReturn(activityLogger);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testDefaultComparsionStrategy() {
    ElkAnalysisState elkAnalysisState = new ElkAnalysisState("ElkAnalysisState");
    assertThat(elkAnalysisState.getComparisonStrategy()).isEqualTo(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void noTestNodes() {
    ElkAnalysisState spyState = spy(elkAnalysisState);
    doReturn(false).when(spyState).isNewInstanceFieldPopulated(any());
    doReturn(Collections.emptyMap()).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.emptyMap()).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse response = spyState.execute(executionContext);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(response.getErrorMessage()).isEqualTo("Could not find hosts to analyze!");

    LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.ELK);
    assertThat(analysisSummary.getRiskLevel()).isEqualTo(RiskLevel.NA);
    assertThat(analysisSummary.getQuery()).isEqualTo(elkAnalysisState.getQuery());
    assertThat(analysisSummary.getAnalysisSummaryMessage()).isEqualTo(response.getErrorMessage());
    assertThat(analysisSummary.getControlClusters().isEmpty()).isTrue();
    assertThat(analysisSummary.getTestClusters().isEmpty()).isTrue();
    assertThat(analysisSummary.getUnknownClusters().isEmpty()).isTrue();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void noControlNodesCompareWithCurrent() {
    elkAnalysisState.setComparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_CURRENT.name());
    ElkAnalysisState spyState = spy(elkAnalysisState);
    doReturn(false).when(spyState).isNewInstanceFieldPopulated(any());
    doReturn(Collections.singletonMap("some-host", DEFAULT_GROUP_NAME))
        .when(spyState)
        .getCanaryNewHostNames(executionContext);
    doReturn(Collections.emptyMap()).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);
    String analysisResponseMsg =
        "Skipping analysis due to lack of baseline hosts. Make sure you have at least two phases defined.";
    ExecutionResponse response = spyState.execute(executionContext);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(response.getErrorMessage()).isEqualTo(analysisResponseMsg);
    verify(activityLogger, times(1)).info(eq(analysisResponseMsg));
    LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.ELK);
    assertThat(analysisSummary.getRiskLevel()).isEqualTo(RiskLevel.NA);
    assertThat(analysisSummary.getQuery()).isEqualTo(elkAnalysisState.getQuery());
    assertThat(analysisSummary.getAnalysisSummaryMessage()).isEqualTo(response.getErrorMessage());
    assertThat(analysisSummary.getControlClusters().isEmpty()).isTrue();
    assertThat(analysisSummary.getTestClusters().isEmpty()).isTrue();
    assertThat(analysisSummary.getUnknownClusters().isEmpty()).isTrue();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void compareWithCurrentSameTestAndControlNodes() {
    elkAnalysisState.setComparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_CURRENT.name());
    ElkAnalysisState spyState = spy(elkAnalysisState);
    doReturn(false).when(spyState).isNewInstanceFieldPopulated(any());
    doReturn(new HashMap<>(Collections.singletonMap("some-host", DEFAULT_GROUP_NAME)))
        .when(spyState)
        .getCanaryNewHostNames(executionContext);
    doReturn(new HashMap<>(Collections.singletonMap("some-host", DEFAULT_GROUP_NAME)))
        .when(spyState)
        .getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse response = spyState.execute(executionContext);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(response.getErrorMessage())
        .isEqualTo("Skipping analysis due to lack of baseline hosts. Make sure you have at least two phases defined.");

    LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.ELK);
    assertThat(analysisSummary.getRiskLevel()).isEqualTo(RiskLevel.NA);
    assertThat(analysisSummary.getQuery()).isEqualTo(elkAnalysisState.getQuery());
    assertThat(analysisSummary.getAnalysisSummaryMessage()).isEqualTo(response.getErrorMessage());
    assertThat(analysisSummary.getControlClusters().isEmpty()).isTrue();
    assertThat(analysisSummary.getTestClusters().isEmpty()).isTrue();
    assertThat(analysisSummary.getUnknownClusters().isEmpty()).isTrue();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testTriggerCollection() throws ParseException {
    assertThat(wingsPersistence.createQuery(DelegateTask.class).count()).isEqualTo(0);
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
    doReturn(false).when(spyState).isNewInstanceFieldPopulated(any());
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
    when(elkAnalysisService.validateQuery(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
             anyString(), anyString(), anyString()))
        .thenReturn(true);
    Logger activityLogger = mock(Logger.class);
    when(cvActivityLogService.getLoggerByStateExecutionId(anyString())).thenReturn(activityLogger);
    ExecutionResponse response = spyState.execute(executionContext);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.RUNNING);
    assertThat(response.getErrorMessage()).isEqualTo("Log Verification running.");

    List<DelegateTask> tasks = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority).asList();
    assertThat(tasks).hasSize(1);
    DelegateTask task = tasks.get(0);
    assertThat(task.getData().getTaskType()).isEqualTo(TaskType.ELK_COLLECT_LOG_DATA.name());
    verify(activityLogger).info(contains("Triggered data collection"), anyLong(), anyLong());
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
    final ElkDataCollectionInfo actualCollectionInfo = (ElkDataCollectionInfo) task.getData().getParameters()[0];
    expectedCollectionInfo.setStartTime(actualCollectionInfo.getStartTime());
    assertThat(actualCollectionInfo).isEqualTo(expectedCollectionInfo);
    assertThat(task.getAccountId()).isEqualTo(accountId);
    assertThat(task.getStatus()).isEqualTo(Status.QUEUED);
    assertThat(task.getAppId()).isEqualTo(appId);
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
    assertThat(accountId).isEqualTo(continuousVerificationExecutionMetaData1.getAccountId());
    assertThat("dummy artifact").isEqualTo(continuousVerificationExecutionMetaData1.getArtifactName());
    assertThat(continuousVerificationExecutionMetaData1.getExecutionStatus()).isEqualTo(ExecutionStatus.RUNNING);

    VerificationStateAnalysisExecutionData logAnalysisExecutionData =
        VerificationStateAnalysisExecutionData.builder().build();
    VerificationDataAnalysisResponse logAnalysisResponse =
        VerificationDataAnalysisResponse.builder().stateExecutionData(logAnalysisExecutionData).build();
    logAnalysisResponse.setExecutionStatus(ExecutionStatus.ERROR);
    Map<String, ResponseData> responseMap = new HashMap<>();
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
    assertThat(continuousVerificationExecutionMetaData1.getExecutionStatus()).isEqualTo(ExecutionStatus.ERROR);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreateDataCollectionInfo() throws ParseException, IllegalAccessException {
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
    elkAnalysisState.setTimestampField("@time");
    elkAnalysisState.setComparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_CURRENT.name());
    FieldUtils.writeField(elkAnalysisState, "renderedQuery", "rendered query", true);
    ElkDataCollectionInfoV2 elkDataCollectionInfoV2 =
        (ElkDataCollectionInfoV2) elkAnalysisState.createDataCollectionInfo(
            executionContext, Sets.newHashSet("host1", "host2"));
    assertThat(StateType.ELK).isEqualTo(elkDataCollectionInfoV2.getStateType());
    assertThat(elkConfig).isEqualTo(elkDataCollectionInfoV2.getElkConfig());
    assertThat(executionContext.getStateExecutionInstanceId()).isEqualTo(elkDataCollectionInfoV2.getStateExecutionId());
    assertThat(elkAnalysisState.getMessageField()).isEqualTo(elkDataCollectionInfoV2.getMessageField());
    assertThat(elkAnalysisState.getRenderedQuery()).isEqualTo(elkDataCollectionInfoV2.getQuery());
    assertThat(elkAnalysisState.getTimestampField()).isEqualTo(elkDataCollectionInfoV2.getTimestampField());
    assertThat(elkAnalysisState.getTimestampFormat()).isEqualTo(elkDataCollectionInfoV2.getTimestampFieldFormat());
    assertThat(elkAnalysisState.getIndices()).isEqualTo(elkDataCollectionInfoV2.getIndices());
    assertThat(elkAnalysisState.getQueryType()).isEqualTo(elkDataCollectionInfoV2.getQueryType());
    assertThat(Sets.newHashSet("host1", "host2")).isEqualTo(elkDataCollectionInfoV2.getHosts());
  }
}
