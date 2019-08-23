package io.harness.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rest.RestResponse.Builder.aRestResponse;
import static io.harness.rule.OwnerRule.PARNIAN;
import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;
import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.PREDECTIVE_HISTORY_MINUTES;
import static software.wings.service.impl.newrelic.LearningEngineAnalysisTask.TIME_SERIES_ANALYSIS_TASK_TIME_OUT;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import io.harness.VerificationBaseTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.rule.OwnerRule.Owner;
import io.harness.security.EncryptionUtils;
import io.harness.service.intfc.ContinuousVerificationService;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.ServiceSecretKey;
import software.wings.beans.ServiceSecretKey.ServiceApiVersion;
import software.wings.beans.ServiceSecretKey.ServiceSecretKeyKeys;
import software.wings.beans.ServiceSecretKey.ServiceType;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.LicenseUtils;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask.LearningEngineAnalysisTaskKeys;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.utils.Misc;
import software.wings.verification.VerificationDataAnalysisResponse;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 1/9/18.
 */
public class LearningEngineAnalysisTest extends VerificationBaseTest {
  @Inject private LearningEngineService learningEngineService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private CVConfigurationService cvConfigurationService;
  @Inject private ContinuousVerificationService continuousVerificationService;
  @Inject private TimeSeriesAnalysisService timeSeriesAnalysisService;
  @Mock private VerificationManagerClientHelper managerClientHelper;

