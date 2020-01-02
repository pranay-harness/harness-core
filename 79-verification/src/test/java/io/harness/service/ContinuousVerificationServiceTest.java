package io.harness.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.NANDAN;
import static io.harness.rule.OwnerRule.PRANJAL;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.SOWMYA;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;
import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;
import static software.wings.common.VerificationConstants.DUMMY_HOST_NAME;
import static software.wings.common.VerificationConstants.TIME_DELAY_QUERY_MINS;
import static software.wings.common.VerificationConstants.VERIFICATION_SERVICE_BASE_URL;
import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.PREDECTIVE_HISTORY_MINUTES;
import static software.wings.service.impl.analysis.MLAnalysisType.FEEDBACK_ANALYSIS;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.service.intfc.analysis.LogAnalysisResource.ANALYSIS_GET_24X7_ALL_LOGS_URL;
import static software.wings.service.intfc.analysis.LogAnalysisResource.ANALYSIS_GET_24X7_LOG_URL;
import static software.wings.service.intfc.analysis.LogAnalysisResource.ANALYSIS_STATE_SAVE_24X7_CLUSTERED_LOG_URL;
import static software.wings.service.intfc.analysis.LogAnalysisResource.LOG_ANALYSIS;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Injector;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import io.harness.VerificationBaseTest;
import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.entities.CVTask;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.rest.RestResponse;
import io.harness.rule.OwnerRule.Owner;
import io.harness.service.intfc.ContinuousVerificationService;
import io.harness.service.intfc.LogAnalysisService;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import io.harness.time.Timestamp;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mongodb.morphia.query.Query;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.alerts.AlertCategory;
import software.wings.alerts.AlertSeverity;
import software.wings.alerts.AlertStatus;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.DatadogConfig;
import software.wings.beans.FeatureName;
import software.wings.beans.SumoConfig;
import software.wings.beans.TaskType;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.Alert.AlertKeys;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.cv.ContinuousVerificationAlertData;
import software.wings.delegatetasks.DataCollectionExecutorService;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.TimeSeriesDataRecord;
import software.wings.service.impl.AlertServiceImpl;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.CVFeedbackRecord;
import software.wings.service.impl.analysis.ContinuousVerificationServiceImpl;
import software.wings.service.impl.analysis.CustomLogDataCollectionInfo;
import software.wings.service.impl.analysis.DataCollectionInfo;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.analysis.FeedbackAction;
import software.wings.service.impl.analysis.FeedbackPriority;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogDataRecord.LogDataRecordKeys;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisStatus;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.apm.APMDataCollectionInfo;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask.LearningEngineAnalysisTaskKeys;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask.LearningEngineExperimentalAnalysisTaskKeys;
import software.wings.service.impl.newrelic.MLExperiments;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.service.impl.sumo.SumoDataCollectionInfo;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.service.intfc.verification.CVActivityLogService.Logger;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.service.intfc.verification.CVTaskService;
import software.wings.sm.StateType;
import software.wings.sm.states.DatadogLogState;
import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfiguration.CVConfigurationKeys;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.log.SplunkCVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ContinuousVerificationServiceTest extends VerificationBaseTest {
  private String accountId;
  private String appId;
  private String envId;
  private String serviceId;
  private String connectorId;
  private String datadogConnectorId;
  private String query;
  private String cvConfigId;
  private String datadogCvConfigId;
  private String workflowId;
  private String workflowExecutionId;
  private String stateExecutionId;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ContinuousVerificationService continuousVerificationService;
  @Inject private Injector injector;
  @Inject private TimeSeriesAnalysisService timeSeriesAnalysisService;
  @Inject private DataCollectionExecutorService dataCollectionService;
  @Inject private DataStoreService dataStoreService;

  @Mock private CVConfigurationService cvConfigurationService;
  @Mock private CVTaskService cvTaskService;
  @Mock private HarnessMetricRegistry metricRegistry;
  @Mock private VerificationManagerClient verificationManagerClient;
  @Mock private DelegateService delegateService;
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @Mock private SettingsService settingsService;
  @Mock private SecretManager secretManager;
  @Mock private AppService appService;
  @Mock private CVActivityLogService cvActivityLogService;
  private SumoConfig sumoConfig;
  private DatadogConfig datadogConfig;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    accountId = generateUuid();
    appId = generateUuid();
    envId = generateUuid();
    serviceId = generateUuid();
    connectorId = generateUuid();
    datadogConnectorId = generateUuid();
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
    datadogConfig = DatadogConfig.builder()
                        .url(generateUuid())
                        .accountId(accountId)
                        .apiKey(generateUuid().toCharArray())
                        .applicationKey(generateUuid().toCharArray())
                        .build();
    long currentTime = System.currentTimeMillis();

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
        TimeUnit.MILLISECONDS.toMinutes(currentTime) - TimeUnit.HOURS.toMinutes(1));
    logsCVConfiguration.setBaselineEndMinute(TimeUnit.MILLISECONDS.toMinutes(currentTime));

    cvConfigId = wingsPersistence.save(logsCVConfiguration);

    LogsCVConfiguration datadogCVConfiguration = new LogsCVConfiguration();
    datadogCVConfiguration.setName(generateUuid());
    datadogCVConfiguration.setAccountId(accountId);
    datadogCVConfiguration.setAppId(appId);
    datadogCVConfiguration.setEnvId(envId);
    datadogCVConfiguration.setServiceId(serviceId);
    datadogCVConfiguration.setEnabled24x7(true);
    datadogCVConfiguration.setConnectorId(datadogConnectorId);
    datadogCVConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
    datadogCVConfiguration.setStateType(StateType.DATA_DOG_LOG);
    datadogCVConfiguration.setBaselineStartMinute(
        TimeUnit.MILLISECONDS.toMinutes(currentTime) - TimeUnit.HOURS.toMinutes(1));
    datadogCVConfiguration.setBaselineEndMinute(TimeUnit.MILLISECONDS.toMinutes(currentTime));

    datadogCvConfigId = wingsPersistence.save(datadogCVConfiguration);

    when(cvConfigurationService.listConfigurations(accountId))
        .thenReturn(Lists.newArrayList(logsCVConfiguration, datadogCVConfiguration));
    writeField(continuousVerificationService, "cvConfigurationService", cvConfigurationService, true);
    writeField(continuousVerificationService, "metricRegistry", metricRegistry, true);
    writeField(continuousVerificationService, "cvTaskService", cvTaskService, true);
    writeField(continuousVerificationService, "cvActivityLogService", cvActivityLogService, true);
    writeField(timeSeriesAnalysisService, "managerClient", verificationManagerClient, true);
    writeField(continuousVerificationService, "timeSeriesAnalysisService", timeSeriesAnalysisService, true);
    when(delegateService.queueTask(anyObject()))
        .then(invocation -> wingsPersistence.save((DelegateTask) invocation.getArguments()[0]));
    when(settingsService.get(connectorId)).thenReturn(aSettingAttribute().withValue(sumoConfig).build());
    when(settingsService.get(datadogConnectorId)).thenReturn(aSettingAttribute().withValue(datadogConfig).build());
    when(secretManager.getEncryptionDetails(anyObject(), anyString(), anyString())).thenReturn(Collections.emptyList());
    MainConfiguration mainConfiguration = new MainConfiguration();
    mainConfiguration.setPortal(new PortalConfig());
    software.wings.service.impl.analysis.ContinuousVerificationService managerVerificationService =
        new ContinuousVerificationServiceImpl();
    when(appService.getAccountIdByAppId(anyString())).thenReturn(accountId);
    writeField(managerVerificationService, "delegateService", delegateService, true);
    writeField(managerVerificationService, "waitNotifyEngine", waitNotifyEngine, true);
    writeField(managerVerificationService, "wingsPersistence", wingsPersistence, true);
    writeField(managerVerificationService, "settingsService", settingsService, true);
    writeField(managerVerificationService, "secretManager", secretManager, true);
    writeField(managerVerificationService, "mainConfiguration", mainConfiguration, true);
    writeField(managerVerificationService, "appService", appService, true);
    writeField(managerVerificationService, "cvConfigurationService", cvConfigurationService, true);
    writeField(managerVerificationService, "cvActivityLogService", cvActivityLogService, true);
    writeField(managerVerificationService, "dataCollectionService", dataCollectionService, true);
    writeField(managerVerificationService, "dataStoreService", dataStoreService, true);

    AlertService alertService = new AlertServiceImpl();
    writeField(alertService, "wingsPersistence", wingsPersistence, true);
    writeField(alertService, "executorService", Executors.newSingleThreadScheduledExecutor(), true);
    writeField(alertService, "injector", injector, true);
    writeField(managerVerificationService, "alertService", alertService, true);
    when(cvActivityLogService.getLoggerByStateExecutionId(anyString())).thenReturn(mock(Logger.class));
    when(cvActivityLogService.getLoggerByCVConfigId(anyString(), anyLong())).thenReturn(mock(Logger.class));
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

    writeField(continuousVerificationService, "verificationManagerClient", verificationManagerClient, true);

    when(verificationManagerClient.triggerCVAlert(anyString(), any(ContinuousVerificationAlertData.class)))
        .then(invocation -> {
          Object[] args = invocation.getArguments();
          managerVerificationService.openAlert((String) args[0], (ContinuousVerificationAlertData) args[1]);
          Call<RestResponse<Boolean>> restCall = mock(Call.class);
          when(restCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
          return restCall;
        });

    when(verificationManagerClient.closeCVAlert(anyString(), any(ContinuousVerificationAlertData.class)))
        .then(invocation -> {
          Object[] args = invocation.getArguments();
          managerVerificationService.closeAlert((String) args[0], (ContinuousVerificationAlertData) args[1]);
          Call<RestResponse<Boolean>> restCall = mock(Call.class);
          when(restCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
          return restCall;
        });
  }

  @Test
  @Owner(developers = PRAVEEN)
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
    assertThat(logsCVConfiguration.getBaselineStartMinute() < 0).isTrue();
    assertThat(logsCVConfiguration.getBaselineEndMinute() < 0).isTrue();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testDefaultBaselineDatadogLog() {
    LogsCVConfiguration logsCVConfiguration = new LogsCVConfiguration();
    logsCVConfiguration.setName(generateUuid());
    logsCVConfiguration.setAccountId(accountId);
    logsCVConfiguration.setAppId(appId);
    logsCVConfiguration.setEnvId(envId);
    logsCVConfiguration.setServiceId(serviceId);
    logsCVConfiguration.setEnabled24x7(true);
    logsCVConfiguration.setConnectorId(datadogConnectorId);
    logsCVConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
    logsCVConfiguration.setStateType(StateType.DATA_DOG_LOG);

    datadogCvConfigId = wingsPersistence.save(logsCVConfiguration);

    logsCVConfiguration = (LogsCVConfiguration) wingsPersistence.get(CVConfiguration.class, datadogCvConfigId);
    assertThat(logsCVConfiguration.getBaselineStartMinute() < 0).isTrue();
    assertThat(logsCVConfiguration.getBaselineEndMinute() < 0).isTrue();
  }

  private DelegateTask updateBaseline(String configId, long currentMinute) {
    LogsCVConfiguration logsCVConfiguration =
        (LogsCVConfiguration) wingsPersistence.get(CVConfiguration.class, configId);
    logsCVConfiguration.setBaselineStartMinute(currentMinute + CRON_POLL_INTERVAL_IN_MINUTES);
    logsCVConfiguration.setBaselineEndMinute(
        logsCVConfiguration.getBaselineStartMinute() + CRON_POLL_INTERVAL_IN_MINUTES * 3);
    wingsPersistence.save(logsCVConfiguration);

    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Lists.newArrayList(logsCVConfiguration));
    continuousVerificationService.triggerLogDataCollection(accountId);
    List<DelegateTask> delegateTasks =
        wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.accountId, accountId).asList();
    assertThat(delegateTasks).isEmpty();

    logsCVConfiguration.setBaselineStartMinute(currentMinute - 2);
    continuousVerificationService.triggerLogDataCollection(accountId);
    delegateTasks =
        wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.accountId, accountId).asList();
    assertThat(delegateTasks).isEmpty();

    logsCVConfiguration.setBaselineStartMinute(currentMinute - 20);

    continuousVerificationService.triggerLogDataCollection(accountId);
    delegateTasks =
        wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.accountId, accountId).asList();
    assertThat(delegateTasks).hasSize(1);

    wingsPersistence.save(logsCVConfiguration);

    return delegateTasks.get(0);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testLogsCollectionBaselineInFuture() throws IOException {
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    logger.info("currentMin: {}", currentMinute);

    DelegateTask delegateTask = updateBaseline(cvConfigId, currentMinute);
    LogsCVConfiguration logsCVConfiguration =
        (LogsCVConfiguration) wingsPersistence.get(CVConfiguration.class, cvConfigId);

    assertThat(delegateTask.getAppId()).isEqualTo(appId);
    assertThat(delegateTask.getAccountId()).isEqualTo(accountId);
    assertThat(TaskType.valueOf(delegateTask.getData().getTaskType())).isEqualTo(TaskType.SUMO_COLLECT_24_7_LOG_DATA);

    SumoDataCollectionInfo sumoDataCollectionInfo = (SumoDataCollectionInfo) delegateTask.getData().getParameters()[0];

    assertThat(sumoDataCollectionInfo.getStartTime())
        .isEqualTo(TimeUnit.MINUTES.toMillis(logsCVConfiguration.getBaselineStartMinute()));
    assertThat(sumoDataCollectionInfo.getEndTime())
        .isEqualTo(TimeUnit.MINUTES.toMillis(
            logsCVConfiguration.getBaselineStartMinute() + CRON_POLL_INTERVAL_IN_MINUTES - 1));

    assertThat(sumoDataCollectionInfo.getSumoConfig()).isEqualTo(sumoConfig);
    assertThat(sumoDataCollectionInfo.getCvConfigId()).isEqualTo(cvConfigId);
    assertThat(sumoDataCollectionInfo.getAccountId()).isEqualTo(accountId);
    assertThat(sumoDataCollectionInfo.getServiceId()).isEqualTo(serviceId);
    assertThat(sumoDataCollectionInfo.getApplicationId()).isEqualTo(appId);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testLogsCollectionBaselineInFutureDatadogLog() throws IOException {
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    logger.info("currentMin: {}", currentMinute);

    DelegateTask delegateTask = updateBaseline(datadogCvConfigId, currentMinute);
    LogsCVConfiguration logsCVConfiguration =
        (LogsCVConfiguration) wingsPersistence.get(CVConfiguration.class, datadogCvConfigId);

    CustomLogDataCollectionInfo customLogDataCollectionInfo =
        (CustomLogDataCollectionInfo) delegateTask.getData().getParameters()[0];

    assertThat(customLogDataCollectionInfo.getStartTime())
        .isEqualTo(TimeUnit.MINUTES.toMillis(logsCVConfiguration.getBaselineStartMinute()));
    assertThat(customLogDataCollectionInfo.getEndTime())
        .isEqualTo(TimeUnit.MINUTES.toMillis(
            logsCVConfiguration.getBaselineStartMinute() + CRON_POLL_INTERVAL_IN_MINUTES - 1));

    assertThat(delegateTask.getAccountId()).isEqualTo(accountId);
    assertThat(delegateTask.getAppId()).isEqualTo(appId);
    assertThat(TaskType.valueOf(delegateTask.getData().getTaskType())).isEqualTo(TaskType.CUSTOM_COLLECT_24_7_LOG_DATA);
    assertThat(customLogDataCollectionInfo.getCvConfigId()).isEqualTo(datadogCvConfigId);
    assertThat(customLogDataCollectionInfo.getApplicationId()).isEqualTo(appId);
    assertThat(customLogDataCollectionInfo.getServiceId()).isEqualTo(serviceId);
    assertThat(customLogDataCollectionInfo.getAccountId()).isEqualTo(accountId);
  }

  @Test
  @Owner(developers = RAGHU)
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
        wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.accountId, accountId).asList();
    assertThat(delegateTasks).isEmpty();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testLogsCollectionNoBaselineSetDatadogLog() {
    LogsCVConfiguration logsCVConfiguration =
        (LogsCVConfiguration) wingsPersistence.get(CVConfiguration.class, datadogCvConfigId);
    logsCVConfiguration.setBaselineStartMinute(-1);
    logsCVConfiguration.setBaselineEndMinute(-1);
    wingsPersistence.save(logsCVConfiguration);
    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Lists.newArrayList(logsCVConfiguration));
    continuousVerificationService.triggerLogDataCollection(accountId);
    List<DelegateTask> delegateTasks =
        wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.accountId, accountId).asList();
    assertThat(delegateTasks).isEmpty();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testLogsCollection() throws IOException {
    continuousVerificationService.triggerLogDataCollection(accountId);
    List<DelegateTask> delegateTasks =
        wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.accountId, accountId).asList();
    assertThat(delegateTasks).hasSize(2);

    DelegateTask delegateTask = delegateTasks.get(0);
    SumoDataCollectionInfo sumoDataCollectionInfo = (SumoDataCollectionInfo) delegateTask.getData().getParameters()[0];

    assertThat(delegateTask.getAccountId()).isEqualTo(accountId);
    assertThat(delegateTask.getAppId()).isEqualTo(appId);
    assertThat(TaskType.valueOf(delegateTask.getData().getTaskType())).isEqualTo(TaskType.SUMO_COLLECT_24_7_LOG_DATA);
    assertThat(sumoDataCollectionInfo.getCvConfigId()).isEqualTo(cvConfigId);
    assertThat(sumoDataCollectionInfo.getApplicationId()).isEqualTo(appId);
    assertThat(sumoDataCollectionInfo.getAccountId()).isEqualTo(accountId);
    assertThat(sumoDataCollectionInfo.getServiceId()).isEqualTo(serviceId);
    assertThat(sumoDataCollectionInfo.getSumoConfig()).isEqualTo(sumoConfig);

    LogsCVConfiguration logsCVConfiguration =
        (LogsCVConfiguration) wingsPersistence.get(CVConfiguration.class, cvConfigId);
    assertThat(sumoDataCollectionInfo.getStartTime())
        .isEqualTo(TimeUnit.MINUTES.toMillis(logsCVConfiguration.getBaselineStartMinute()));
    assertThat(sumoDataCollectionInfo.getEndTime())
        .isEqualTo(TimeUnit.MINUTES.toMillis(
            logsCVConfiguration.getBaselineStartMinute() + CRON_POLL_INTERVAL_IN_MINUTES - 1));

    // save some log and trigger again
    long numOfMinutesSaved = 30;
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
    delegateTasks =
        wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.accountId, accountId).asList();
    assertThat(delegateTasks).hasSize(4);

    delegateTask = delegateTasks.get(2);
    assertThat(delegateTask.getAccountId()).isEqualTo(accountId);
    assertThat(delegateTask.getAppId()).isEqualTo(appId);
    assertThat(TaskType.valueOf(delegateTask.getData().getTaskType())).isEqualTo(TaskType.SUMO_COLLECT_24_7_LOG_DATA);
    sumoDataCollectionInfo = (SumoDataCollectionInfo) delegateTask.getData().getParameters()[0];
    assertThat(sumoDataCollectionInfo.getSumoConfig()).isEqualTo(sumoConfig);
    assertThat(sumoDataCollectionInfo.getCvConfigId()).isEqualTo(cvConfigId);
    assertThat(sumoDataCollectionInfo.getApplicationId()).isEqualTo(appId);
    assertThat(sumoDataCollectionInfo.getAccountId()).isEqualTo(accountId);
    assertThat(sumoDataCollectionInfo.getServiceId()).isEqualTo(serviceId);

    assertThat(sumoDataCollectionInfo.getStartTime())
        .isEqualTo(TimeUnit.MINUTES.toMillis(logsCVConfiguration.getBaselineStartMinute() + numOfMinutesSaved + 1));
    assertThat(sumoDataCollectionInfo.getEndTime())
        .isEqualTo(TimeUnit.MINUTES.toMillis(
            logsCVConfiguration.getBaselineStartMinute() + CRON_POLL_INTERVAL_IN_MINUTES + numOfMinutesSaved));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSplunkLogCollectionWithCVTask() throws IOException {
    Call<RestResponse<Boolean>> managerFeatureFlagCall = mock(Call.class);
    when(managerFeatureFlagCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isFeatureEnabled(FeatureName.SPLUNK_24_7_CV_TASK, accountId))
        .thenReturn(managerFeatureFlagCall);
    SplunkCVConfiguration splunkCVConfiguration = SplunkCVConfiguration.builder().build();
    splunkCVConfiguration.setEnabled24x7(true);
    splunkCVConfiguration.setUuid(cvConfigId);
    splunkCVConfiguration.setAccountId(accountId);
    splunkCVConfiguration.setStateType(StateType.SPLUNKV2);
    splunkCVConfiguration.setBaselineStartMinute(
        TimeUnit.MILLISECONDS.toMinutes(Instant.now().minus(60, ChronoUnit.MINUTES).toEpochMilli()));
    splunkCVConfiguration.setBaselineEndMinute(
        TimeUnit.MILLISECONDS.toMinutes(Instant.now().minus(30, ChronoUnit.MINUTES).toEpochMilli()));
    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Lists.newArrayList(splunkCVConfiguration));
    continuousVerificationService.triggerLogDataCollection(accountId);
    ArgumentCaptor<CVTask> capture = ArgumentCaptor.forClass(CVTask.class);
    verify(cvTaskService).saveCVTask(capture.capture());
    CVTask savedTask = capture.getValue();
    assertThat(savedTask.getStatus()).isEqualTo(ExecutionStatus.QUEUED);
    DataCollectionInfoV2 savedDataCollectionInfo = savedTask.getDataCollectionInfo();
    assertThat(savedDataCollectionInfo.getCvConfigId()).isEqualTo(cvConfigId);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testDatadogLogsCollection() {
    continuousVerificationService.triggerLogDataCollection(accountId);
    List<DelegateTask> delegateTasks =
        wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.accountId, accountId).asList();
    assertThat(delegateTasks).hasSize(2);
    DelegateTask delegateTask = delegateTasks.get(1);
    assertThat(delegateTask.getAccountId()).isEqualTo(accountId);
    assertThat(delegateTask.getAppId()).isEqualTo(appId);
    assertThat(TaskType.valueOf(delegateTask.getData().getTaskType())).isEqualTo(TaskType.CUSTOM_COLLECT_24_7_LOG_DATA);
    CustomLogDataCollectionInfo customLogDataCollectionInfo =
        (CustomLogDataCollectionInfo) delegateTask.getData().getParameters()[0];
    assertThat(customLogDataCollectionInfo.getCvConfigId()).isEqualTo(datadogCvConfigId);
    assertThat(customLogDataCollectionInfo.getApplicationId()).isEqualTo(appId);
    assertThat(customLogDataCollectionInfo.getAccountId()).isEqualTo(accountId);
    assertThat(customLogDataCollectionInfo.getServiceId()).isEqualTo(serviceId);

    LogsCVConfiguration logsCVConfiguration =
        (LogsCVConfiguration) wingsPersistence.get(CVConfiguration.class, datadogCvConfigId);
    assertThat(customLogDataCollectionInfo.getStartTime())
        .isEqualTo(TimeUnit.MINUTES.toMillis(logsCVConfiguration.getBaselineStartMinute()));
    assertThat(customLogDataCollectionInfo.getEndTime())
        .isEqualTo(TimeUnit.MINUTES.toMillis(
            logsCVConfiguration.getBaselineStartMinute() + CRON_POLL_INTERVAL_IN_MINUTES - 1));

    // save some log and trigger again
    long numOfMinutesSaved = 30;
    for (long i = logsCVConfiguration.getBaselineStartMinute();
         i <= logsCVConfiguration.getBaselineStartMinute() + numOfMinutesSaved; i++) {
      LogDataRecord logDataRecord = new LogDataRecord();
      logDataRecord.setAppId(appId);
      logDataRecord.setCvConfigId(datadogCvConfigId);
      logDataRecord.setLogCollectionMinute((int) i);
      logDataRecord.setClusterLevel(ClusterLevel.H0);
      wingsPersistence.save(logDataRecord);
    }
    continuousVerificationService.triggerLogDataCollection(accountId);
    delegateTasks =
        wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.accountId, accountId).asList();
    assertThat(delegateTasks).hasSize(4);

    delegateTask = delegateTasks.get(3);
    assertThat(delegateTask.getAccountId()).isEqualTo(accountId);
    assertThat(delegateTask.getAppId()).isEqualTo(appId);
    assertThat(TaskType.valueOf(delegateTask.getData().getTaskType())).isEqualTo(TaskType.CUSTOM_COLLECT_24_7_LOG_DATA);
    customLogDataCollectionInfo = (CustomLogDataCollectionInfo) delegateTask.getData().getParameters()[0];
    assertThat(customLogDataCollectionInfo.getCvConfigId()).isEqualTo(datadogCvConfigId);
    assertThat(customLogDataCollectionInfo.getApplicationId()).isEqualTo(appId);
    assertThat(customLogDataCollectionInfo.getAccountId()).isEqualTo(accountId);
    assertThat(customLogDataCollectionInfo.getServiceId()).isEqualTo(serviceId);

    assertThat(customLogDataCollectionInfo.getStartTime())
        .isEqualTo(TimeUnit.MINUTES.toMillis(logsCVConfiguration.getBaselineStartMinute() + numOfMinutesSaved + 1));
    assertThat(customLogDataCollectionInfo.getEndTime())
        .isEqualTo(TimeUnit.MINUTES.toMillis(
            logsCVConfiguration.getBaselineStartMinute() + CRON_POLL_INTERVAL_IN_MINUTES + numOfMinutesSaved));
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testDatadogLogsCollectionEndTimeGreaterThanCurrentTime() throws IOException {
    continuousVerificationService.triggerLogDataCollection(accountId);
    List<DelegateTask> delegateTasks =
        wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.accountId, accountId).asList();
    assertThat(delegateTasks).hasSize(2);
    DelegateTask delegateTask = delegateTasks.get(1);
    assertThat(delegateTask.getAccountId()).isEqualTo(accountId);
    assertThat(delegateTask.getAppId()).isEqualTo(appId);
    assertThat(TaskType.valueOf(delegateTask.getData().getTaskType())).isEqualTo(TaskType.CUSTOM_COLLECT_24_7_LOG_DATA);
    CustomLogDataCollectionInfo customLogDataCollectionInfo =
        (CustomLogDataCollectionInfo) delegateTask.getData().getParameters()[0];
    assertThat(customLogDataCollectionInfo.getCvConfigId()).isEqualTo(datadogCvConfigId);
    assertThat(customLogDataCollectionInfo.getApplicationId()).isEqualTo(appId);
    assertThat(customLogDataCollectionInfo.getAccountId()).isEqualTo(accountId);
    assertThat(customLogDataCollectionInfo.getServiceId()).isEqualTo(serviceId);

    LogsCVConfiguration logsCVConfiguration =
        (LogsCVConfiguration) wingsPersistence.get(CVConfiguration.class, datadogCvConfigId);
    assertThat(customLogDataCollectionInfo.getStartTime())
        .isEqualTo(TimeUnit.MINUTES.toMillis(logsCVConfiguration.getBaselineStartMinute()));
    assertThat(customLogDataCollectionInfo.getEndTime())
        .isEqualTo(TimeUnit.MINUTES.toMillis(
            logsCVConfiguration.getBaselineStartMinute() + CRON_POLL_INTERVAL_IN_MINUTES - 1));

    // save some log and trigger again
    long numOfMinutesSaved = 70;
    for (long i = logsCVConfiguration.getBaselineStartMinute();
         i <= logsCVConfiguration.getBaselineStartMinute() + numOfMinutesSaved; i++) {
      LogDataRecord logDataRecord = new LogDataRecord();
      logDataRecord.setAppId(appId);
      logDataRecord.setCvConfigId(datadogCvConfigId);
      logDataRecord.setLogCollectionMinute((int) i);
      logDataRecord.setClusterLevel(ClusterLevel.H0);
      wingsPersistence.save(logDataRecord);
    }
    continuousVerificationService.triggerLogDataCollection(accountId);
    delegateTasks =
        wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.accountId, accountId).asList();
    assertThat(delegateTasks).hasSize(3);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testTriggerLogsCollection() throws IOException {
    Call<RestResponse<Boolean>> managerCall = mock(Call.class);
    when(managerCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isStateValid(anyString(), anyString())).thenReturn(managerCall);

    AnalysisContext context =
        createSUMOAnalysisContext(TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary()));
    wingsPersistence.save(context);
    continuousVerificationService.triggerWorkflowDataCollection(context);
    List<DelegateTask> delegateTasks =
        wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.accountId, accountId).asList();
    assertThat(delegateTasks).hasSize(1);
    DelegateTask delegateTask = delegateTasks.get(0);

    assertThat(delegateTask.getAccountId()).isEqualTo(accountId);
    assertThat(delegateTask.getAppId()).isEqualTo(appId);
    assertThat(TaskType.valueOf(delegateTask.getData().getTaskType())).isEqualTo(TaskType.SUMO_COLLECT_LOG_DATA);
    SumoDataCollectionInfo sumoDataCollectionInfo = (SumoDataCollectionInfo) delegateTask.getData().getParameters()[0];
    assertThat(sumoDataCollectionInfo.getSumoConfig()).isEqualTo(sumoConfig);
    assertThat(sumoDataCollectionInfo.getApplicationId()).isEqualTo(appId);
    assertThat(sumoDataCollectionInfo.getAccountId()).isEqualTo(accountId);
    assertThat(sumoDataCollectionInfo.getServiceId()).isEqualTo(serviceId);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testTriggerDatadogLogsCollection() throws IOException {
    Call<RestResponse<Boolean>> managerCall = mock(Call.class);
    when(managerCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isStateValid(anyString(), anyString())).thenReturn(managerCall);
    AnalysisContext context =
        createDatadogLogAnalysisContext((int) TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary()));
    wingsPersistence.save(context);
    continuousVerificationService.triggerWorkflowDataCollection(context);
    List<DelegateTask> delegateTasks =
        wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.accountId, accountId).asList();
    assertThat(delegateTasks).hasSize(1);
    DelegateTask delegateTask = delegateTasks.get(0);

    assertThat(delegateTask.getAccountId()).isEqualTo(accountId);
    assertThat(delegateTask.getAppId()).isEqualTo(appId);
    assertThat(TaskType.valueOf(delegateTask.getData().getTaskType())).isEqualTo(TaskType.CUSTOM_LOG_COLLECTION_TASK);
    CustomLogDataCollectionInfo customLogDataCollectionInfo =
        (CustomLogDataCollectionInfo) delegateTask.getData().getParameters()[0];
    assertThat(customLogDataCollectionInfo.getApplicationId()).isEqualTo(appId);
    assertThat(customLogDataCollectionInfo.getAccountId()).isEqualTo(accountId);
    assertThat(customLogDataCollectionInfo.getServiceId()).isEqualTo(serviceId);
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(UnitTests.class)
  public void testTriggerLogsCollectionInvalidState() throws IOException {
    Call<RestResponse<Boolean>> managerCall = mock(Call.class);
    when(managerCall.execute()).thenReturn(Response.success(new RestResponse<>(false)));
    when(verificationManagerClient.isStateValid(anyString(), anyString())).thenReturn(managerCall);
    AnalysisContext context = createAnalysisContext(
        null, TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary()), StateType.SUMO, connectorId);
    wingsPersistence.save(context);
    boolean isTriggered = continuousVerificationService.triggerWorkflowDataCollection(context);
    assertThat(isTriggered).isFalse();
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(UnitTests.class)
  public void testTriggerDatadogLogsCollectionInvalidState() throws IOException {
    Call<RestResponse<Boolean>> managerCall = mock(Call.class);
    when(managerCall.execute()).thenReturn(Response.success(new RestResponse<>(false)));
    when(verificationManagerClient.isStateValid(anyString(), anyString())).thenReturn(managerCall);
    AnalysisContext context = createAnalysisContext(null,
        TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary()), StateType.DATA_DOG_LOG, datadogConnectorId);
    wingsPersistence.save(context);
    boolean isTriggered = continuousVerificationService.triggerWorkflowDataCollection(context);
    assertThat(isTriggered).isFalse();
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(UnitTests.class)
  public void testTriggerLogsCollectionCompletedCollection() throws IOException {
    Call<RestResponse<Boolean>> managerCall = mock(Call.class);
    when(managerCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isStateValid(anyString(), anyString())).thenReturn(managerCall);
    long startTimeInterval = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    AnalysisContext context = createAnalysisContext(null, startTimeInterval, StateType.SUMO, connectorId);
    wingsPersistence.save(context);

    LogDataRecord record = createLogDataRecord(startTimeInterval, StateType.SUMO);
    wingsPersistence.save(record);

    boolean isTriggered = continuousVerificationService.triggerWorkflowDataCollection(context);
    assertThat(isTriggered).isFalse();
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(UnitTests.class)
  public void testTriggerDatadogLogsCollectionCompletedCollection() throws IOException {
    Call<RestResponse<Boolean>> managerCall = mock(Call.class);
    when(managerCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isStateValid(anyString(), anyString())).thenReturn(managerCall);
    long startTimeInterval = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    AnalysisContext context =
        createAnalysisContext(null, startTimeInterval, StateType.DATA_DOG_LOG, datadogConnectorId);
    wingsPersistence.save(context);

    LogDataRecord record = createLogDataRecord(startTimeInterval, StateType.DATA_DOG_LOG);
    wingsPersistence.save(record);

    boolean isTriggered = continuousVerificationService.triggerWorkflowDataCollection(context);
    assertThat(isTriggered).isFalse();
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(UnitTests.class)
  public void testTriggerLogsCollectionNextMinuteDataCollection() throws IOException {
    Call<RestResponse<Boolean>> managerCall = mock(Call.class);
    when(managerCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isStateValid(anyString(), anyString())).thenReturn(managerCall);
    long startTimeInterval = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    AnalysisContext context = createAnalysisContext(null, startTimeInterval, StateType.SUMO, connectorId);
    context.setTimeDuration(2);
    wingsPersistence.save(context);

    LogDataRecord record = createLogDataRecord(startTimeInterval, StateType.SUMO);
    wingsPersistence.save(record);

    boolean isTriggered = continuousVerificationService.triggerWorkflowDataCollection(context);
    assertThat(isTriggered).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(UnitTests.class)
  public void testTriggerDatadogLogsCollectionNextMinuteDataCollection() throws IOException {
    Call<RestResponse<Boolean>> managerCall = mock(Call.class);
    when(managerCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isStateValid(anyString(), anyString())).thenReturn(managerCall);
    long startTimeInterval = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    AnalysisContext context =
        createAnalysisContext(null, startTimeInterval, StateType.DATA_DOG_LOG, datadogConnectorId);
    context.setTimeDuration(2);
    wingsPersistence.save(context);

    LogDataRecord record = createLogDataRecord(startTimeInterval, StateType.DATA_DOG_LOG);
    wingsPersistence.save(record);

    boolean isTriggered = continuousVerificationService.triggerWorkflowDataCollection(context);
    assertThat(isTriggered).isTrue();
  }

  private LogDataRecord createLogDataRecord(long startTimeInterval, StateType stateType) {
    LogDataRecord record = new LogDataRecord();
    record.setStateType(stateType);
    record.setWorkflowId(workflowId);
    record.setLogCollectionMinute(startTimeInterval);
    record.setQuery(query);
    record.setAppId(appId);
    record.setStateExecutionId(stateExecutionId);
    return record;
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testLogsL1ClusteringNothingNewPast2hours() {
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    continuousVerificationService.triggerLogsL1Clustering(accountId);
    List<LearningEngineAnalysisTask> learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertThat(0).isEqualTo(learningEngineAnalysisTasks.size());

    int numOfMinutes = 10;
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
        logDataRecord.setClusterLevel(ClusterLevel.H0);
        logDataRecord.setLogCollectionMinute(currentMinute - 200 + i);
        wingsPersistence.save(logDataRecord);

        if (i % 2 == 0) {
          logDataRecord.setUuid(null);
          logDataRecord.setClusterLevel(ClusterLevel.L0);
          wingsPersistence.save(logDataRecord);
        }
      }
    }

    continuousVerificationService.triggerLogsL1Clustering(accountId);
    learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertThat(0).isEqualTo(learningEngineAnalysisTasks.size());
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testLogsL1Clustering() {
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    continuousVerificationService.triggerLogsL1Clustering(accountId);
    List<LearningEngineAnalysisTask> learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertThat(learningEngineAnalysisTasks).isEmpty();

    int numOfMinutes = 10;
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
        logDataRecord.setClusterLevel(ClusterLevel.H0);
        logDataRecord.setLogCollectionMinute(currentMinute - 100 + i);
        wingsPersistence.save(logDataRecord);

        if (i % 2 == 0) {
          logDataRecord.setUuid(null);
          logDataRecord.setClusterLevel(ClusterLevel.L0);
          wingsPersistence.save(logDataRecord);
        }
      }
    }

    continuousVerificationService.triggerLogsL1Clustering(accountId);
    learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();

    Set<String> hosts = new HashSet<>();
    for (int i = 0; i < numOfHosts; i++) {
      hosts.add("host-" + i);
    }
    assertThat(numOfMinutes / 2).isEqualTo(learningEngineAnalysisTasks.size());
    for (int i = 0; i < numOfMinutes / 2; i++) {
      LearningEngineAnalysisTask learningEngineAnalysisTask = learningEngineAnalysisTasks.get(i);
      assertThat(learningEngineAnalysisTask.getWorkflow_id()).isNull();
      assertThat(learningEngineAnalysisTask.getWorkflow_execution_id()).isNull();
      assertThat("LOGS_CLUSTER_L1_" + cvConfigId + "_" + (currentMinute - 100 + i * 2))
          .isEqualTo(learningEngineAnalysisTask.getState_execution_id());
      assertThat(serviceId).isEqualTo(learningEngineAnalysisTask.getService_id());
      assertThat(currentMinute - 100 + i * 2).isEqualTo(learningEngineAnalysisTask.getAnalysis_minute());
      assertThat(VERIFICATION_SERVICE_BASE_URL + "/" + LOG_ANALYSIS + ANALYSIS_GET_24X7_LOG_URL + "?cvConfigId="
          + cvConfigId + "&appId=" + appId + "&clusterLevel=L0&logCollectionMinute=" + (currentMinute - 100 + i * 2))
          .isEqualTo(learningEngineAnalysisTask.getControl_input_url());
      assertThat(learningEngineAnalysisTask.getTest_input_url()).isNull();
      assertThat(VERIFICATION_SERVICE_BASE_URL + "/" + LOG_ANALYSIS + ANALYSIS_STATE_SAVE_24X7_CLUSTERED_LOG_URL
          + "?cvConfigId=" + cvConfigId + "&appId=" + appId
          + "&clusterLevel=L1&logCollectionMinute=" + (currentMinute - 100 + i * 2))
          .isEqualTo(learningEngineAnalysisTask.getAnalysis_save_url());
      assertThat(hosts).isEqualTo(learningEngineAnalysisTask.getControl_nodes());
      assertThat(learningEngineAnalysisTask.getTest_nodes()).isNull();
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testLogsL2Clustering() {
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    continuousVerificationService.triggerLogsL2Clustering(accountId);
    List<LearningEngineAnalysisTask> learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertThat(learningEngineAnalysisTasks).isEmpty();

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
        logDataRecord.setLogCollectionMinute(currentMinute - 100 + i);
        wingsPersistence.save(logDataRecord);

        logDataRecord.setUuid(null);
        logDataRecord.setClusterLevel(ClusterLevel.L0);
        wingsPersistence.save(logDataRecord);
      }
    }

    continuousVerificationService.triggerLogsL2Clustering(accountId);
    learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertThat(learningEngineAnalysisTasks).isEmpty();

    for (int i = numOfMinutes; i < CRON_POLL_INTERVAL_IN_MINUTES; i++) {
      for (int j = 0; j < numOfHosts; j++) {
        logDataRecord.setUuid(null);
        logDataRecord.setClusterLevel(ClusterLevel.H1);
        logDataRecord.setHost("host-" + j);
        logDataRecord.setLogCollectionMinute(currentMinute - 100 + i);
        wingsPersistence.save(logDataRecord);

        logDataRecord.setUuid(null);
        logDataRecord.setClusterLevel(ClusterLevel.L0);
        wingsPersistence.save(logDataRecord);
      }
    }

    continuousVerificationService.triggerLogsL2Clustering(accountId);
    learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertThat(learningEngineAnalysisTasks).isEmpty();

    wingsPersistence.delete(
        wingsPersistence.createQuery(LogDataRecord.class).filter(LogDataRecordKeys.clusterLevel, ClusterLevel.L0));

    continuousVerificationService.triggerLogsL2Clustering(accountId);
    learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertThat(learningEngineAnalysisTasks).hasSize(1);
    final int clusterMinute = (int) currentMinute - 100 + CRON_POLL_INTERVAL_IN_MINUTES - 1;
    LearningEngineAnalysisTask learningEngineAnalysisTask = learningEngineAnalysisTasks.get(0);
    validateL2Clustering(learningEngineAnalysisTask, clusterMinute, currentMinute);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testLogsL2ClusteringRetryBackoff() throws Exception {
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    continuousVerificationService.triggerLogsL2Clustering(accountId);
    List<LearningEngineAnalysisTask> learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertThat(learningEngineAnalysisTasks).isEmpty();

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
        logDataRecordInRetry.setLogCollectionMinute(currentMinute - 100 + i);
        wingsPersistence.save(logDataRecordInRetry);

        logDataRecordInRetry.setUuid(null);
        logDataRecordInRetry.setClusterLevel(ClusterLevel.L0);
        wingsPersistence.save(logDataRecordInRetry);
      }
    }
    final int clusterMinute = (int) currentMinute - 100 + CRON_POLL_INTERVAL_IN_MINUTES - 1;
    createFailedLETask("LOGS_CLUSTER_L2_" + cvConfigId + "_" + clusterMinute, null, null, clusterMinute, false);
    continuousVerificationService.triggerLogsL2Clustering(accountId);
    learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertThat(learningEngineAnalysisTasks).hasSize(1);

    for (int i = numOfMinutes; i < CRON_POLL_INTERVAL_IN_MINUTES; i++) {
      for (int j = 0; j < numOfHosts; j++) {
        logDataRecordInRetry.setUuid(null);
        logDataRecordInRetry.setClusterLevel(ClusterLevel.H1);
        logDataRecordInRetry.setHost("host-" + j);
        logDataRecordInRetry.setLogCollectionMinute(currentMinute - 100 + i);
        wingsPersistence.save(logDataRecordInRetry);

        logDataRecordInRetry.setUuid(null);
        logDataRecordInRetry.setClusterLevel(ClusterLevel.L0);
        wingsPersistence.save(logDataRecordInRetry);
      }
    }

    continuousVerificationService.triggerLogsL2Clustering(accountId);
    learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertThat(learningEngineAnalysisTasks).hasSize(1);

    wingsPersistence.delete(
        wingsPersistence.createQuery(LogDataRecord.class).filter(LogDataRecordKeys.clusterLevel, ClusterLevel.L0));

    createFailedLETask("LOGS_CLUSTER_L2_" + cvConfigId + "_" + clusterMinute, null, null, clusterMinute, true);
    Thread.sleep(1000); // introducing this sleep so the "nextScheduleTime" in backoff takes effect.
    continuousVerificationService.triggerLogsL2Clustering(accountId);
    learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertThat(learningEngineAnalysisTasks).hasSize(3);

    LearningEngineAnalysisTask learningEngineAnalysisTask = learningEngineAnalysisTasks.get(2);
    validateL2Clustering(learningEngineAnalysisTask, clusterMinute, currentMinute);
  }

  private void validateL2Clustering(
      LearningEngineAnalysisTask learningEngineAnalysisTask, int clusterMinute, long currentMin) {
    long startMin = currentMin - 100;
    assertThat(learningEngineAnalysisTask.getWorkflow_id()).isNull();
    assertThat(learningEngineAnalysisTask.getWorkflow_execution_id()).isNull();
    assertThat(learningEngineAnalysisTask.getState_execution_id())
        .isEqualTo("LOGS_CLUSTER_L2_" + cvConfigId + "_" + clusterMinute);
    assertThat(learningEngineAnalysisTask.getService_id()).isEqualTo(serviceId);
    assertThat(learningEngineAnalysisTask.getAnalysis_minute()).isEqualTo(clusterMinute);
    assertThat(learningEngineAnalysisTask.getControl_input_url())
        .isEqualTo(VERIFICATION_SERVICE_BASE_URL + "/" + LOG_ANALYSIS + ANALYSIS_GET_24X7_ALL_LOGS_URL
            + "?cvConfigId=" + cvConfigId + "&appId=" + appId + "&clusterLevel=L1&startMinute=" + startMin
            + "&endMinute=" + clusterMinute);
    assertThat(learningEngineAnalysisTask.getTest_input_url()).isNull();
    assertThat(learningEngineAnalysisTask.getAnalysis_save_url())
        .isEqualTo(VERIFICATION_SERVICE_BASE_URL + "/" + LOG_ANALYSIS + ANALYSIS_STATE_SAVE_24X7_CLUSTERED_LOG_URL
            + "?cvConfigId=" + cvConfigId + "&appId=" + appId
            + "&clusterLevel=L2&logCollectionMinute=" + clusterMinute);
    assertThat(learningEngineAnalysisTask.getControl_nodes()).isNull();
    assertThat(learningEngineAnalysisTask.getTest_nodes()).isNull();
  }

  private void createFailedLETask(String stateExecutionId, String workflowId, String workflowExecutionId,
      int analysisMin, boolean changeLastUpdated) {
    LearningEngineAnalysisTask task = LearningEngineAnalysisTask.builder()
                                          .state_execution_id(stateExecutionId)
                                          .workflow_id(workflowId)
                                          .workflow_execution_id(workflowExecutionId)
                                          .analysis_minute(analysisMin)
                                          .executionStatus(ExecutionStatus.RUNNING)
                                          .cluster_level(ClusterLevel.L2.getLevel())
                                          .ml_analysis_type(MLAnalysisType.LOG_CLUSTER)
                                          .service_guard_backoff_count(0)
                                          .retry(4)
                                          .build();

    task.setAppId(appId);

    if (changeLastUpdated) {
      task.setLastUpdatedAt(Timestamp.currentMinuteBoundary() - TimeUnit.MINUTES.toMillis(12));
    }
    wingsPersistence.save(task);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testTriggerTimeSeriesAlertIfNecessary() throws IOException {
    final NewRelicCVServiceConfiguration cvConfiguration = new NewRelicCVServiceConfiguration();
    cvConfiguration.setAppId(appId);
    cvConfiguration.setEnvId(envId);
    cvConfiguration.setServiceId(serviceId);
    cvConfiguration.setAlertEnabled(false);
    cvConfiguration.setAlertThreshold(0.5);
    cvConfiguration.setName(generateUuid());
    cvConfiguration.setAccountId(accountId);
    cvConfiguration.setStateType(StateType.NEW_RELIC);
    final String configId = wingsPersistence.save(cvConfiguration);

    File file = new File(getClass()
                             .getClassLoader()
                             .getResource("./verification/24x7_ts_transactionMetricRisk_MetricRecords")
                             .getFile());
    final Gson gson1 = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<List<NewRelicMetricDataRecord>>() {}.getType();
      List<NewRelicMetricDataRecord> metricDataRecords = gson1.fromJson(br, type);
      metricDataRecords.forEach(metricDataRecord -> {
        metricDataRecord.setAppId(appId);
        metricDataRecord.setCvConfigId(configId);
        metricDataRecord.setStateType(StateType.NEW_RELIC);
        metricDataRecord.setDataCollectionMinute(10);
      });

      final List<TimeSeriesDataRecord> dataRecords =
          TimeSeriesDataRecord.getTimeSeriesDataRecordsFromNewRelicDataRecords(metricDataRecords);
      dataRecords.forEach(dataRecord -> dataRecord.compress());

      wingsPersistence.save(dataRecords);
    }

    when(cvConfigurationService.getConfiguration(anyString())).thenReturn(cvConfiguration);

    // disabled alert should not throw alert
    continuousVerificationService.triggerTimeSeriesAlertIfNecessary(configId, 0.6, 10);
    assertThat(wingsPersistence.createQuery(Alert.class, excludeAuthority).asList()).isEmpty();

    cvConfiguration.setAlertEnabled(true);
    wingsPersistence.save(cvConfiguration);
    // lower than threshold, no alert should be thrown
    continuousVerificationService.triggerTimeSeriesAlertIfNecessary(configId, 0.4, 10);
    assertThat(wingsPersistence.createQuery(Alert.class, excludeAuthority).asList()).isEmpty();

    // throw alert
    continuousVerificationService.triggerTimeSeriesAlertIfNecessary(configId, 0.6, 10);
    waitForAlert(1, Optional.empty());
    List<Alert> alerts = wingsPersistence.createQuery(Alert.class, excludeAuthority).asList();

    assertThat(alerts).hasSize(1);
    final Alert alert = alerts.get(0);
    assertThat(alert.getAppId()).isEqualTo(appId);
    assertThat(alert.getAccountId()).isEqualTo(accountId);
    assertThat(alert.getType()).isEqualTo(AlertType.CONTINUOUS_VERIFICATION_ALERT);
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.Open);
    assertThat(alert.getCategory()).isEqualTo(AlertCategory.ContinuousVerification);
    assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.Error);

    final ContinuousVerificationAlertData alertData = (ContinuousVerificationAlertData) alert.getAlertData();
    assertThat(alertData.getMlAnalysisType()).isEqualTo(MLAnalysisType.TIME_SERIES);
    assertThat(alertData.getRiskScore()).isEqualTo(0.6);
    assertThat(alertData.getCvConfiguration().getUuid()).isEqualTo(configId);
    assertThat(alertData.getLogAnomaly()).isNull();
    assertThat(alertData.getHighRiskTxns().size()).isEqualTo(5);

    // same minute should not throw another alert
    continuousVerificationService.triggerTimeSeriesAlertIfNecessary(configId, 0.6, 10);
    sleep(ofMillis(2000));
    alerts = wingsPersistence.createQuery(Alert.class, excludeAuthority).asList();
    assertThat(alerts).hasSize(1);

    // diff minute within an hour should not throw an alert
    continuousVerificationService.triggerTimeSeriesAlertIfNecessary(configId, 0.6, 20);
    waitForAlert(1, Optional.empty());

    alerts = wingsPersistence.createQuery(Alert.class, excludeAuthority).asList();
    assertThat(alerts).hasSize(1);
    alerts.forEach(cvAlert -> assertThat(cvAlert.getStatus()).isEqualTo(AlertStatus.Open));

    // diff minute after an hour but within 4 hours should not throw an alert
    continuousVerificationService.triggerTimeSeriesAlertIfNecessary(configId, 0.6, 80);
    waitForAlert(1, Optional.empty());
    alerts = wingsPersistence.createQuery(Alert.class, excludeAuthority).asList();
    assertThat(alerts).hasSize(1);
    alerts.forEach(cvAlert -> assertThat(cvAlert.getStatus()).isEqualTo(AlertStatus.Open));

    // diff minute after 4 hours should trigger an alert
    continuousVerificationService.triggerTimeSeriesAlertIfNecessary(configId, 0.6, 250);
    waitForAlert(2, Optional.empty());

    alerts = wingsPersistence.createQuery(Alert.class, excludeAuthority).asList();
    assertThat(alerts).hasSize(2);
    alerts.forEach(cvAlert -> assertThat(cvAlert.getStatus()).isEqualTo(AlertStatus.Open));

    // diff minute within an hour of last alert should not trigger an alert
    continuousVerificationService.triggerTimeSeriesAlertIfNecessary(configId, 0.6, 90);
    waitForAlert(2, Optional.empty());

    alerts = wingsPersistence.createQuery(Alert.class, excludeAuthority).asList();
    assertThat(alerts).hasSize(2);
    alerts.forEach(cvAlert -> assertThat(cvAlert.getStatus()).isEqualTo(AlertStatus.Open));

    // less risk score should close all the alerts
    continuousVerificationService.triggerTimeSeriesAlertIfNecessary(configId, 0.4, 90);
    waitForAlert(2, Optional.of(AlertStatus.Closed));
    alerts = wingsPersistence.createQuery(Alert.class, excludeAuthority).asList();
    assertThat(alerts).hasSize(2);
    alerts.forEach(cvAlert -> assertThat(cvAlert.getStatus()).isEqualTo(AlertStatus.Closed));

    // new risk should open another alert
    continuousVerificationService.triggerTimeSeriesAlertIfNecessary(configId, 0.6, 90);
    waitForAlert(3, Optional.empty());
    assertThat(wingsPersistence.createQuery(Alert.class, excludeAuthority)
                   .filter(AlertKeys.status, AlertStatus.Closed)
                   .count())
        .isEqualTo(2);
    assertThat(
        wingsPersistence.createQuery(Alert.class, excludeAuthority).filter(AlertKeys.status, AlertStatus.Open).count())
        .isEqualTo(1);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testTriggerLogAnalysisAlertIfNecessary() {
    LogsCVConfiguration cvConfiguration = (LogsCVConfiguration) wingsPersistence.get(CVConfiguration.class, cvConfigId);
    cvConfiguration.setAlertEnabled(false);
    wingsPersistence.save(cvConfiguration);
    when(cvConfigurationService.getConfiguration(anyString())).thenReturn(cvConfiguration);

    final String configId = cvConfiguration.getUuid();

    SplunkAnalysisCluster splunkAnalysisCluster = new SplunkAnalysisCluster();
    splunkAnalysisCluster.setText("msg1");
    Map<String, Map<String, SplunkAnalysisCluster>> unknownClusters = new HashMap<>();
    unknownClusters.put("1", new HashMap<>());
    unknownClusters.get("1").put("host1", splunkAnalysisCluster);
    unknownClusters.put("2", new HashMap<>());
    splunkAnalysisCluster = new SplunkAnalysisCluster();
    splunkAnalysisCluster.setText("msg2");
    unknownClusters.get("2").put("host1", splunkAnalysisCluster);
    splunkAnalysisCluster = new SplunkAnalysisCluster();
    splunkAnalysisCluster.setText("msg2");
    unknownClusters.get("2").put("host2", splunkAnalysisCluster);
    unknownClusters.get("2").put("host3", splunkAnalysisCluster);

    LogMLAnalysisRecord logMLAnalysisRecord = new LogMLAnalysisRecord();
    logMLAnalysisRecord.setUnknown_clusters(unknownClusters);
    // disabled alert should not throw alert
    continuousVerificationService.triggerLogAnalysisAlertIfNecessary(configId, logMLAnalysisRecord, 10);
    assertThat(wingsPersistence.createQuery(Alert.class, excludeAuthority).asList()).isEmpty();

    cvConfiguration.setAlertEnabled(true);
    wingsPersistence.save(cvConfiguration);

    // throw alert
    continuousVerificationService.triggerLogAnalysisAlertIfNecessary(configId, logMLAnalysisRecord, 10);
    waitForAlert(2, Optional.empty());
    List<Alert> alerts = wingsPersistence.createQuery(Alert.class, excludeAuthority).asList();

    assertThat(alerts).hasSize(2);
    Set<String> alertAnomalies = new HashSet<>();
    alerts.forEach(alert -> {
      assertThat(alert.getAppId()).isEqualTo(appId);
      assertThat(alert.getAccountId()).isEqualTo(accountId);
      assertThat(alert.getType()).isEqualTo(AlertType.CONTINUOUS_VERIFICATION_ALERT);
      assertThat(alert.getStatus()).isEqualTo(AlertStatus.Open);
      assertThat(alert.getCategory()).isEqualTo(AlertCategory.ContinuousVerification);
      assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.Error);

      final ContinuousVerificationAlertData alertData = (ContinuousVerificationAlertData) alert.getAlertData();
      assertThat(alertData.getMlAnalysisType()).isEqualTo(MLAnalysisType.LOG_ML);
      assertThat(alertData.getCvConfiguration().getUuid()).isEqualTo(configId);
      assertThat(alertData.getLogAnomaly()).isNotNull();

      if (alertData.getLogAnomaly().equals("msg1")) {
        assertThat(alertData.getHosts()).isEqualTo(Sets.newHashSet("host1"));
      }

      if (alertData.getLogAnomaly().equals("msg2")) {
        assertThat(alertData.getHosts()).isEqualTo(Sets.newHashSet("host1", "host2", "host3"));
      }
      alertAnomalies.add(alertData.getLogAnomaly());
    });

    assertThat(alertAnomalies).isEqualTo(Sets.newHashSet("msg1", "msg2"));
    // same minute should not throw another alert
    continuousVerificationService.triggerLogAnalysisAlertIfNecessary(configId, logMLAnalysisRecord, 10);
    sleep(ofMillis(2000));
    alerts = wingsPersistence.createQuery(Alert.class, excludeAuthority).asList();
    assertThat(alerts).hasSize(2);

    // diff minute should throw another alert
    continuousVerificationService.triggerLogAnalysisAlertIfNecessary(configId, logMLAnalysisRecord, 30);
    waitForAlert(4, Optional.empty());

    alerts = wingsPersistence.createQuery(Alert.class, excludeAuthority).asList();
    assertThat(alerts).hasSize(4);
  }

  private List<CVFeedbackRecord> getFeedbacks() {
    CVFeedbackRecord record = CVFeedbackRecord.builder()
                                  .cvConfigId(cvConfigId)
                                  .actionTaken(FeedbackAction.ADD_TO_BASELINE)
                                  .envId(envId)
                                  .serviceId(serviceId)
                                  .build();

    CVFeedbackRecord record2 = CVFeedbackRecord.builder()
                                   .cvConfigId(cvConfigId)
                                   .actionTaken(FeedbackAction.UPDATE_PRIORITY)
                                   .priority(FeedbackPriority.P2)
                                   .envId(envId)
                                   .serviceId(serviceId)
                                   .build();

    Map<FeedbackAction, List<CVFeedbackRecord>> feedbackActionListMap = new HashMap<>();
    feedbackActionListMap.put(FeedbackAction.UPDATE_PRIORITY, Arrays.asList(record2));
    feedbackActionListMap.put(FeedbackAction.ADD_TO_BASELINE, Arrays.asList(record));
    return Arrays.asList(record, record2);
  }

  private void setupFeedbacks(boolean withFeedbackData) throws Exception {
    LogAnalysisService logAnalysisService = injector.getInstance(LogAnalysisService.class);
    Call<RestResponse<List<CVFeedbackRecord>>> managerCall = mock(Call.class);
    if (withFeedbackData) {
      when(managerCall.execute()).thenReturn(Response.success(new RestResponse<>(getFeedbacks())));
    } else {
      when(managerCall.execute()).thenReturn(Response.success(new RestResponse<>(new ArrayList<>())));
    }
    when(verificationManagerClient.getFeedbackList(anyString(), anyString())).thenReturn(managerCall);

    writeField(logAnalysisService, "managerClient", verificationManagerClient, true);
    writeField(continuousVerificationService, "logAnalysisService", logAnalysisService, true);

    Call<RestResponse<Boolean>> managerFeatureFlagCall = mock(Call.class);
    when(managerFeatureFlagCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isFeatureEnabled(FeatureName.CV_FEEDBACKS, accountId))
        .thenReturn(managerFeatureFlagCall);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testCreateFeedbackAnalysisTaskNoPrevFeedbackAnalysisRecord() throws Exception {
    // setup mocks
    setupFeedbacks(true);

    // initally there should be no tasks even if we trigger
    continuousVerificationService.triggerFeedbackAnalysis(accountId);
    List<LearningEngineAnalysisTask> learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.ml_analysis_type, FEEDBACK_ANALYSIS)
            .asList();
    assertThat(learningEngineAnalysisTasks).isEmpty();

    int oldMinute = (int) TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary()) - 15;

    LogMLAnalysisRecord oldLogAnalysisRecord =
        LogMLAnalysisRecord.builder().appId(appId).cvConfigId(cvConfigId).logCollectionMinute(oldMinute).build();

    oldLogAnalysisRecord.setAnalysisStatus(LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE);

    wingsPersistence.save(oldLogAnalysisRecord);

    continuousVerificationService.triggerFeedbackAnalysis(accountId);
    learningEngineAnalysisTasks = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                      .filter(LearningEngineAnalysisTaskKeys.ml_analysis_type, FEEDBACK_ANALYSIS)
                                      .asList();
    assertThat(learningEngineAnalysisTasks).hasSize(1);
    assertThat(learningEngineAnalysisTasks.get(0).getAnalysis_minute()).isEqualTo(oldMinute);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testCreateFeedbackAnalysisTaskNoFeedbacks() throws Exception {
    // setup mocks
    setupFeedbacks(false);
    // initally there should be no tasks even if we trigger
    continuousVerificationService.triggerFeedbackAnalysis(accountId);
    List<LearningEngineAnalysisTask> learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.ml_analysis_type, FEEDBACK_ANALYSIS)
            .asList();
    assertThat(learningEngineAnalysisTasks).isEmpty();

    int oldMinute = (int) TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary()) - 15;

    LogMLAnalysisRecord oldFeedbackRecord =
        LogMLAnalysisRecord.builder().appId(appId).cvConfigId(cvConfigId).logCollectionMinute(oldMinute).build();

    oldFeedbackRecord.setAnalysisStatus(LogMLAnalysisStatus.FEEDBACK_ANALYSIS_COMPLETE);

    wingsPersistence.save(oldFeedbackRecord);

    continuousVerificationService.triggerFeedbackAnalysis(accountId);
    learningEngineAnalysisTasks = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                      .filter(LearningEngineAnalysisTaskKeys.ml_analysis_type, FEEDBACK_ANALYSIS)
                                      .asList();
    assertThat(learningEngineAnalysisTasks).isEmpty();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testCreateFeedbackAnalysisTaskNotYetTimeForNewTask() throws Exception {
    // setup mocks
    setupFeedbacks(false);
    // initally there should be no tasks even if we trigger
    continuousVerificationService.triggerFeedbackAnalysis(accountId);
    List<LearningEngineAnalysisTask> learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.ml_analysis_type, FEEDBACK_ANALYSIS)
            .asList();
    assertThat(learningEngineAnalysisTasks).isEmpty();

    int oldMinute = (int) TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary()) - 1;

    LogMLAnalysisRecord oldFeedbackRecord =
        LogMLAnalysisRecord.builder().appId(appId).cvConfigId(cvConfigId).logCollectionMinute(oldMinute).build();

    oldFeedbackRecord.setAnalysisStatus(LogMLAnalysisStatus.FEEDBACK_ANALYSIS_COMPLETE);

    wingsPersistence.save(oldFeedbackRecord);

    LogMLAnalysisRecord oldLERecord =
        LogMLAnalysisRecord.builder().appId(appId).cvConfigId(cvConfigId).logCollectionMinute(oldMinute).build();

    oldLERecord.setAnalysisStatus(LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE);

    wingsPersistence.save(oldLERecord);

    continuousVerificationService.triggerFeedbackAnalysis(accountId);
    learningEngineAnalysisTasks = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                      .filter(LearningEngineAnalysisTaskKeys.ml_analysis_type, FEEDBACK_ANALYSIS)
                                      .asList();
    assertThat(learningEngineAnalysisTasks).isEmpty();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testCreateFeedbackAnalysisTaskBasedOnOldLE() throws Exception {
    // setup mocks
    setupFeedbacks(true);
    // initally there should be no tasks even if we trigger
    continuousVerificationService.triggerFeedbackAnalysis(accountId);
    List<LearningEngineAnalysisTask> learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.ml_analysis_type, FEEDBACK_ANALYSIS)
            .asList();
    assertThat(learningEngineAnalysisTasks).isEmpty();

    int oldMinute = (int) TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary()) - 1;
    LogMLAnalysisRecord oldFeedbackRecord =
        LogMLAnalysisRecord.builder().appId(appId).cvConfigId(cvConfigId).logCollectionMinute(oldMinute - 15).build();
    oldFeedbackRecord.setAnalysisStatus(LogMLAnalysisStatus.FEEDBACK_ANALYSIS_COMPLETE);
    wingsPersistence.save(oldFeedbackRecord);

    LogMLAnalysisRecord oldLERecord =
        LogMLAnalysisRecord.builder().appId(appId).cvConfigId(cvConfigId).logCollectionMinute(oldMinute).build();

    oldLERecord.setAnalysisStatus(LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE);

    wingsPersistence.save(oldLERecord);

    continuousVerificationService.triggerFeedbackAnalysis(accountId);
    learningEngineAnalysisTasks = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                      .filter(LearningEngineAnalysisTaskKeys.ml_analysis_type, FEEDBACK_ANALYSIS)
                                      .asList();
    assertThat(learningEngineAnalysisTasks).hasSize(1);
    assertThat(learningEngineAnalysisTasks.get(0).getAnalysis_minute()).isEqualTo(oldMinute);
  }

  private AnalysisContext createDatadogLogAnalysisContext(int startMinute) {
    String messageField = UUID.randomUUID().toString();
    String timestampFieldFormat = UUID.randomUUID().toString();

    CustomLogDataCollectionInfo dataCollectionInfo = CustomLogDataCollectionInfo.builder()
                                                         .baseUrl(datadogConfig.getUrl())
                                                         .validationUrl(DatadogConfig.validationUrl)
                                                         .dataUrl(DatadogConfig.LOG_API_PATH_SUFFIX)
                                                         .headers(new HashMap<>())
                                                         .options(datadogConfig.fetchLogOptionsMap())
                                                         .query("test query")
                                                         .hosts(Sets.newHashSet(DUMMY_HOST_NAME))
                                                         .stateType(StateType.DATA_DOG_LOG)
                                                         .applicationId(appId)
                                                         .stateExecutionId(stateExecutionId)
                                                         .workflowId(workflowId)
                                                         .workflowExecutionId(workflowExecutionId)
                                                         .serviceId(serviceId)

                                                         .hostnameSeparator(DatadogLogState.HOST_NAME_SEPARATOR)
                                                         .shouldInspectHosts(true)
                                                         .collectionFrequency(1)
                                                         .collectionTime(15)
                                                         .accountId(accountId)
                                                         .build();
    return createAnalysisContext(dataCollectionInfo, startMinute, StateType.DATA_DOG_LOG, datadogConnectorId);
  }

  private AnalysisContext createSUMOAnalysisContext(long startTimeInterval) {
    SumoDataCollectionInfo sumoDataCollectionInfo =
        SumoDataCollectionInfo.builder()
            .sumoConfig(sumoConfig)
            .accountId(accountId)
            .applicationId(appId)
            .stateExecutionId(stateExecutionId)
            .workflowId(workflowId)
            .workflowExecutionId(workflowExecutionId)
            .serviceId(serviceId)
            .query(query)
            .hosts(Sets.newHashSet(DUMMY_HOST_NAME))
            .hostnameField("{host.hostname}")
            .encryptedDataDetails(secretManager.getEncryptionDetails(sumoConfig, appId, workflowExecutionId))
            .build();

    return createAnalysisContext(sumoDataCollectionInfo, startTimeInterval, StateType.SUMO, connectorId);
  }

  private AnalysisContext createAnalysisContext(
      DataCollectionInfo dataCollectionInfo, long startMinute, StateType stateType, String connectorId) {
    return AnalysisContext.builder()
        .accountId(accountId)
        .appId(appId)
        .workflowId(workflowId)
        .query(query)
        .analysisType(MLAnalysisType.LOG_CLUSTER)
        .workflowExecutionId(workflowExecutionId)
        .stateExecutionId(stateExecutionId)
        .analysisServerConfigId(connectorId)
        .serviceId(serviceId)
        .controlNodes(Collections.singletonMap("host1", DEFAULT_GROUP_NAME))
        .testNodes(Collections.singletonMap("host1", DEFAULT_GROUP_NAME))
        .isSSL(true)
        .analysisServerConfigId(connectorId)
        .appPort(9090)
        .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
        .timeDuration(1)
        .stateType(stateType)
        .correlationId(UUID.randomUUID().toString())
        .prevWorkflowExecutionId("-1")
        .dataCollectionInfo(dataCollectionInfo)
        .startDataCollectionMinute(startMinute)
        .hostNameField("pod_name")
        .build();
  }

  private DatadogCVServiceConfiguration getNRConfig() {
    Map<String, String> dockerMetrics = new HashMap<>();
    dockerMetrics.put("service_name:harness", "docker.cpu.usage, docker.mem.rss");
    DatadogCVServiceConfiguration config = DatadogCVServiceConfiguration.builder().dockerMetrics(dockerMetrics).build();
    config.setConnectorId(datadogConnectorId);
    config.setUuid(generateUuid());
    config.setAccountId(accountId);
    config.setAppId(appId);
    config.setServiceId(serviceId);
    config.setEnabled24x7(true);
    config.setStateType(StateType.DATA_DOG);
    return config;
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testDataCollectionAfter2Hours() throws Exception {
    long currentTime = Timestamp.currentMinuteBoundary();

    DatadogCVServiceConfiguration nrConfig = getNRConfig();
    wingsPersistence.save(nrConfig);
    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Lists.newArrayList(nrConfig));
    // create metric data
    NewRelicMetricDataRecord dataRecord =
        NewRelicMetricDataRecord.builder()
            .cvConfigId(nrConfig.getUuid())
            .dataCollectionMinute((int) TimeUnit.MILLISECONDS.toMinutes(currentTime) - 400)
            .level(ClusterLevel.HF)
            .build();

    final List<TimeSeriesDataRecord> dataRecords =
        TimeSeriesDataRecord.getTimeSeriesDataRecordsFromNewRelicDataRecords(Lists.newArrayList(dataRecord));
    dataRecords.forEach(record -> record.compress());
    wingsPersistence.save(dataRecords);

    long expectedEnd = currentTime - TimeUnit.MINUTES.toMillis(2);
    long expectedStart = expectedEnd - TimeUnit.MINUTES.toMillis(PREDECTIVE_HISTORY_MINUTES * 2);
    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    continuousVerificationService.triggerAPMDataCollection(accountId);
    verify(delegateService).queueTask(taskCaptor.capture());
    APMDataCollectionInfo info = (APMDataCollectionInfo) taskCaptor.getValue().getData().getParameters()[0];
    assertThat(info.getStartTime()).isEqualTo(expectedStart);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testDataCollectionHappyCase() throws Exception {
    long currentTime = Timestamp.currentMinuteBoundary();

    DatadogCVServiceConfiguration nrConfig = getNRConfig();
    wingsPersistence.save(nrConfig);
    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Lists.newArrayList(nrConfig));
    // create metric data
    NewRelicMetricDataRecord dataRecord =
        NewRelicMetricDataRecord.builder()
            .cvConfigId(nrConfig.getUuid())
            .dataCollectionMinute((int) TimeUnit.MILLISECONDS.toMinutes(currentTime) - 60)
            .level(ClusterLevel.HF)
            .build();

    final List<TimeSeriesDataRecord> dataRecords =
        TimeSeriesDataRecord.getTimeSeriesDataRecordsFromNewRelicDataRecords(Lists.newArrayList(dataRecord));
    dataRecords.forEach(record -> record.compress());
    wingsPersistence.save(dataRecords);

    long expectedEnd = currentTime - TimeUnit.MINUTES.toMillis(2);
    long expectedStart = TimeUnit.MINUTES.toMillis(TimeUnit.MILLISECONDS.toMinutes(currentTime) - 60);
    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    continuousVerificationService.triggerAPMDataCollection(accountId);
    verify(delegateService).queueTask(taskCaptor.capture());
    APMDataCollectionInfo info = (APMDataCollectionInfo) taskCaptor.getValue().getData().getParameters()[0];
    assertThat(info.getStartTime()).isEqualTo(expectedStart);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testDataCollectionFirstCollection() throws Exception {
    long currentTime = Timestamp.currentMinuteBoundary();

    DatadogCVServiceConfiguration nrConfig = getNRConfig();
    wingsPersistence.save(nrConfig);
    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Lists.newArrayList(nrConfig));

    long expectedStart = currentTime - TimeUnit.MINUTES.toMillis(TIME_DELAY_QUERY_MINS + 135);
    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    continuousVerificationService.triggerAPMDataCollection(accountId);
    verify(delegateService).queueTask(taskCaptor.capture());
    APMDataCollectionInfo info = (APMDataCollectionInfo) taskCaptor.getValue().getData().getParameters()[0];
    assertThat(info.getStartTime()).isEqualTo(expectedStart);
  }

  private long getFlooredTime(long currentTime, long delta) {
    long expectedStart = currentTime - delta;
    if (Math.floorMod(expectedStart - 1, CRON_POLL_INTERVAL_IN_MINUTES) != 0) {
      expectedStart -= Math.floorMod(expectedStart - 1, CRON_POLL_INTERVAL_IN_MINUTES);
    }
    return expectedStart;
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testLogsDataCollectionAfter2Hours() throws Exception {
    long currentTime = Timestamp.currentMinuteBoundary();

    LogsCVConfiguration sumoConfig = (LogsCVConfiguration) wingsPersistence.get(CVConfiguration.class, cvConfigId);
    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Lists.newArrayList(sumoConfig));

    LogDataRecord dataRecord = LogDataRecord.builder()
                                   .cvConfigId(cvConfigId)
                                   .logCollectionMinute((int) TimeUnit.MILLISECONDS.toMinutes(currentTime) - 400)
                                   .clusterLevel(ClusterLevel.HF)
                                   .build();

    wingsPersistence.save(dataRecord);

    long expectedStart = TimeUnit.MILLISECONDS.toMinutes(currentTime) - PREDECTIVE_HISTORY_MINUTES;

    // here it brings it closer to the actual 15min boundary
    sumoConfig.setBaselineStartMinute(expectedStart);
    long expectedEnd = sumoConfig.getBaselineStartMinute() + CRON_POLL_INTERVAL_IN_MINUTES - 1;
    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);

    continuousVerificationService.triggerLogDataCollection(accountId);

    verify(delegateService).queueTask(taskCaptor.capture());
    SumoDataCollectionInfo info = (SumoDataCollectionInfo) taskCaptor.getValue().getData().getParameters()[0];
    assertThat(TimeUnit.MINUTES.toMillis(sumoConfig.getBaselineStartMinute())).isEqualTo(info.getStartTime());
    assertThat(TimeUnit.MINUTES.toMillis(expectedEnd)).isEqualTo(info.getEndTime());
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testLogsL1ClusteringHalfBefore2hoursAndHalfAfter() {
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    continuousVerificationService.triggerLogsL1Clustering(accountId);
    List<LearningEngineAnalysisTask> learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();
    assertThat(0).isEqualTo(learningEngineAnalysisTasks.size());

    int numOfMinutes = 10;
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
        logDataRecord.setClusterLevel(ClusterLevel.H0);
        logDataRecord.setLogCollectionMinute(currentMinute - 125 + i);
        wingsPersistence.save(logDataRecord);

        if (i % 2 == 0) {
          logDataRecord.setUuid(null);
          logDataRecord.setClusterLevel(ClusterLevel.L0);
          wingsPersistence.save(logDataRecord);
        }
      }
    }

    continuousVerificationService.triggerLogsL1Clustering(accountId);
    learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("appId", appId).asList();

    Set<String> hosts = new HashSet<>();
    for (int i = 0; i < numOfHosts; i++) {
      hosts.add("host-" + i);
    }
    assertThat(learningEngineAnalysisTasks).hasSize(2);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testLogsDataCollectionHappyCase() throws Exception {
    long currentTime = Timestamp.currentMinuteBoundary();

    LogsCVConfiguration sumoConfig = (LogsCVConfiguration) wingsPersistence.get(CVConfiguration.class, cvConfigId);
    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Lists.newArrayList(sumoConfig));

    // save 2 hour old data
    LogDataRecord dataRecord = LogDataRecord.builder()
                                   .cvConfigId(cvConfigId)
                                   .logCollectionMinute((int) TimeUnit.MILLISECONDS.toMinutes(currentTime) - 60)
                                   .clusterLevel(ClusterLevel.HF)
                                   .build();

    wingsPersistence.save(dataRecord);
    long expectedStart = currentTime - TimeUnit.MINUTES.toMillis(60) + TimeUnit.MINUTES.toMillis(1);

    long expectedEnd = expectedStart + TimeUnit.MINUTES.toMillis(CRON_POLL_INTERVAL_IN_MINUTES - 1);
    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);

    continuousVerificationService.triggerLogDataCollection(accountId);

    verify(delegateService).queueTask(taskCaptor.capture());
    SumoDataCollectionInfo info = (SumoDataCollectionInfo) taskCaptor.getValue().getData().getParameters()[0];
    assertThat(expectedStart).isEqualTo(info.getStartTime());
    assertThat(expectedEnd).isEqualTo(info.getEndTime());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testLogsDataCollectionNoDataSoFar() throws Exception {
    long currentTime = Timestamp.currentMinuteBoundary();

    LogsCVConfiguration sumoConfig = (LogsCVConfiguration) wingsPersistence.get(CVConfiguration.class, cvConfigId);
    sumoConfig.setBaselineStartMinute(TimeUnit.MILLISECONDS.toMinutes(currentTime) - 30);

    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Lists.newArrayList(sumoConfig));

    // save 2 hour old data
    LogDataRecord dataRecord = LogDataRecord.builder()
                                   .cvConfigId(cvConfigId)
                                   .logCollectionMinute(sumoConfig.getBaselineEndMinute())
                                   .clusterLevel(ClusterLevel.HF)
                                   .build();

    long expectedStart = currentTime - TimeUnit.MINUTES.toMillis(60) + TimeUnit.MINUTES.toMillis(1);

    long expectedEnd = expectedStart + TimeUnit.MINUTES.toMillis(CRON_POLL_INTERVAL_IN_MINUTES - 1);
    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);

    continuousVerificationService.triggerLogDataCollection(accountId);

    verify(delegateService).queueTask(taskCaptor.capture());
    SumoDataCollectionInfo info = (SumoDataCollectionInfo) taskCaptor.getValue().getData().getParameters()[0];
    assertThat(TimeUnit.MINUTES.toMillis(sumoConfig.getBaselineStartMinute())).isEqualTo(info.getStartTime());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testLogsDataCollectionBaselineEndMoreThan2Hours() throws Exception {
    long currentTime = Timestamp.currentMinuteBoundary();

    LogsCVConfiguration sumoConfig = (LogsCVConfiguration) wingsPersistence.get(CVConfiguration.class, cvConfigId);
    sumoConfig.setBaselineStartMinute(TimeUnit.MILLISECONDS.toMinutes(currentTime) - 180);
    sumoConfig.setBaselineEndMinute(sumoConfig.getBaselineStartMinute() + 30);

    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Lists.newArrayList(sumoConfig));

    long expectedStart = TimeUnit.MILLISECONDS.toMinutes(currentTime) - PREDECTIVE_HISTORY_MINUTES;

    // here it brings it closer to the actual 15min boundary
    sumoConfig.setBaselineStartMinute(expectedStart);
    long expectedEnd = sumoConfig.getBaselineStartMinute() + CRON_POLL_INTERVAL_IN_MINUTES - 1;
    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);

    continuousVerificationService.triggerLogDataCollection(accountId);

    verify(delegateService).queueTask(taskCaptor.capture());
    SumoDataCollectionInfo info = (SumoDataCollectionInfo) taskCaptor.getValue().getData().getParameters()[0];
    assertThat(TimeUnit.MINUTES.toMillis(sumoConfig.getBaselineStartMinute())).isEqualTo(info.getStartTime());
    assertThat(TimeUnit.MINUTES.toMillis(expectedEnd)).isEqualTo(info.getEndTime());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testFeedbackEngineTaskRestartAfter2HoursNoNewLogTask() throws Exception {
    // mock setup
    Call<RestResponse<Boolean>> managerFeatureFlagCall = mock(Call.class);
    when(managerFeatureFlagCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isFeatureEnabled(FeatureName.CV_FEEDBACKS, accountId))
        .thenReturn(managerFeatureFlagCall);
    setupFeedbacks(true);

    long currentTime = Timestamp.currentMinuteBoundary();
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(currentTime);
    int lastLogTime = (int) getFlooredTime(currentMinute, PREDECTIVE_HISTORY_MINUTES) - 1 - PREDECTIVE_HISTORY_MINUTES;
    LogMLAnalysisRecord record = LogMLAnalysisRecord.builder()
                                     .cvConfigId(cvConfigId)
                                     .logCollectionMinute(lastLogTime)
                                     .stateExecutionId(cvConfigId + "stateExecutionId")
                                     .appId(appId)
                                     .uuid(generateUuid())
                                     .build();
    record.setAnalysisStatus(LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE);

    LogMLAnalysisRecord record2 = LogMLAnalysisRecord.builder()
                                      .cvConfigId(cvConfigId)
                                      .logCollectionMinute(lastLogTime)
                                      .stateExecutionId(cvConfigId + "stateExecutionId2")
                                      .appId(appId)
                                      .uuid(generateUuid())
                                      .build();
    record2.setAnalysisStatus(LogMLAnalysisStatus.FEEDBACK_ANALYSIS_COMPLETE);

    wingsPersistence.save(Arrays.asList(record, record2));

    continuousVerificationService.triggerFeedbackAnalysis(accountId);

    List<LearningEngineAnalysisTask> tasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.cvConfigId, cvConfigId)
            .filter(LearningEngineAnalysisTaskKeys.ml_analysis_type, FEEDBACK_ANALYSIS)
            .asList();
    assertThat(tasks).isNullOrEmpty();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testFeedbackEngineTaskRestartAfter2HoursNewLogTask() throws Exception {
    // mock setup
    Call<RestResponse<Boolean>> managerFeatureFlagCall = mock(Call.class);
    when(managerFeatureFlagCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isFeatureEnabled(FeatureName.CV_FEEDBACKS, accountId))
        .thenReturn(managerFeatureFlagCall);
    setupFeedbacks(true);

    long currentTime = Timestamp.currentMinuteBoundary();
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(currentTime);
    int lastLogTime = (int) getFlooredTime(currentMinute, PREDECTIVE_HISTORY_MINUTES) - 1 - PREDECTIVE_HISTORY_MINUTES;

    LogMLAnalysisRecord record2 = LogMLAnalysisRecord.builder()
                                      .cvConfigId(cvConfigId)
                                      .logCollectionMinute(lastLogTime)
                                      .stateExecutionId(cvConfigId + "stateExecutionId2")
                                      .appId(appId)
                                      .uuid(generateUuid())
                                      .build();
    record2.setAnalysisStatus(LogMLAnalysisStatus.FEEDBACK_ANALYSIS_COMPLETE);

    lastLogTime = (int) getFlooredTime(currentMinute, PREDECTIVE_HISTORY_MINUTES) - 1 + CRON_POLL_INTERVAL_IN_MINUTES;
    LogMLAnalysisRecord record = LogMLAnalysisRecord.builder()
                                     .cvConfigId(cvConfigId)
                                     .logCollectionMinute(lastLogTime)
                                     .stateExecutionId(cvConfigId + "stateExecutionId")
                                     .appId(appId)
                                     .uuid(generateUuid())
                                     .build();
    record.setAnalysisStatus(LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE);

    wingsPersistence.save(Arrays.asList(record, record2));

    continuousVerificationService.triggerFeedbackAnalysis(accountId);

    List<LearningEngineAnalysisTask> tasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.cvConfigId, cvConfigId)
            .filter(LearningEngineAnalysisTaskKeys.ml_analysis_type, FEEDBACK_ANALYSIS)
            .asList();
    assertThat(tasks).isNotEmpty();
    assertThat(tasks.size()).isEqualTo(1);
    assertThat(tasks.get(0).getAnalysis_minute()).isEqualTo(record.getLogCollectionMinute());
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testLockCleanup() {
    DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, "quartz_verification_locks");
    BasicDBObject lockObject = new BasicDBObject();
    lockObject.put(ID_KEY, new ObjectId());
    lockObject.put("type", "t");
    lockObject.put("keyGroup", generateUUID());
    lockObject.put("keyName", generateUUID());
    lockObject.put("instanceId", generateUUID());
    lockObject.put("time", new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(6)));
    collection.insert(lockObject);

    assertThat(collection.find().size()).isEqualTo(1);

    continuousVerificationService.cleanupStuckLocks();
    assertThat(collection.find().size()).isEqualTo(0);

    // insert and see its not deleted
    lockObject.put(ID_KEY, new ObjectId());
    lockObject.put("time", new Date(System.currentTimeMillis()));
    collection.insert(lockObject);
    assertThat(collection.find().size()).isEqualTo(1);

    continuousVerificationService.cleanupStuckLocks();
    assertThat(collection.find().size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testTriggerTimeSeriesAnalysisHappyCase() {
    String cvConfigId = generateUuid(), accId = generateUuid();
    DatadogCVServiceConfiguration cvServiceConfiguration = getNRConfig();
    cvServiceConfiguration.setUuid(cvConfigId);
    cvServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
    wingsPersistence.save(cvServiceConfiguration);
    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Arrays.asList(cvServiceConfiguration));
    // save some raw timeseries data
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());

    for (long time = currentMinute - 135; time < currentMinute; time++) {
      TimeSeriesDataRecord dataRecord = TimeSeriesDataRecord.builder()
                                            .cvConfigId(cvConfigId)
                                            .dataCollectionMinute((int) time)
                                            .serviceId(serviceId)
                                            .stateType(cvServiceConfiguration.getStateType())
                                            .groupName(DEFAULT_GROUP_NAME)
                                            .build();
      wingsPersistence.save(dataRecord);
    }

    continuousVerificationService.triggerServiceGuardTimeSeriesAnalysis(accountId);

    LearningEngineAnalysisTask task = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                          .filter(LearningEngineAnalysisTaskKeys.cvConfigId, cvConfigId)
                                          .get();

    assertThat(task).isNotNull();
    assertThat(task.getAnalysis_minute()).isEqualTo(currentMinute - 1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testTriggerTimeSeriesAnalysisHappyCaseWithExperiment() {
    wingsPersistence.save(MLExperiments.builder()
                              .experimentName("textExp")
                              .ml_analysis_type(MLAnalysisType.TIME_SERIES)
                              .is24x7(true)
                              .build());
    testTriggerTimeSeriesAnalysisHappyCase();
    CVConfiguration cvConfiguration = wingsPersistence.createQuery(CVConfiguration.class)
                                          .filter(CVConfiguration.ACCOUNT_ID_KEY, accountId)
                                          .filter(CVConfigurationKeys.stateType, StateType.DATA_DOG)
                                          .get();
    LearningEngineExperimentalAnalysisTask expTask =
        wingsPersistence.createQuery(LearningEngineExperimentalAnalysisTask.class)
            .filter(LearningEngineExperimentalAnalysisTaskKeys.cvConfigId, cvConfiguration.getUuid())
            .get();
    LearningEngineAnalysisTask task = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                          .filter(LearningEngineAnalysisTaskKeys.cvConfigId, cvConfiguration.getUuid())
                                          .get();
    assertThat(expTask).isNotNull();
    assertThat(task).isNotNull();
    assertThat(expTask.getAnalysis_minute()).isEqualTo(task.getAnalysis_minute());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testTriggerTimeSeriesAnalysisWithNoRawData() {
    String cvConfigId = generateUuid(), accId = generateUuid();
    DatadogCVServiceConfiguration cvServiceConfiguration = getNRConfig();
    cvServiceConfiguration.setUuid(cvConfigId);
    cvServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
    wingsPersistence.save(cvServiceConfiguration);
    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Arrays.asList(cvServiceConfiguration));

    continuousVerificationService.triggerServiceGuardTimeSeriesAnalysis(accountId);

    LearningEngineAnalysisTask task = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                          .filter(LearningEngineAnalysisTaskKeys.cvConfigId, cvConfigId)
                                          .get();

    assertThat(task).isNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testTriggerTimeSeriesAnalysisWithNotYetTime() {
    String cvConfigId = generateUuid(), accId = generateUuid();
    DatadogCVServiceConfiguration cvServiceConfiguration = getNRConfig();
    cvServiceConfiguration.setUuid(cvConfigId);
    cvServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
    wingsPersistence.save(cvServiceConfiguration);
    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Arrays.asList(cvServiceConfiguration));

    // save some raw timeseries data
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    for (long time = currentMinute - 135; time < currentMinute + 4; time++) {
      TimeSeriesDataRecord dataRecord = TimeSeriesDataRecord.builder()
                                            .cvConfigId(cvConfigId)
                                            .dataCollectionMinute((int) time)
                                            .serviceId(serviceId)
                                            .stateType(cvServiceConfiguration.getStateType())
                                            .groupName(DEFAULT_GROUP_NAME)
                                            .build();
      wingsPersistence.save(dataRecord);
    }

    // save an analysis also
    TimeSeriesMLAnalysisRecord analysisRecord = TimeSeriesMLAnalysisRecord.builder().build();
    analysisRecord.setAnalysisMinute((int) currentMinute - 1);
    analysisRecord.setCvConfigId(cvConfigId);

    wingsPersistence.save(analysisRecord);

    continuousVerificationService.triggerServiceGuardTimeSeriesAnalysis(accountId);

    LearningEngineAnalysisTask task = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                          .filter(LearningEngineAnalysisTaskKeys.cvConfigId, cvConfigId)
                                          .get();

    assertThat(task).isNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testTriggerTimeSeriesAnalysis2HoursSinceLastAnalysis() {
    String cvConfigId = generateUuid(), accId = generateUuid();
    DatadogCVServiceConfiguration cvServiceConfiguration = getNRConfig();
    cvServiceConfiguration.setUuid(cvConfigId);
    cvServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
    wingsPersistence.save(cvServiceConfiguration);
    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Arrays.asList(cvServiceConfiguration));

    // save some raw timeseries data
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    for (long time = currentMinute - 135; time < currentMinute + 4; time++) {
      TimeSeriesDataRecord dataRecord = TimeSeriesDataRecord.builder()
                                            .cvConfigId(cvConfigId)
                                            .dataCollectionMinute((int) time)
                                            .serviceId(serviceId)
                                            .stateType(cvServiceConfiguration.getStateType())
                                            .groupName(DEFAULT_GROUP_NAME)
                                            .build();
      wingsPersistence.save(dataRecord);
    }

    // save an analysis also
    TimeSeriesMLAnalysisRecord analysisRecord = TimeSeriesMLAnalysisRecord.builder().build();
    analysisRecord.setAnalysisMinute((int) currentMinute - 163);
    analysisRecord.setCvConfigId(cvConfigId);

    wingsPersistence.save(analysisRecord);

    continuousVerificationService.triggerServiceGuardTimeSeriesAnalysis(accountId);

    LearningEngineAnalysisTask task = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                          .filter(LearningEngineAnalysisTaskKeys.cvConfigId, cvConfigId)
                                          .get();

    assertThat(task).isNotNull();
    assertThat(task.getAnalysis_minute()).isEqualTo((int) currentMinute - 118);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testTriggerLogAnalysisBaseline() throws Exception {
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    CVConfiguration sumoConfig = wingsPersistence.createQuery(CVConfiguration.class)
                                     .filter(CVConfiguration.ACCOUNT_ID_KEY, accountId)
                                     .filter(CVConfigurationKeys.stateType, StateType.SUMO)
                                     .get();

    LogsCVConfiguration logConfig = (LogsCVConfiguration) sumoConfig;
    logConfig.setBaselineStartMinute(currentMinute - 29);
    logConfig.setBaselineEndMinute(currentMinute - 15);

    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Arrays.asList(sumoConfig));
    Call<RestResponse<Boolean>> managerFeatureFlagCall = mock(Call.class);
    when(managerFeatureFlagCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isFeatureEnabled(FeatureName.SEND_LOG_ANALYSIS_COMPRESSED, accountId))
        .thenReturn(managerFeatureFlagCall);

    // save some L2 records
    for (long time = logConfig.getBaselineStartMinute(); time < currentMinute - 15; time++) {
      LogDataRecord record = LogDataRecord.builder()
                                 .cvConfigId(sumoConfig.getUuid())
                                 .clusterLevel(ClusterLevel.L2)
                                 .logCollectionMinute(time)
                                 .build();

      LogDataRecord record2 = LogDataRecord.builder()
                                  .cvConfigId(sumoConfig.getUuid())
                                  .clusterLevel(ClusterLevel.H2)
                                  .logCollectionMinute(time)
                                  .build();
      wingsPersistence.save(Arrays.asList(record, record2));
    }

    continuousVerificationService.triggerLogDataAnalysis(accountId);

    LearningEngineAnalysisTask task = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                          .filter(LearningEngineAnalysisTaskKeys.cvConfigId, cvConfigId)
                                          .get();

    assertThat(task).isNotNull();
    assertThat(task.getAnalysis_minute())
        .isEqualTo(logConfig.getBaselineStartMinute() + CRON_POLL_INTERVAL_IN_MINUTES - 1);
    assertThat(task.getTest_input_url()).isNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testTriggerLogAnalysisAfterBaseline() throws Exception {
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    CVConfiguration sumoConfig = wingsPersistence.createQuery(CVConfiguration.class)
                                     .filter(CVConfiguration.ACCOUNT_ID_KEY, accountId)
                                     .filter(CVConfigurationKeys.stateType, StateType.SUMO)
                                     .get();

    LogsCVConfiguration logConfig = (LogsCVConfiguration) sumoConfig;
    logConfig.setBaselineStartMinute(currentMinute - 29);
    logConfig.setBaselineEndMinute(currentMinute - 15);

    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Arrays.asList(sumoConfig));
    Call<RestResponse<Boolean>> managerFeatureFlagCall = mock(Call.class);
    when(managerFeatureFlagCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isFeatureEnabled(FeatureName.SEND_LOG_ANALYSIS_COMPRESSED, accountId))
        .thenReturn(managerFeatureFlagCall);

    LogMLAnalysisRecord analysisRecord = LogMLAnalysisRecord.builder()
                                             .cvConfigId(sumoConfig.getUuid())
                                             .logCollectionMinute((int) logConfig.getBaselineEndMinute())
                                             .build();

    wingsPersistence.save(analysisRecord);

    // save some L2 records
    for (long time = logConfig.getBaselineEndMinute() + 1; time < logConfig.getBaselineEndMinute() + 16; time++) {
      LogDataRecord record = LogDataRecord.builder()
                                 .cvConfigId(sumoConfig.getUuid())
                                 .clusterLevel(ClusterLevel.L2)
                                 .logCollectionMinute(time)
                                 .build();

      LogDataRecord record2 = LogDataRecord.builder()
                                  .cvConfigId(sumoConfig.getUuid())
                                  .clusterLevel(ClusterLevel.H2)
                                  .logCollectionMinute(time)
                                  .build();
      wingsPersistence.save(Arrays.asList(record, record2));
    }

    continuousVerificationService.triggerLogDataAnalysis(accountId);

    LearningEngineAnalysisTask task = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                          .filter(LearningEngineAnalysisTaskKeys.cvConfigId, cvConfigId)
                                          .get();

    assertThat(task).isNotNull();
    assertThat(task.getAnalysis_minute()).isEqualTo(logConfig.getBaselineEndMinute() + CRON_POLL_INTERVAL_IN_MINUTES);
    assertThat(task.getTest_input_url()).isNotNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testTriggerLogAnalysisWithExperiment() throws Exception {
    wingsPersistence.save(
        MLExperiments.builder().experimentName("textExp").ml_analysis_type(MLAnalysisType.LOG_ML).is24x7(true).build());
    testTriggerLogAnalysisBaseline();

    CVConfiguration sumoConfig = wingsPersistence.createQuery(CVConfiguration.class)
                                     .filter(CVConfiguration.ACCOUNT_ID_KEY, accountId)
                                     .filter(CVConfigurationKeys.stateType, StateType.SUMO)
                                     .get();

    LearningEngineAnalysisTask task = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                          .filter(LearningEngineAnalysisTaskKeys.cvConfigId, sumoConfig.getUuid())
                                          .get();

    LearningEngineExperimentalAnalysisTask expTask =
        wingsPersistence.createQuery(LearningEngineExperimentalAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.cvConfigId, sumoConfig.getUuid())
            .get();

    assertThat(task).isNotNull();
    assertThat(expTask).isNotNull();
    assertThat(expTask.getAnalysis_minute()).isEqualTo(task.getAnalysis_minute());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testTriggerLogAnalysisBaselineMoreThan2HrsAgo() throws Exception {
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    CVConfiguration sumoConfig = wingsPersistence.createQuery(CVConfiguration.class)
                                     .filter(CVConfiguration.ACCOUNT_ID_KEY, accountId)
                                     .filter(CVConfigurationKeys.stateType, StateType.SUMO)
                                     .get();

    LogsCVConfiguration logConfig = (LogsCVConfiguration) sumoConfig;
    logConfig.setBaselineStartMinute(currentMinute - 130);
    logConfig.setBaselineEndMinute(currentMinute - 100);

    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Arrays.asList(sumoConfig));
    Call<RestResponse<Boolean>> managerFeatureFlagCall = mock(Call.class);
    when(managerFeatureFlagCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isFeatureEnabled(FeatureName.SEND_LOG_ANALYSIS_COMPRESSED, accountId))
        .thenReturn(managerFeatureFlagCall);

    // save some L2 records
    for (long time = logConfig.getBaselineStartMinute() + 1; time < currentMinute; time++) {
      LogDataRecord record = LogDataRecord.builder()
                                 .cvConfigId(sumoConfig.getUuid())
                                 .clusterLevel(ClusterLevel.L2)
                                 .logCollectionMinute(time)
                                 .build();

      LogDataRecord record2 = LogDataRecord.builder()
                                  .cvConfigId(sumoConfig.getUuid())
                                  .clusterLevel(ClusterLevel.H2)
                                  .logCollectionMinute(time)
                                  .build();
      wingsPersistence.save(Arrays.asList(record, record2));
    }

    continuousVerificationService.triggerLogDataAnalysis(accountId);

    LearningEngineAnalysisTask task = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                          .filter(LearningEngineAnalysisTaskKeys.cvConfigId, cvConfigId)
                                          .get();

    assertThat(task).isNotNull();
    assertThat(task.getAnalysis_minute()).isEqualTo(logConfig.getBaselineStartMinute() + CRON_POLL_INTERVAL_IN_MINUTES);
    assertThat(task.getTest_input_url()).isNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testTriggerLogAnalysisAfterBaselineMoreThan2hrs() throws Exception {
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    CVConfiguration sumoConfig = wingsPersistence.createQuery(CVConfiguration.class)
                                     .filter(CVConfiguration.ACCOUNT_ID_KEY, accountId)
                                     .filter(CVConfigurationKeys.stateType, StateType.SUMO)
                                     .get();

    LogsCVConfiguration logConfig = (LogsCVConfiguration) sumoConfig;
    logConfig.setBaselineStartMinute(currentMinute - 190);
    logConfig.setBaselineEndMinute(currentMinute - 160);

    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Arrays.asList(sumoConfig));
    Call<RestResponse<Boolean>> managerFeatureFlagCall = mock(Call.class);
    when(managerFeatureFlagCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isFeatureEnabled(FeatureName.SEND_LOG_ANALYSIS_COMPRESSED, accountId))
        .thenReturn(managerFeatureFlagCall);

    LogMLAnalysisRecord analysisRecord = LogMLAnalysisRecord.builder()
                                             .cvConfigId(sumoConfig.getUuid())
                                             .logCollectionMinute((int) logConfig.getBaselineStartMinute() + 45)
                                             .build();

    wingsPersistence.save(analysisRecord);

    // save some L2 records
    for (long time = logConfig.getBaselineEndMinute() + 1; time < currentMinute; time++) {
      LogDataRecord record = LogDataRecord.builder()
                                 .cvConfigId(sumoConfig.getUuid())
                                 .clusterLevel(ClusterLevel.L2)
                                 .logCollectionMinute(time)
                                 .build();

      LogDataRecord record2 = LogDataRecord.builder()
                                  .cvConfigId(sumoConfig.getUuid())
                                  .clusterLevel(ClusterLevel.H2)
                                  .logCollectionMinute(time)
                                  .build();
      wingsPersistence.save(Arrays.asList(record, record2));
    }

    continuousVerificationService.triggerLogDataAnalysis(accountId);

    LearningEngineAnalysisTask task = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                          .filter(LearningEngineAnalysisTaskKeys.cvConfigId, cvConfigId)
                                          .get();

    assertThat(task).isNotNull();
    assertThat(currentMinute - task.getAnalysis_minute()).isLessThanOrEqualTo(120); // brings it within the 2hour range.
    assertThat(task.getTest_input_url()).isNotNull();
  }

  @Test
  @Owner(developers = NANDAN)
  @Category(UnitTests.class)
  public void testTrigger247LogDataV2Analysis() throws Exception {
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    CVConfiguration sumoConfig = wingsPersistence.createQuery(CVConfiguration.class)
                                     .filter(CVConfiguration.ACCOUNT_ID_KEY, accountId)
                                     .filter(CVConfigurationKeys.stateType, StateType.SUMO)
                                     .get();

    LogsCVConfiguration logConfig = (LogsCVConfiguration) sumoConfig;
    logConfig.setBaselineStartMinute(currentMinute - 29);
    logConfig.setBaselineEndMinute(currentMinute - 15);
    logConfig.set247LogsV2(true);

    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Arrays.asList(sumoConfig));
    Call<RestResponse<Boolean>> managerFeatureFlagCall = mock(Call.class);
    when(managerFeatureFlagCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isFeatureEnabled(FeatureName.SEND_LOG_ANALYSIS_COMPRESSED, accountId))
        .thenReturn(managerFeatureFlagCall);

    // save some L2 records
    for (long time = logConfig.getBaselineStartMinute(); time < currentMinute - 15; time++) {
      LogDataRecord record = LogDataRecord.builder()
                                 .cvConfigId(sumoConfig.getUuid())
                                 .clusterLevel(ClusterLevel.L2)
                                 .logCollectionMinute(time)
                                 .build();

      LogDataRecord record2 = LogDataRecord.builder()
                                  .cvConfigId(sumoConfig.getUuid())
                                  .clusterLevel(ClusterLevel.H2)
                                  .logCollectionMinute(time)
                                  .build();
      wingsPersistence.save(Arrays.asList(record, record2));
    }

    continuousVerificationService.trigger247LogDataV2Analysis(logConfig);

    LearningEngineAnalysisTask task = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                          .filter(LearningEngineAnalysisTaskKeys.cvConfigId, cvConfigId)
                                          .get();

    assertThat(task).isNotNull();
    assertThat(task.getAnalysis_minute())
        .isEqualTo(logConfig.getBaselineStartMinute() + CRON_POLL_INTERVAL_IN_MINUTES - 1);
    assertThat(task.getTest_input_url()).isNotNull();
  }

  @Test
  @Owner(developers = NANDAN)
  @Category(UnitTests.class)
  public void testTrigger247LogDataV2AnalysisWithNoNewL2RecordsInLastTwoHrs() throws Exception {
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    CVConfiguration sumoConfig = wingsPersistence.createQuery(CVConfiguration.class)
                                     .filter(CVConfiguration.ACCOUNT_ID_KEY, accountId)
                                     .filter(CVConfigurationKeys.stateType, StateType.SUMO)
                                     .get();

    LogsCVConfiguration logConfig = (LogsCVConfiguration) sumoConfig;
    logConfig.setBaselineStartMinute(currentMinute - 190);
    logConfig.setBaselineEndMinute(currentMinute - 160);
    logConfig.set247LogsV2(true);

    when(cvConfigurationService.listConfigurations(accountId)).thenReturn(Arrays.asList(sumoConfig));
    Call<RestResponse<Boolean>> managerFeatureFlagCall = mock(Call.class);
    when(managerFeatureFlagCall.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.isFeatureEnabled(FeatureName.SEND_LOG_ANALYSIS_COMPRESSED, accountId))
        .thenReturn(managerFeatureFlagCall);

    // save a L2 records which is created more than 2 hours ago
    long time = currentMinute - 150;
    LogDataRecord record = LogDataRecord.builder()
                               .cvConfigId(sumoConfig.getUuid())
                               .clusterLevel(ClusterLevel.L2)
                               .logCollectionMinute(time)
                               .build();

    LogDataRecord record2 = LogDataRecord.builder()
                                .cvConfigId(sumoConfig.getUuid())
                                .clusterLevel(ClusterLevel.H2)
                                .logCollectionMinute(time)
                                .build();
    wingsPersistence.save(Arrays.asList(record, record2));

    continuousVerificationService.trigger247LogDataV2Analysis(logConfig);

    LearningEngineAnalysisTask task = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                          .filter(LearningEngineAnalysisTaskKeys.cvConfigId, cvConfigId)
                                          .get();

    assertThat(task).isNull();
  }

  @Test
  @Owner(developers = NANDAN)
  @Category(UnitTests.class)
  public void testTrigger247LogDataV2AnalysisWithExperiment() throws Exception {
    wingsPersistence.save(
        MLExperiments.builder().experimentName("textExp").ml_analysis_type(MLAnalysisType.LOG_ML).is24x7(true).build());
    testTrigger247LogDataV2Analysis();

    CVConfiguration sumoConfig = wingsPersistence.createQuery(CVConfiguration.class)
                                     .filter(CVConfiguration.ACCOUNT_ID_KEY, accountId)
                                     .filter(CVConfigurationKeys.stateType, StateType.SUMO)
                                     .get();

    LearningEngineAnalysisTask task = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                          .filter(LearningEngineAnalysisTaskKeys.cvConfigId, sumoConfig.getUuid())
                                          .get();

    LearningEngineExperimentalAnalysisTask expTask =
        wingsPersistence.createQuery(LearningEngineExperimentalAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.cvConfigId, sumoConfig.getUuid())
            .get();

    assertThat(task).isNotNull();
    assertThat(expTask).isNotNull();
    assertThat(expTask.getAnalysis_minute()).isEqualTo(task.getAnalysis_minute());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testProcessNextTask() {
    CVTask cvTask = CVTask.builder()
                        .accountId(accountId)
                        .dataCollectionInfo(mock(DataCollectionInfoV2.class))
                        .status(ExecutionStatus.QUEUED)
                        .build();
    when(verificationManagerClient.collectCVData(anyString(), anyObject())).then(invocation -> {
      Object[] args = invocation.getArguments();
      Call<Boolean> restCall = mock(Call.class);
      when(restCall.execute()).thenReturn(Response.success(true));
      return restCall;
    });
    when(cvTaskService.getNextTask(eq(accountId)))
        .thenReturn(Optional.of(cvTask))
        .thenReturn(Optional.of(cvTask))
        .thenReturn(Optional.empty());
    continuousVerificationService.processNextCVTasks(accountId);
    verify(verificationManagerClient, times(2)).collectCVData(cvTask.getUuid(), cvTask.getDataCollectionInfo());
  }

  private void waitForAlert(int expectedNumOfAlerts, Optional<AlertStatus> alertStatus) {
    int tryCount = 0;
    long numOfAlerts;
    do {
      Query<Alert> alertQuery = wingsPersistence.createQuery(Alert.class, excludeAuthority);
      if (alertStatus.isPresent()) {
        alertQuery.filter(AlertKeys.status, alertStatus.get());
      }
      numOfAlerts = alertQuery.count();
      tryCount++;
      sleep(ofMillis(500));
    } while (numOfAlerts < expectedNumOfAlerts && tryCount < 10);
  }
}