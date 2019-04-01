package io.harness.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;
import static software.wings.common.VerificationConstants.VERIFICATION_SERVICE_BASE_URL;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.service.intfc.analysis.LogAnalysisResource.ANALYSIS_GET_24X7_ALL_LOGS_URL;
import static software.wings.service.intfc.analysis.LogAnalysisResource.ANALYSIS_GET_24X7_LOG_URL;
import static software.wings.service.intfc.analysis.LogAnalysisResource.ANALYSIS_STATE_SAVE_24X7_CLUSTERED_LOG_URL;
import static software.wings.service.intfc.analysis.LogAnalysisResource.LOG_ANALYSIS;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.VerificationBaseTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.rest.RestResponse;
import io.harness.service.intfc.ContinuousVerificationService;
import io.harness.time.Timestamp;
import io.harness.waiter.WaitNotifyEngine;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.beans.DelegateTask;
import software.wings.beans.SumoConfig;
import software.wings.beans.TaskType;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.ContinuousVerificationServiceImpl;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.sumo.SumoDataCollectionInfo;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ContinuousVerificationServiceTest extends VerificationBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(ContinuousVerificationServiceTest.class);
  private String accountId, appId, envId, serviceId, connectorId, query, cvConfigId, workflowId, workflowExecutionId,
      stateExecutionId;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ContinuousVerificationService continuousVerificationService;
  @Mock private CVConfigurationService cvConfigurationService;
  @Mock private HarnessMetricRegistry metricRegistry;
  @Mock private VerificationManagerClient verificationManagerClient;
  @Mock private DelegateService delegateService;
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @Mock private SettingsService settingsService;
  @Mock private SecretManager secretManager;
  private SumoConfig sumoConfig;

  @Before
  public void setUp() {
    accountId = generateUuid();
    appId = generateUuid();
    envId = generateUuid();
    serviceId = generateUuid();
    connectorId = generateUuid();
    query = generateUuid();
    workflowId = generateUuid();
    workflowExecutionId = generateUuid();
    stateExecutionId = generateUuid();

    sumoConfig = SumoConfig.builder()
                     .sumoUrl(generateUuid())
                     .accountId(accountId)
                     .accessKey(generateUuid().toCharArray())
                     .accessId(generateUuid().toCharArray())
                     .build();
    LogsCVConfiguration logsCVConfiguration = new LogsCVConfiguration();
    logsCVConfiguration.setName(generateUuid());
    logsCVConfiguration.setAccountId(accountId);
    logsCVConfiguration.setAppId(appId);
    logsCVConfiguration.setEnvId(envId);
    logsCVConfiguration.setServiceId(serviceId);
    logsCVConfiguration.setEnabled24x7(true);
    logsCVConfiguration.setConnectorId(connectorId);
    logsCVConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
    logsCVConfiguration.setStateType(StateType.SUMO);
    logsCVConfiguration.setBaselineStartMinute(
        TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()) - TimeUnit.DAYS.toMinutes(1));
    logsCVConfiguration.setBaselineEndMinute(TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()));

    cvConfigId = wingsPersistence.save(logsCVConfiguration);
    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Lists.newArrayList(logsCVConfiguration));
    setInternalState(continuousVerificationService, "cvConfigurationService", cvConfigurationService);
    setInternalState(continuousVerificationService, "metricRegistry", metricRegistry);

    when(delegateService.queueTask(anyObject()))
        .then(invocation -> wingsPersistence.save((DelegateTask) invocation.getArguments()[0]));
    when(settingsService.get(connectorId)).thenReturn(aSettingAttribute().withValue(sumoConfig).build());
    when(secretManager.getEncryptionDetails(anyObject(), anyString(), anyString())).thenReturn(Collections.emptyList());
    software.wings.service.impl.analysis.ContinuousVerificationService managerVerificationService =
        new ContinuousVerificationServiceImpl();
    setInternalState(managerVerificationService, "delegateService", delegateService);
    setInternalState(managerVerificationService, "waitNotifyEngine", waitNotifyEngine);
    setInternalState(managerVerificationService, "wingsPersistence", wingsPersistence);
    setInternalState(managerVerificationService, "settingsService", settingsService);
    setInternalState(managerVerificationService, "secretManager", secretManager);

    when(verificationManagerClient.triggerCVDataCollection(anyString(), anyObject(), anyLong(), anyLong()))
        .then(invocation -> {
          Object[] args = invocation.getArguments();
          managerVerificationService.collect247Data(
              (String) args[0], (StateType) args[1], (long) args[2], (Long) args[3]);
          Call<Boolean> restCall = mock(Call.class);
          when(restCall.execute()).thenReturn(Response.success(true));
          return restCall;
        });

    when(verificationManagerClient.triggerWorkflowDataCollection(anyString(), anyLong())).then(invocation -> {
      Object[] args = invocation.getArguments();
      managerVerificationService.collectCVDataForWorkflow((String) args[0], (long) args[1]);
      Call<Boolean> restCall = mock(Call.class);
      when(restCall.execute()).thenReturn(Response.success(true));
      return restCall;
    });
    setInternalState(continuousVerificationService, "verificationManagerClient", verificationManagerClient);
  }

  @Test
  @Category(UnitTests.class)
  public void testDefaultBaseline() {
    LogsCVConfiguration logsCVConfiguration = new LogsCVConfiguration();
    logsCVConfiguration.setName(generateUuid());
    logsCVConfiguration.setAccountId(accountId);
    logsCVConfiguration.setAppId(appId);
    logsCVConfiguration.setEnvId(envId);
    logsCVConfiguration.setServiceId(serviceId);
    logsCVConfiguration.setEnabled24x7(true);
    logsCVConfiguration.setConnectorId(connectorId);
    logsCVConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
    logsCVConfiguration.setStateType(StateType.SUMO);

    cvConfigId = wingsPersistence.save(logsCVConfiguration);

    logsCVConfiguration = (LogsCVConfiguration) wingsPersistence.get(CVConfiguration.class, cvConfigId);
    assertTrue(logsCVConfiguration.getBaselineStartMinute() < 0);
    assertTrue(logsCVConfiguration.getBaselineEndMinute() < 0);
  }

  @Test
  @Category(UnitTests.class)
  public void testLogsCollectionBaselineInFuture() {
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    logger.info("currentMin: {}", currentMinute);
    LogsCVConfiguration logsCVConfiguration =
        (LogsCVConfiguration) wingsPersistence.get(CVConfiguration.class, cvConfigId);
    logsCVConfiguration.setBaselineStartMinute(currentMinute + CRON_POLL_INTERVAL_IN_MINUTES);
    logsCVConfiguration.setBaselineEndMinute(
        logsCVConfiguration.getBaselineStartMinute() + CRON_POLL_INTERVAL_IN_MINUTES * 3);
    wingsPersistence.save(logsCVConfiguration);
    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Lists.newArrayList(logsCVConfiguration));
    continuousVerificationService.triggerLogDataCollection(accountId);
    List<DelegateTask> delegateTasks =
        wingsPersistence.createQuery(DelegateTask.class).filter("accountId", accountId).asList();
    assertEquals(0, delegateTasks.size());

    logsCVConfiguration.setBaselineStartMinute(currentMinute - 2);
    continuousVerificationService.triggerLogDataCollection(accountId);
    delegateTasks = wingsPersistence.createQuery(DelegateTask.class).filter("accountId", accountId).asList();
    assertEquals(0, delegateTasks.size());

    logsCVConfiguration.setBaselineStartMinute(currentMinute - 20);

    continuousVerificationService.triggerLogDataCollection(accountId);
    delegateTasks = wingsPersistence.createQuery(DelegateTask.class).filter("accountId", accountId).asList();
    assertEquals(1, delegateTasks.size());

    DelegateTask delegateTask = delegateTasks.get(0);
    assertEquals(accountId, delegateTask.getAccountId());
    assertEquals(appId, delegateTask.getAppId());
    assertEquals(TaskType.SUMO_COLLECT_24_7_LOG_DATA, TaskType.valueOf(delegateTask.getData().getTaskType()));
    SumoDataCollectionInfo sumoDataCollectionInfo = (SumoDataCollectionInfo) delegateTask.getData().getParameters()[0];
    assertEquals(sumoConfig, sumoDataCollectionInfo.getSumoConfig());
    assertEquals(cvConfigId, sumoDataCollectionInfo.getCvConfigId());
    assertEquals(appId, sumoDataCollectionInfo.getApplicationId());
    assertEquals(accountId, sumoDataCollectionInfo.getAccountId());
    assertEquals(serviceId, sumoDataCollectionInfo.getServiceId());

    assertEquals(
        TimeUnit.MINUTES.toMillis(logsCVConfiguration.getBaselineStartMinute()), sumoDataCollectionInfo.getStartTime());
    assertEquals(
        TimeUnit.MINUTES.toMillis(logsCVConfiguration.getBaselineStartMinute() + CRON_POLL_INTERVAL_IN_MINUTES - 1),
        sumoDataCollectionInfo.getEndTime());
  }

  @Test
  @Category(UnitTests.class)
  public void testLogsCollectionNoBaselineSet() {
    LogsCVConfiguration logsCVConfiguration =
        (LogsCVConfiguration) wingsPersistence.get(CVConfiguration.class, cvConfigId);
    logsCVConfiguration.setBaselineStartMinute(-1);
    logsCVConfiguration.setBaselineEndMinute(-1);
    wingsPersistence.save(logsCVConfiguration);
    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Lists.newArrayList(logsCVConfiguration));
    continuousVerificationService.triggerLogDataCollection(accountId);
    List<DelegateTask> delegateTasks =
        wingsPersistence.createQuery(DelegateTask.class).filter("accountId", accountId).asList();
    assertEquals(0, delegateTasks.size());
  }

  @Test
  @Category(UnitTests.class)
  public void testLogsCollection() {
    continuousVerificationService.triggerLogDataCollection(accountId);
    List<DelegateTask> delegateTasks =
        wingsPersistence.createQuery(DelegateTask.class).filter("accountId", accountId).asList();
    assertEquals(1, delegateTasks.size());
    DelegateTask delegateTask = delegateTasks.get(0);
    assertEquals(accountId, delegateTask.getAccountId());
    assertEquals(appId, delegateTask.getAppId());
    assertEquals(TaskType.SUMO_COLLECT_24_7_LOG_DATA, TaskType.valueOf(delegateTask.getData().getTaskType()));
    SumoDataCollectionInfo sumoDataCollectionInfo = (SumoDataCollectionInfo) delegateTask.getData().getParameters()[0];
    assertEquals(sumoConfig, sumoDataCollectionInfo.getSumoConfig());
    assertEquals(cvConfigId, sumoDataCollectionInfo.getCvConfigId());
    assertEquals(appId, sumoDataCollectionInfo.getApplicationId());
    assertEquals(accountId, sumoDataCollectionInfo.getAccountId());
    assertEquals(serviceId, sumoDataCollectionInfo.getServiceId());

    LogsCVConfiguration logsCVConfiguration =
        (LogsCVConfiguration) wingsPersistence.get(CVConfiguration.class, cvConfigId);
    assertEquals(
        TimeUnit.MINUTES.toMillis(logsCVConfiguration.getBaselineStartMinute()), sumoDataCollectionInfo.getStartTime());
    assertEquals(
        TimeUnit.MINUTES.toMillis(logsCVConfiguration.getBaselineStartMinute() + CRON_POLL_INTERVAL_IN_MINUTES - 1),
        sumoDataCollectionInfo.getEndTime());

    // save some log and trigger again
    long numOfMinutesSaved = 100;
    for (long i = logsCVConfiguration.getBaselineStartMinute();
         i <= logsCVConfiguration.getBaselineStartMinute() + numOfMinutesSaved; i++) {
      LogDataRecord logDataRecord = new LogDataRecord();
      logDataRecord.setAppId(appId);
      logDataRecord.setCvConfigId(cvConfigId);
      logDataRecord.setLogCollectionMinute((int) i);
      logDataRecord.setClusterLevel(ClusterLevel.H0);
      wingsPersistence.save(logDataRecord);
    }
    continuousVerificationService.triggerLogDataCollection(accountId);
    delegateTasks = wingsPersistence.createQuery(DelegateTask.class).filter("accountId", accountId).asList();
    assertEquals(2, delegateTasks.size());

    delegateTask = delegateTasks.get(1);
    assertEquals(accountId, delegateTask.getAccountId());
    assertEquals(appId, delegateTask.getAppId());
    assertEquals(TaskType.SUMO_COLLECT_24_7_LOG_DATA, TaskType.valueOf(delegateTask.getData().getTaskType()));
    sumoDataCollectionInfo = (SumoDataCollectionInfo) delegateTask.getData().getParameters()[0];
    assertEquals(sumoConfig, sumoDataCollectionInfo.getSumoConfig());
    assertEquals(cvConfigId, sumoDataCollectionInfo.getCvConfigId());
    assertEquals(appId, sumoDataCollectionInfo.getApplicationId());
    assertEquals(accountId, sumoDataCollectionInfo.getAccountId());
    assertEquals(serviceId, sumoDataCollectionInfo.getServiceId());

    assertEquals(TimeUnit.MINUTES.toMillis(logsCVConfiguration.getBaselineStartMinute() + numOfMinutesSaved + 1),
        sumoDataCollectionInfo.getStartTime());
    assertEquals(TimeUnit.MINUTES.toMillis(
                     logsCVConfiguration.getBaselineStartMinute() + CRON_POLL_INTERVAL_IN_MINUTES + numOfMinutesSaved),
        sumoDataCollectionInfo.getEndTime());
  }

  @Test
  @Category(UnitTests.class)
  public void testTriggerLogsCollection() throws IOException {
    Call<RestResponse<Boolean>> managerCall = mock(Call.class);
    when(managerCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isStateValid(anyString(), anyString())).thenReturn(managerCall);
    AnalysisContext context =
        createMockAnalysisContext(TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary()));
    wingsPersistence.save(context);
    continuousVerificationService.triggerLogDataCollection(context);
    List<DelegateTask> delegateTasks =
        wingsPersistence.createQuery(DelegateTask.class).filter("accountId", accountId).asList();
    assertEquals(1, delegateTasks.size());
    DelegateTask delegateTask = delegateTasks.get(0);

    assertEquals(accountId, delegateTask.getAccountId());
    assertEquals(appId, delegateTask.getAppId());
    assertEquals(TaskType.SUMO_COLLECT_LOG_DATA, TaskType.valueOf(delegateTask.getData().getTaskType()));
    SumoDataCollectionInfo sumoDataCollectionInfo = (SumoDataCollectionInfo) delegateTask.getData().getParameters()[0];
    assertEquals(sumoConfig, sumoDataCollectionInfo.getSumoConfig());
    assertEquals(appId, sumoDataCollectionInfo.getApplicationId());
    assertEquals(accountId, sumoDataCollectionInfo.getAccountId());
    assertEquals(serviceId, sumoDataCollectionInfo.getServiceId());
  }

  @Test
  @Category(UnitTests.class)
  public void testTriggerLogsCollectionInvalidState() throws IOException {
    Call<RestResponse<Boolean>> managerCall = mock(Call.class);
    when(managerCall.execute()).thenReturn(Response.success(new RestResponse<>(false)));
    when(verificationManagerClient.isStateValid(anyString(), anyString())).thenReturn(managerCall);
    AnalysisContext context =
        createMockAnalysisContext(TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary()));
    wingsPersistence.save(context);
    boolean isTriggered = continuousVerificationService.triggerLogDataCollection(context);
    assertFalse(isTriggered);
  }

  @Test
  @Category(UnitTests.class)
  public void testTriggerLogsCollectionCompletedCollection() throws IOException {
    Call<RestResponse<Boolean>> managerCall = mock(Call.class);
    when(managerCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isStateValid(anyString(), anyString())).thenReturn(managerCall);
    long startTimeInterval = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    AnalysisContext context = createMockAnalysisContext(startTimeInterval);
    wingsPersistence.save(context);

    LogDataRecord record = createLogDataRecord(startTimeInterval);
    wingsPersistence.save(record);

    boolean isTriggered = continuousVerificationService.triggerLogDataCollection(context);
    assertFalse(isTriggered);
  }

  @Test
  @Category(UnitTests.class)
  public void testTriggerLogsCollectionNextMinuteDataCollection() throws IOException {
    Call<RestResponse<Boolean>> managerCall = mock(Call.class);
    when(managerCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isStateValid(anyString(), anyString())).thenReturn(managerCall);
    long startTimeInterval = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    AnalysisContext context = createMockAnalysisContext(startTimeInterval);
    context.setTimeDuration(2);
    wingsPersistence.save(context);

    LogDataRecord record = createLogDataRecord(startTimeInterval);
    wingsPersistence.save(record);

    boolean isTriggered = continuousVerificationService.triggerLogDataCollection(context);
    assertTrue(isTriggered);
  }

  private LogDataRecord createLogDataRecord(long startTimeInterval) {
    LogDataRecord record = new LogDataRecord();
    record.setStateType(StateType.SUMO);
    record.setWorkflowId(workflowId);
    record.setLogCollectionMinute(startTimeInterval);
    record.setQuery(query);
    record.setAppId(appId);
    record.setStateExecutionId(stateExecutionId);
    return record;
  }

  private AnalysisContext createMockAnalysisContext(long startTimeInterval) {
    return AnalysisContext.builder()
        .accountId(accountId)
        .appId(appId)
        .workflowId(workflowId)
        .query(query)
        .workflowExecutionId(workflowExecutionId)
        .stateExecutionId(stateExecutionId)
        .serviceId(serviceId)
        .controlNodes(Collections.singletonMap("host1", DEFAULT_GROUP_NAME))
        .testNodes(Collections.singletonMap("host1", DEFAULT_GROUP_NAME))
        .isSSL(true)
        .analysisServerConfigId(connectorId)
        .appPort(9090)
        .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
        .timeDuration(1)
        .stateType(StateType.SUMO)
        .correlationId(UUID.randomUUID().toString())
        .prevWorkflowExecutionId("-1")
        .startDataCollectionMinute(startTimeInterval)
        .build();
  }

  @Test
  @Category(UnitTests.class)
  public void testLogsL1Clustering() {
    continuousVerificationService.triggerLogsL1Clustering(accountId);
    List<LearningEngineAnalysisTask> learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertEquals(0, learningEngineAnalysisTasks.size());

    int numOfMinutes = 5;
    int numOfHosts = 3;

    LogDataRecord logDataRecord = new LogDataRecord();
    logDataRecord.setAppId(appId);
    logDataRecord.setCvConfigId(cvConfigId);
    logDataRecord.setStateType(StateType.SUMO);
    logDataRecord.setClusterLevel(ClusterLevel.H0);

    for (int i = 0; i < numOfMinutes; i++) {
      for (int j = 0; j < numOfHosts; j++) {
        logDataRecord.setUuid(null);
        logDataRecord.setHost("host-" + j);
        logDataRecord.setLogCollectionMinute(100 + i);
        wingsPersistence.save(logDataRecord);
      }
    }

    continuousVerificationService.triggerLogsL1Clustering(accountId);
    learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();

    Set<String> hosts = new HashSet<>();
    for (int i = 0; i < numOfHosts; i++) {
      hosts.add("host-" + i);
    }
    assertEquals(numOfMinutes, learningEngineAnalysisTasks.size());
    for (int i = 0; i < numOfMinutes; i++) {
      LearningEngineAnalysisTask learningEngineAnalysisTask = learningEngineAnalysisTasks.get(i);
      assertNull(learningEngineAnalysisTask.getWorkflow_id());
      assertNull(learningEngineAnalysisTask.getWorkflow_execution_id());
      assertEquals(
          "LOGS_CLUSTER_L1_" + cvConfigId + "_" + (100 + i), learningEngineAnalysisTask.getState_execution_id());
      assertEquals(serviceId, learningEngineAnalysisTask.getService_id());
      assertEquals(100 + i, learningEngineAnalysisTask.getAnalysis_minute());
      assertEquals(VERIFICATION_SERVICE_BASE_URL + "/" + LOG_ANALYSIS + ANALYSIS_GET_24X7_LOG_URL
              + "?cvConfigId=" + cvConfigId + "&appId=" + appId + "&clusterLevel=L0&logCollectionMinute=" + (100 + i),
          learningEngineAnalysisTask.getControl_input_url());
      assertNull(learningEngineAnalysisTask.getTest_input_url());
      assertEquals(VERIFICATION_SERVICE_BASE_URL + "/" + LOG_ANALYSIS + ANALYSIS_STATE_SAVE_24X7_CLUSTERED_LOG_URL
              + "?cvConfigId=" + cvConfigId + "&appId=" + appId + "&clusterLevel=L1&logCollectionMinute=" + (100 + i),
          learningEngineAnalysisTask.getAnalysis_save_url());
      assertEquals(hosts, learningEngineAnalysisTask.getControl_nodes());
      assertNull(learningEngineAnalysisTask.getTest_nodes());
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testLogsL2Clustering() {
    continuousVerificationService.triggerLogsL2Clustering(accountId);
    List<LearningEngineAnalysisTask> learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertEquals(0, learningEngineAnalysisTasks.size());

    int numOfMinutes = CRON_POLL_INTERVAL_IN_MINUTES - 5;
    int numOfHosts = 3;

    LogDataRecord logDataRecord = new LogDataRecord();
    logDataRecord.setAppId(appId);
    logDataRecord.setCvConfigId(cvConfigId);
    logDataRecord.setStateType(StateType.SUMO);
    logDataRecord.setClusterLevel(ClusterLevel.H1);

    for (int i = 0; i < numOfMinutes; i++) {
      for (int j = 0; j < numOfHosts; j++) {
        logDataRecord.setUuid(null);
        logDataRecord.setClusterLevel(ClusterLevel.H1);
        logDataRecord.setHost("host-" + j);
        logDataRecord.setLogCollectionMinute(100 + i);
        wingsPersistence.save(logDataRecord);

        logDataRecord.setUuid(null);
        logDataRecord.setClusterLevel(ClusterLevel.L0);
        wingsPersistence.save(logDataRecord);
      }
    }

    continuousVerificationService.triggerLogsL2Clustering(accountId);
    learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertEquals(0, learningEngineAnalysisTasks.size());

    for (int i = numOfMinutes; i < CRON_POLL_INTERVAL_IN_MINUTES; i++) {
      for (int j = 0; j < numOfHosts; j++) {
        logDataRecord.setUuid(null);
        logDataRecord.setClusterLevel(ClusterLevel.H1);
        logDataRecord.setHost("host-" + j);
        logDataRecord.setLogCollectionMinute(100 + i);
        wingsPersistence.save(logDataRecord);

        logDataRecord.setUuid(null);
        logDataRecord.setClusterLevel(ClusterLevel.L0);
        wingsPersistence.save(logDataRecord);
      }
    }

    continuousVerificationService.triggerLogsL2Clustering(accountId);
    learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertEquals(0, learningEngineAnalysisTasks.size());

    wingsPersistence.delete(wingsPersistence.createQuery(LogDataRecord.class).filter("clusterLevel", ClusterLevel.L0));

    continuousVerificationService.triggerLogsL2Clustering(accountId);
    learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertEquals(1, learningEngineAnalysisTasks.size());
    final int clusterMinute = 100 + CRON_POLL_INTERVAL_IN_MINUTES - 1;
    LearningEngineAnalysisTask learningEngineAnalysisTask = learningEngineAnalysisTasks.get(0);
    validateL2Clustering(learningEngineAnalysisTask, clusterMinute);
  }

  @Test
  @Category(UnitTests.class)
  public void testLogsL2ClusteringRetry() {
    continuousVerificationService.triggerLogsL2Clustering(accountId);
    List<LearningEngineAnalysisTask> learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertEquals(0, learningEngineAnalysisTasks.size());

    int numOfMinutes = CRON_POLL_INTERVAL_IN_MINUTES - 5;
    int numOfHosts = 3;

    LogDataRecord logDataRecordInRetry = new LogDataRecord();
    logDataRecordInRetry.setAppId(appId);
    logDataRecordInRetry.setCvConfigId(cvConfigId);
    logDataRecordInRetry.setStateType(StateType.SUMO);
    logDataRecordInRetry.setClusterLevel(ClusterLevel.H1);

    for (int i = 0; i < numOfMinutes; i++) {
      for (int j = 0; j < numOfHosts; j++) {
        logDataRecordInRetry.setUuid(null);
        logDataRecordInRetry.setClusterLevel(ClusterLevel.H1);
        logDataRecordInRetry.setHost("host-" + j);
        logDataRecordInRetry.setLogCollectionMinute(100 + i);
        wingsPersistence.save(logDataRecordInRetry);

        logDataRecordInRetry.setUuid(null);
        logDataRecordInRetry.setClusterLevel(ClusterLevel.L0);
        wingsPersistence.save(logDataRecordInRetry);
      }
    }
    final int clusterMinute = 100 + CRON_POLL_INTERVAL_IN_MINUTES - 1;
    createFailedLETask("LOGS_CLUSTER_L2_" + cvConfigId + "_" + clusterMinute, null, null, clusterMinute);
    continuousVerificationService.triggerLogsL2Clustering(accountId);
    learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertEquals(1, learningEngineAnalysisTasks.size());

    for (int i = numOfMinutes; i < CRON_POLL_INTERVAL_IN_MINUTES; i++) {
      for (int j = 0; j < numOfHosts; j++) {
        logDataRecordInRetry.setUuid(null);
        logDataRecordInRetry.setClusterLevel(ClusterLevel.H1);
        logDataRecordInRetry.setHost("host-" + j);
        logDataRecordInRetry.setLogCollectionMinute(100 + i);
        wingsPersistence.save(logDataRecordInRetry);

        logDataRecordInRetry.setUuid(null);
        logDataRecordInRetry.setClusterLevel(ClusterLevel.L0);
        wingsPersistence.save(logDataRecordInRetry);
      }
    }

    continuousVerificationService.triggerLogsL2Clustering(accountId);
    learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertEquals(1, learningEngineAnalysisTasks.size());

    wingsPersistence.delete(wingsPersistence.createQuery(LogDataRecord.class).filter("clusterLevel", ClusterLevel.L0));

    createFailedLETask("LOGS_CLUSTER_L2_" + cvConfigId + "_" + clusterMinute, null, null, clusterMinute);
    continuousVerificationService.triggerLogsL2Clustering(accountId);
    learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertEquals(3, learningEngineAnalysisTasks.size());

    LearningEngineAnalysisTask learningEngineAnalysisTask = learningEngineAnalysisTasks.get(2);
    validateL2Clustering(learningEngineAnalysisTask, clusterMinute);
  }

  private void validateL2Clustering(LearningEngineAnalysisTask learningEngineAnalysisTask, int clusterMinute) {
    assertNull(learningEngineAnalysisTask.getWorkflow_id());
    assertNull(learningEngineAnalysisTask.getWorkflow_execution_id());
    assertEquals(
        "LOGS_CLUSTER_L2_" + cvConfigId + "_" + clusterMinute, learningEngineAnalysisTask.getState_execution_id());
    assertEquals(serviceId, learningEngineAnalysisTask.getService_id());
    assertEquals(clusterMinute, learningEngineAnalysisTask.getAnalysis_minute());
    assertEquals(VERIFICATION_SERVICE_BASE_URL + "/" + LOG_ANALYSIS + ANALYSIS_GET_24X7_ALL_LOGS_URL + "?cvConfigId="
            + cvConfigId + "&appId=" + appId + "&clusterLevel=L1&startMinute=100&endMinute=" + clusterMinute,
        learningEngineAnalysisTask.getControl_input_url());
    assertNull(learningEngineAnalysisTask.getTest_input_url());
    assertEquals(VERIFICATION_SERVICE_BASE_URL + "/" + LOG_ANALYSIS + ANALYSIS_STATE_SAVE_24X7_CLUSTERED_LOG_URL
            + "?cvConfigId=" + cvConfigId + "&appId=" + appId + "&clusterLevel=L2&logCollectionMinute=" + clusterMinute,
        learningEngineAnalysisTask.getAnalysis_save_url());
    assertNull(learningEngineAnalysisTask.getControl_nodes());
    assertNull(learningEngineAnalysisTask.getTest_nodes());
  }

  private void createFailedLETask(
      String stateExecutionId, String workflowId, String workflowExecutionId, int analysisMin) {
    LearningEngineAnalysisTask task = LearningEngineAnalysisTask.builder()
                                          .state_execution_id(stateExecutionId)
                                          .workflow_id(workflowId)
                                          .workflow_execution_id(workflowExecutionId)
                                          .analysis_minute(analysisMin)
                                          .executionStatus(ExecutionStatus.RUNNING)
                                          .cluster_level(ClusterLevel.L2.getLevel())
                                          .ml_analysis_type(MLAnalysisType.LOG_CLUSTER)
                                          .retry(4)
                                          .build();

    task.setAppId(appId);
    wingsPersistence.save(task);
  }
}
