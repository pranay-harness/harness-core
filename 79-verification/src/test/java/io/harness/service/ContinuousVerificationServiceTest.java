package io.harness.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;

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
import java.util.List;
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
}