  private String accountId;
  private String appId;
  private String workflowExecutionId;
  private String stateExecutionId;

  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    when(managerClientHelper.callManagerWithRetry(any())).thenReturn(aRestResponse().withResource(true).build());
    doNothing()
        .when(managerClientHelper)
        .notifyManagerForVerificationAnalysis(any(AnalysisContext.class), any(VerificationDataAnalysisResponse.class));
    FieldUtils.writeField(timeSeriesAnalysisService, "managerClientHelper", managerClientHelper, true);
    FieldUtils.writeField(continuousVerificationService, "timeSeriesAnalysisService", timeSeriesAnalysisService, true);
    FieldUtils.writeField(learningEngineService, "managerClientHelper", managerClientHelper, true);
    Account account = anAccount().withAccountName(generateUUID()).build();
    account.setEncryptedLicenseInfo(EncryptionUtils.encrypt(
        LicenseUtils.convertToString(LicenseInfo.builder().accountType(AccountType.PAID).build())
            .getBytes(Charset.forName("UTF-8")),
        null));
    accountId = wingsPersistence.save(account);
    appId = wingsPersistence.save(anApplication().accountId(accountId).name(generateUUID()).build());
    workflowExecutionId = generateUUID();
    stateExecutionId = generateUUID();
  }

  @After
  public void tearDown() {
    LearningEngineAnalysisTask.TIME_SERIES_ANALYSIS_TASK_TIME_OUT = TimeUnit.MINUTES.toMillis(2);
  }

  @Test
  @Category(UnitTests.class)
  public void testQueueWithStatus() {
    int numOfTasks = 100;
    for (int i = 0; i < numOfTasks; i++) {
      workflowExecutionId = UUID.randomUUID().toString();
      stateExecutionId = UUID.randomUUID().toString();
      LearningEngineAnalysisTask learningEngineAnalysisTask = LearningEngineAnalysisTask.builder()
                                                                  .state_execution_id(stateExecutionId)
                                                                  .workflow_execution_id(workflowExecutionId)
                                                                  .executionStatus(ExecutionStatus.QUEUED)
                                                                  .build();
      learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask);
    }

    assertThat(wingsPersistence.createQuery(LearningEngineAnalysisTask.class).count()).isEqualTo(numOfTasks);

    for (int i = 1; i <= numOfTasks; i++) {
      LearningEngineAnalysisTask leTask = learningEngineService.getNextLearningEngineAnalysisTask(
          ServiceApiVersion.V1, Optional.of(false), Optional.empty());
      assertThat(leTask.getExecutionStatus()).isEqualTo(ExecutionStatus.RUNNING);

      assertEquals(numOfTasks - i,
          wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
              .filter("executionStatus", ExecutionStatus.QUEUED)
              .filter("retry", 0)
              .asList()
              .size());
      assertEquals(i,
          wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
              .filter("executionStatus", ExecutionStatus.RUNNING)
              .filter("retry", 1)
              .asList()
              .size());
    }

    assertNull(learningEngineService.getNextLearningEngineAnalysisTask(
        ServiceApiVersion.V1, Optional.of(false), Optional.empty()));
  }

  @Test
  @Category(UnitTests.class)
  public void testQueueWithStatus24x7Task() {
    int numOfTasks = 100;
    for (int i = 0; i < numOfTasks; i++) {
      workflowExecutionId = UUID.randomUUID().toString();
      stateExecutionId = UUID.randomUUID().toString();
      LearningEngineAnalysisTask learningEngineAnalysisTask = LearningEngineAnalysisTask.builder()
                                                                  .state_execution_id(stateExecutionId)
                                                                  .workflow_execution_id(workflowExecutionId)
                                                                  .executionStatus(ExecutionStatus.QUEUED)
                                                                  .is24x7Task(true)
                                                                  .build();
      learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask);
    }

    assertThat(wingsPersistence.createQuery(LearningEngineAnalysisTask.class).count()).isEqualTo(numOfTasks);

    for (int i = 1; i <= numOfTasks; i++) {
      LearningEngineAnalysisTask task = learningEngineService.getNextLearningEngineAnalysisTask(
          ServiceApiVersion.V1, Optional.of(true), Optional.empty());
      assertThat(task.getExecutionStatus()).isEqualTo(ExecutionStatus.RUNNING);

      assertEquals(numOfTasks - i,
          wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
              .filter("executionStatus", ExecutionStatus.QUEUED)
              .filter("retry", 0)
              .asList()
              .size());
    }

    assertNull(learningEngineService.getNextLearningEngineAnalysisTask(
        ServiceApiVersion.V1, Optional.of(true), Optional.empty()));
  }

  @Test
  @Category(UnitTests.class)
  public void testQueueWithAnalysisType() {
    int numOfTasks = 100;
    for (int i = 0; i < numOfTasks; i++) {
      workflowExecutionId = UUID.randomUUID().toString();
      stateExecutionId = UUID.randomUUID().toString();
      LearningEngineAnalysisTask learningEngineAnalysisTask = LearningEngineAnalysisTask.builder()
                                                                  .state_execution_id(stateExecutionId)
                                                                  .workflow_execution_id(workflowExecutionId)
                                                                  .executionStatus(ExecutionStatus.QUEUED)
                                                                  .ml_analysis_type(MLAnalysisType.LOG_CLUSTER)
                                                                  .is24x7Task(true)
                                                                  .build();
      learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask);
    }

    assertThat(wingsPersistence.createQuery(LearningEngineAnalysisTask.class).count()).isEqualTo(numOfTasks);

    LearningEngineAnalysisTask task = learningEngineService.getNextLearningEngineAnalysisTask(ServiceApiVersion.V1,
        Optional.of(true), Optional.of(Lists.newArrayList(MLAnalysisType.TIME_SERIES, MLAnalysisType.LOG_ML)));
    assertThat(task).isNull();

    task = learningEngineService.getNextLearningEngineAnalysisTask(ServiceApiVersion.V1, Optional.of(true),
        Optional.of(Lists.newArrayList(MLAnalysisType.TIME_SERIES, MLAnalysisType.LOG_CLUSTER, MLAnalysisType.LOG_ML)));
    assertThat(task).isNotNull();
    assertThat(task.getMl_analysis_type()).isEqualTo(MLAnalysisType.LOG_CLUSTER);
    assertEquals(numOfTasks - 1,
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.executionStatus, ExecutionStatus.QUEUED)
            .count());

    task = learningEngineService.getNextLearningEngineAnalysisTask(
        ServiceApiVersion.V1, Optional.of(true), Optional.empty());
    assertThat(task).isNotNull();
    assertThat(task.getMl_analysis_type()).isEqualTo(MLAnalysisType.LOG_CLUSTER);
    assertEquals(numOfTasks - 2,
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.executionStatus, ExecutionStatus.QUEUED)
            .count());

    task = learningEngineService.getNextLearningEngineAnalysisTask(
        ServiceApiVersion.V1, Optional.of(true), Optional.of(Lists.newArrayList()));
    assertThat(task).isNotNull();
    assertThat(task.getMl_analysis_type()).isEqualTo(MLAnalysisType.LOG_CLUSTER);
    assertEquals(numOfTasks - 3,
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.executionStatus, ExecutionStatus.QUEUED)
            .count());
  }

  @Test
  @Category(UnitTests.class)
  public void testQueueWithStatus24x7TaskTrue() {
    int numOfTasks = 100;
    for (int i = 0; i < numOfTasks; i++) {
      workflowExecutionId = UUID.randomUUID().toString();
      stateExecutionId = UUID.randomUUID().toString();
      LearningEngineAnalysisTask learningEngineAnalysisTask = LearningEngineAnalysisTask.builder()
                                                                  .state_execution_id(stateExecutionId)
                                                                  .workflow_execution_id(workflowExecutionId)
                                                                  .executionStatus(ExecutionStatus.QUEUED)
                                                                  .build();
      learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask);
    }

    assertThat(wingsPersistence.createQuery(LearningEngineAnalysisTask.class).count()).isEqualTo(numOfTasks);

    for (int i = 1; i <= numOfTasks; i++) {
      LearningEngineAnalysisTask analysisTask = learningEngineService.getNextLearningEngineAnalysisTask(
          ServiceApiVersion.V1, Optional.of(true), Optional.empty());
      assertThat(analysisTask).isNull();

      assertEquals(numOfTasks,
          wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
              .filter("executionStatus", ExecutionStatus.QUEUED)
              .filter("retry", 0)
              .asList()
              .size());
    }

    assertNull(learningEngineService.getNextLearningEngineAnalysisTask(
        ServiceApiVersion.V1, Optional.of(true), Optional.empty()));
  }

  @Test
  @Category(UnitTests.class)
  public void testAlreadyQueued() {
    int numOfTasks = 5;
    for (int i = 0; i < numOfTasks; i++) {
      LearningEngineAnalysisTask learningEngineAnalysisTask = LearningEngineAnalysisTask.builder()
                                                                  .state_execution_id(stateExecutionId)
                                                                  .workflow_execution_id(workflowExecutionId)
                                                                  .executionStatus(ExecutionStatus.QUEUED)
                                                                  .analysis_minute(0)
                                                                  .build();
      learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask);
    }

    List<LearningEngineAnalysisTask> learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority).asList();
    assertThat(learningEngineAnalysisTasks).hasSize(1);
    LearningEngineAnalysisTask analysisTask = learningEngineAnalysisTasks.get(0);
    assertThat(analysisTask.getWorkflow_execution_id()).isEqualTo(workflowExecutionId);
    assertThat(analysisTask.getState_execution_id()).isEqualTo(stateExecutionId);
    assertThat(analysisTask.getAnalysis_minute()).isEqualTo(0);
    assertThat(analysisTask.getRetry()).isEqualTo(0);
  }

  @Test
  @Category(UnitTests.class)
  public void testAlreadyQueuedForMinute() {
    LearningEngineAnalysisTask learningEngineAnalysisTask = LearningEngineAnalysisTask.builder()
                                                                .state_execution_id(stateExecutionId)
                                                                .workflow_execution_id(workflowExecutionId)
                                                                .executionStatus(ExecutionStatus.QUEUED)
                                                                .analysis_minute(0)
                                                                .build();
    learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask);

    learningEngineAnalysisTask =
        LearningEngineAnalysisTask.builder()
            .state_execution_id(stateExecutionId)
            .workflow_execution_id(workflowExecutionId)
            .executionStatus(ExecutionStatus.QUEUED)
            .analysis_minute((int) (TimeUnit.MILLISECONDS.toMinutes(TIME_SERIES_ANALYSIS_TASK_TIME_OUT)))
            .build();
    assertThat(learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask)).isFalse();
  }

  @Test
  @Category(UnitTests.class)
  public void testRetryExceeded() {
    LearningEngineAnalysisTask learningEngineAnalysisTask = LearningEngineAnalysisTask.builder()
                                                                .state_execution_id(stateExecutionId)
                                                                .workflow_execution_id(workflowExecutionId)
                                                                .executionStatus(ExecutionStatus.QUEUED)
                                                                .retry(LearningEngineAnalysisTask.RETRIES)
                                                                .build();
    wingsPersistence.updateField(LearningEngineAnalysisTask.class, learningEngineAnalysisTask.getUuid(), "retry",
        LearningEngineAnalysisTask.RETRIES);
    assertNull(learningEngineService.getNextLearningEngineAnalysisTask(
        ServiceApiVersion.V1, Optional.empty(), Optional.empty()));
  }

  @Test
  @Category(UnitTests.class)
  public void testQueueWithTimeOut() throws InterruptedException {
    LearningEngineAnalysisTask.TIME_SERIES_ANALYSIS_TASK_TIME_OUT = TimeUnit.SECONDS.toMillis(5);
    long startTime = System.currentTimeMillis();
    int numOfTasks = 10;
    for (int i = 0; i < numOfTasks; i++) {
      workflowExecutionId = UUID.randomUUID().toString();
      stateExecutionId = UUID.randomUUID().toString();
      LearningEngineAnalysisTask learningEngineAnalysisTask = LearningEngineAnalysisTask.builder()
                                                                  .state_execution_id(stateExecutionId)
                                                                  .workflow_execution_id(workflowExecutionId)
                                                                  .executionStatus(ExecutionStatus.RUNNING)
                                                                  .build();
      learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask);
    }
    Thread.sleep(TimeUnit.SECONDS.toMillis(10));

    assertThat(wingsPersistence.createQuery(LearningEngineAnalysisTask.class).count()).isEqualTo(numOfTasks);

    for (int i = 1; i <= numOfTasks; i++) {
      LearningEngineAnalysisTask analysisTask = learningEngineService.getNextLearningEngineAnalysisTask(
          ServiceApiVersion.V1, Optional.empty(), Optional.empty());
      assertThat(analysisTask.getExecutionStatus()).isEqualTo(ExecutionStatus.RUNNING);

      assertEquals(numOfTasks - i,
          wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
              .field(LearningEngineAnalysisTask.LAST_UPDATED_AT_KEY)
              .greaterThan(startTime)
              .filter("retry", 0)
              .asList()
              .size());
      assertEquals(i,
          wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
              .field(LearningEngineAnalysisTask.LAST_UPDATED_AT_KEY)
              .greaterThan(startTime)
              .filter("retry", 1)
              .asList()
              .size());
    }

    assertNull(learningEngineService.getNextLearningEngineAnalysisTask(
        ServiceApiVersion.V1, Optional.empty(), Optional.empty()));
  }

  @Test
  @Owner(emails = PARNIAN)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void testInitializeServiceSecretKeys() {
    assertThat(wingsPersistence.createQuery(ServiceSecretKey.class).asList().isEmpty()).isTrue();
    learningEngineService.initializeServiceSecretKeys();
    List<ServiceSecretKey> serviceSecretKeys =
        wingsPersistence.createQuery(ServiceSecretKey.class)
            .filter(ServiceSecretKeyKeys.serviceType, ServiceType.LEARNING_ENGINE)
            .asList();
    assertThat(serviceSecretKeys).hasSize(1);

    String secretKey = serviceSecretKeys.get(0).getServiceSecret();

    int numOfTries = 24;
    for (int i = 0; i < numOfTries; i++) {
      learningEngineService.initializeServiceSecretKeys();
    }

    serviceSecretKeys = wingsPersistence.createQuery(ServiceSecretKey.class)
                            .filter(ServiceSecretKeyKeys.serviceType, ServiceType.LEARNING_ENGINE)
                            .asList();
    assertThat(serviceSecretKeys).hasSize(1);

    assertThat(serviceSecretKeys.get(0).getServiceSecret()).isEqualTo(secretKey);
  }

  @Test
  @Category(UnitTests.class)
  public void testParseVersion() {
    ServiceApiVersion latestVersion = ServiceApiVersion.values()[ServiceApiVersion.values().length - 1];
    assertThat(Misc.parseApisVersion("application/json")).isEqualTo(latestVersion);

    for (ServiceApiVersion serviceApiVersion : ServiceApiVersion.values()) {
      String headerString = "application/" + serviceApiVersion.name().toLowerCase() + "+json, application/json";
      assertThat(Misc.parseApisVersion(headerString)).isEqualTo(serviceApiVersion);
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testUniqueIndexExperimentalTask() {
    assertThat(
        learningEngineService.addLearningEngineExperimentalAnalysisTask(LearningEngineExperimentalAnalysisTask.builder()
                                                                            .state_execution_id(stateExecutionId)
                                                                            .workflow_execution_id(workflowExecutionId)
                                                                            .stateType(StateType.ELK)
                                                                            .executionStatus(ExecutionStatus.QUEUED)
                                                                            .analysis_minute(14)
                                                                            .build()))
        .isTrue();

    assertThat(
        learningEngineService.addLearningEngineExperimentalAnalysisTask(LearningEngineExperimentalAnalysisTask.builder()
                                                                            .state_execution_id(stateExecutionId)
                                                                            .workflow_execution_id(workflowExecutionId)
                                                                            .stateType(StateType.ELK)
                                                                            .executionStatus(ExecutionStatus.QUEUED)
                                                                            .analysis_minute(14)
                                                                            .build()))
        .isFalse();
  }

  @Test
  @Category(UnitTests.class)
  public void testCV247TaskQueue() {
    NewRelicCVServiceConfiguration cvServiceConfiguration =
        NewRelicCVServiceConfiguration.builder().applicationId(generateUUID()).build();
    cvServiceConfiguration.setName(generateUUID());
    cvServiceConfiguration.setConnectorId(generateUUID());
    cvServiceConfiguration.setEnvId(generateUUID());
    cvServiceConfiguration.setServiceId(generateUUID());
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.LOW);
    cvServiceConfiguration.setStateType(StateType.NEW_RELIC);
    String cvConfigId = generateUuid();
    cvServiceConfiguration.setUuid(cvConfigId);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setAppId(appId);
    wingsPersistence.save(cvServiceConfiguration);

    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());

    // create data record
    wingsPersistence.save(NewRelicMetricDataRecord.builder()
                              .appId(appId)
                              .cvConfigId(cvConfigId)
                              .dataCollectionMinute((int) currentMinute)
                              .build());

    // save analysis record
    int numOfUnitsToBeAnalyized = 23;
    TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord = TimeSeriesMLAnalysisRecord.builder().build();
    timeSeriesMLAnalysisRecord.setAppId(appId);
    timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
    timeSeriesMLAnalysisRecord.setAnalysisMinute(
        (int) (currentMinute - CRON_POLL_INTERVAL_IN_MINUTES * numOfUnitsToBeAnalyized));
    wingsPersistence.save(timeSeriesMLAnalysisRecord);

    when(managerClientHelper.callManagerWithRetry(any())).thenReturn(aRestResponse().withResource(false).build());

    continuousVerificationService.triggerServiceGuardTimeSeriesAnalysis(accountId);

    List<LearningEngineAnalysisTask> analysisTasks = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                                         .filter("appId", this.appId)
                                                         .order("-analysis_minute")
                                                         .asList();
    assertThat(analysisTasks).hasSize(1);
    LearningEngineAnalysisTask analysisTask = analysisTasks.get(0);
    // this is beyond 2 hours, so analysis start minute should be currentTime - 2 hours. analysisMin should be
    // currentTime - 1h45m
    assertEquals(
        currentMinute - PREDECTIVE_HISTORY_MINUTES + CRON_POLL_INTERVAL_IN_MINUTES, analysisTask.getAnalysis_minute());
    assertThat(analysisTask.getAnalysis_start_min()).isEqualTo(currentMinute - PREDECTIVE_HISTORY_MINUTES * 2 + 1);
    assertEquals(
        analysisTask.getAnalysis_start_min() + PREDECTIVE_HISTORY_MINUTES - 1, analysisTask.getPrediction_start_time());

    continuousVerificationService.triggerServiceGuardTimeSeriesAnalysis(accountId);
    analysisTasks = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                        .filter("appId", this.appId)
                        .order("-analysis_minute")
                        .asList();
    assertThat(analysisTasks).hasSize(1);

    // there has been no new analysis saved since the previous trigger, so no new task should be created
    assertEquals(
        currentMinute - PREDECTIVE_HISTORY_MINUTES + CRON_POLL_INTERVAL_IN_MINUTES, analysisTask.getAnalysis_minute());
    assertThat(analysisTask.getAnalysis_start_min()).isEqualTo(currentMinute - PREDECTIVE_HISTORY_MINUTES * 2 + 1);
    assertEquals(
        analysisTask.getAnalysis_start_min() + PREDECTIVE_HISTORY_MINUTES - 1, analysisTask.getPrediction_start_time());

    timeSeriesMLAnalysisRecord = TimeSeriesMLAnalysisRecord.builder().build();
    timeSeriesMLAnalysisRecord.setAppId(appId);
    timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
    timeSeriesMLAnalysisRecord.setAnalysisMinute((int) analysisTask.getAnalysis_minute());
    wingsPersistence.save(timeSeriesMLAnalysisRecord);
    analysisTask.setExecutionStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(analysisTask);

    continuousVerificationService.triggerServiceGuardTimeSeriesAnalysis(accountId);

    analysisTasks = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                        .filter("appId", this.appId)
                        .order("-analysis_minute")
                        .asList();
    assertThat(analysisTasks).hasSize(2);

    analysisTask = analysisTasks.get(0);
    assertEquals(timeSeriesMLAnalysisRecord.getAnalysisMinute() + CRON_POLL_INTERVAL_IN_MINUTES,
        analysisTask.getAnalysis_minute());
    assertEquals(analysisTask.getAnalysis_minute() - PREDECTIVE_HISTORY_MINUTES - CRON_POLL_INTERVAL_IN_MINUTES + 1,
        analysisTask.getAnalysis_start_min());
    assertEquals(
        analysisTask.getAnalysis_minute() - CRON_POLL_INTERVAL_IN_MINUTES, analysisTask.getPrediction_start_time());
  }

  @Test
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
  @Category(UnitTests.class)
  public void testNotifyFailure() {
    workflowExecutionId = UUID.randomUUID().toString();
    stateExecutionId = UUID.randomUUID().toString();
    LearningEngineAnalysisTask learningEngineAnalysisTask = LearningEngineAnalysisTask.builder()
                                                                .state_execution_id(stateExecutionId)
                                                                .workflow_execution_id(workflowExecutionId)
                                                                .executionStatus(ExecutionStatus.QUEUED)
                                                                .build();
    learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask);
    wingsPersistence.save(AnalysisContext.builder().stateExecutionId(stateExecutionId).build());

    assertEquals(1,
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.executionStatus, ExecutionStatus.QUEUED)
            .count());

    for (int i = 0; i < LearningEngineAnalysisTask.RETRIES - 1; i++) {
      LearningEngineAnalysisTask leTask = learningEngineService.getNextLearningEngineAnalysisTask(
          ServiceApiVersion.V1, Optional.of(false), Optional.empty());
      assertThat(leTask.getExecutionStatus()).isEqualTo(ExecutionStatus.RUNNING);

      assertEquals(0,
          wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
              .filter(LearningEngineAnalysisTaskKeys.executionStatus, ExecutionStatus.QUEUED)
              .count());

      assertEquals(1,
          wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
              .filter(LearningEngineAnalysisTaskKeys.executionStatus, ExecutionStatus.RUNNING)
              .count());

      learningEngineService.notifyFailure(leTask.getUuid(), LearningEngineError.builder().build());
      assertEquals(1,
          wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
              .filter(LearningEngineAnalysisTaskKeys.executionStatus, ExecutionStatus.QUEUED)
              .count());

      assertEquals(0,
          wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
              .filter(LearningEngineAnalysisTaskKeys.executionStatus, ExecutionStatus.RUNNING)
              .count());
    }

    LearningEngineAnalysisTask leTask = learningEngineService.getNextLearningEngineAnalysisTask(
        ServiceApiVersion.V1, Optional.of(false), Optional.empty());
    assertThat(leTask.getExecutionStatus()).isEqualTo(ExecutionStatus.RUNNING);

    assertEquals(0,
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.executionStatus, ExecutionStatus.QUEUED)
            .count());

    assertEquals(1,
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.executionStatus, ExecutionStatus.RUNNING)
            .count());

    learningEngineService.notifyFailure(leTask.getUuid(), LearningEngineError.builder().build());
    assertEquals(0,
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.executionStatus, ExecutionStatus.QUEUED)
            .count());

    assertEquals(0,
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.executionStatus, ExecutionStatus.RUNNING)
            .count());

    assertEquals(1,
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.executionStatus, ExecutionStatus.FAILED)
            .count());

    assertNull(learningEngineService.getNextLearningEngineAnalysisTask(
        ServiceApiVersion.V1, Optional.of(false), Optional.empty()));
  }
}
