package io.harness.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
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
import static software.wings.service.intfc.analysis.LogAnalysisResource.ANALYSIS_GET_24X7_ALL_LOGS_URL;
import static software.wings.service.intfc.analysis.LogAnalysisResource.ANALYSIS_GET_24X7_LOG_URL;
import static software.wings.service.intfc.analysis.LogAnalysisResource.ANALYSIS_STATE_SAVE_24X7_CLUSTERED_LOG_URL;
import static software.wings.service.intfc.analysis.LogAnalysisResource.LOG_ANALYSIS;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.VerificationBaseTest;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.service.intfc.ContinuousVerificationService;
import io.harness.waiter.WaitNotifyEngine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.beans.DelegateTask;
import software.wings.beans.SumoConfig;
import software.wings.beans.TaskType;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.ContinuousVerificationServiceImpl;
import software.wings.service.impl.analysis.LogDataRecord;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ContinuousVerificationServiceTest extends VerificationBaseTest {
  private String accountId, appId, envId, serviceId, connectorId, query, cvConfigId;

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
    setInternalState(continuousVerificationService, "verificationManagerClient", verificationManagerClient);
  }

  @Test
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
  public void testLogsCollectionBaselineInFuture() {
    LogsCVConfiguration logsCVConfiguration =
        (LogsCVConfiguration) wingsPersistence.get(CVConfiguration.class, cvConfigId);
    logsCVConfiguration.setBaselineStartMinute(TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()) + 1);
    logsCVConfiguration.setBaselineEndMinute(logsCVConfiguration.getBaselineStartMinute() + 15);
    wingsPersistence.save(logsCVConfiguration);
    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Lists.newArrayList(logsCVConfiguration));
    continuousVerificationService.triggerLogDataCollection(accountId);
    List<DelegateTask> delegateTasks =
        wingsPersistence.createQuery(DelegateTask.class).filter("accountId", accountId).asList();
    assertEquals(0, delegateTasks.size());

    logsCVConfiguration.setBaselineStartMinute(TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()) - 10);
    continuousVerificationService.triggerLogDataCollection(accountId);
    delegateTasks = wingsPersistence.createQuery(DelegateTask.class).filter("accountId", accountId).asList();
    assertEquals(0, delegateTasks.size());

    logsCVConfiguration.setBaselineStartMinute(TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()) - 20);

    continuousVerificationService.triggerLogDataCollection(accountId);
    delegateTasks = wingsPersistence.createQuery(DelegateTask.class).filter("accountId", accountId).asList();
    assertEquals(1, delegateTasks.size());

    DelegateTask delegateTask = delegateTasks.get(0);
    assertEquals(accountId, delegateTask.getAccountId());
    assertEquals(appId, delegateTask.getAppId());
    assertEquals(TaskType.SUMO_COLLECT_24_7_LOG_DATA, TaskType.valueOf(delegateTask.getTaskType()));
    SumoDataCollectionInfo sumoDataCollectionInfo = (SumoDataCollectionInfo) delegateTask.getParameters()[0];
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
  public void testLogsCollection() {
    continuousVerificationService.triggerLogDataCollection(accountId);
    List<DelegateTask> delegateTasks =
        wingsPersistence.createQuery(DelegateTask.class).filter("accountId", accountId).asList();
    assertEquals(1, delegateTasks.size());
    DelegateTask delegateTask = delegateTasks.get(0);
    assertEquals(accountId, delegateTask.getAccountId());
    assertEquals(appId, delegateTask.getAppId());
    assertEquals(TaskType.SUMO_COLLECT_24_7_LOG_DATA, TaskType.valueOf(delegateTask.getTaskType()));
    SumoDataCollectionInfo sumoDataCollectionInfo = (SumoDataCollectionInfo) delegateTask.getParameters()[0];
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
    assertEquals(TaskType.SUMO_COLLECT_24_7_LOG_DATA, TaskType.valueOf(delegateTask.getTaskType()));
    sumoDataCollectionInfo = (SumoDataCollectionInfo) delegateTask.getParameters()[0];
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
}
