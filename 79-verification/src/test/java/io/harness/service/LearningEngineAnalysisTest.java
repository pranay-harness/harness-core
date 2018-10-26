package io.harness.service;

import static io.harness.persistence.HQuery.excludeAuthority;
import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;
import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.PREDECTIVE_HISTORY_MINUTES;
import static software.wings.service.impl.newrelic.LearningEngineAnalysisTask.TIME_SERIES_ANALYSIS_TASK_TIME_OUT;

import com.google.inject.Inject;

import io.harness.VerificationBaseTest;
import io.harness.service.intfc.ContinuousVerificationService;
import io.harness.service.intfc.LearningEngineService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.ServiceSecretKey;
import software.wings.beans.ServiceSecretKey.ServiceApiVersion;
import software.wings.beans.ServiceSecretKey.ServiceType;
import software.wings.dl.WingsPersistence;
import software.wings.resources.CVConfigurationResource;
import software.wings.security.encryption.EncryptionUtils;
import software.wings.service.impl.LicenseUtil;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.utils.Misc;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 1/9/18.
 */
public class LearningEngineAnalysisTest extends VerificationBaseTest {
  @Inject private LearningEngineService learningEngineService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private CVConfigurationResource cvConfigurationResource;
  @Inject private ContinuousVerificationService continuousVerificationService;

  private String accountId;
  private String appId;
  private String workflowExecutionId;
  private String stateExecutionId;

  @Before
  public void setup() {
    Account account = anAccount().withAccountName(generateUUID()).build();

    account.setEncryptedLicenseInfo(
        EncryptionUtils.encrypt(LicenseUtil.convertToString(LicenseInfo.builder().accountType(AccountType.PAID).build())
                                    .getBytes(Charset.forName("UTF-8")),
            null));
    accountId = wingsPersistence.save(account);
    appId = wingsPersistence.save(anApplication().withAccountId(accountId).withName(generateUUID()).build());
    workflowExecutionId = generateUUID();
    stateExecutionId = generateUUID();
  }

  @After
  public void tearDown() {
    LearningEngineAnalysisTask.TIME_SERIES_ANALYSIS_TASK_TIME_OUT = TimeUnit.MINUTES.toMillis(2);
  }

  @Test
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

    assertEquals(numOfTasks, wingsPersistence.createQuery(LearningEngineAnalysisTask.class).count());

    for (int i = 1; i <= numOfTasks; i++) {
      LearningEngineAnalysisTask analysisTask =
          learningEngineService.getNextLearningEngineAnalysisTask(ServiceApiVersion.V1);
      assertEquals(ExecutionStatus.RUNNING, analysisTask.getExecutionStatus());

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

    assertNull(learningEngineService.getNextLearningEngineAnalysisTask(ServiceApiVersion.V1));
  }

  @Test
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
    assertEquals(1, learningEngineAnalysisTasks.size());
    LearningEngineAnalysisTask analysisTask = learningEngineAnalysisTasks.get(0);
    assertEquals(workflowExecutionId, analysisTask.getWorkflow_execution_id());
    assertEquals(stateExecutionId, analysisTask.getState_execution_id());
    assertEquals(0, analysisTask.getAnalysis_minute());
    assertEquals(0, analysisTask.getRetry());
  }

  @Test
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
    assertFalse(learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask));
  }

  @Test
  public void testRetryExceeded() {
    LearningEngineAnalysisTask learningEngineAnalysisTask = LearningEngineAnalysisTask.builder()
                                                                .state_execution_id(stateExecutionId)
                                                                .workflow_execution_id(workflowExecutionId)
                                                                .executionStatus(ExecutionStatus.QUEUED)
                                                                .retry(LearningEngineAnalysisTask.RETRIES)
                                                                .build();
    wingsPersistence.updateField(LearningEngineAnalysisTask.class, learningEngineAnalysisTask.getUuid(), "retry",
        LearningEngineAnalysisTask.RETRIES);
    assertNull(learningEngineService.getNextLearningEngineAnalysisTask(ServiceApiVersion.V1));
  }

  @Test
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

    assertEquals(numOfTasks, wingsPersistence.createQuery(LearningEngineAnalysisTask.class).count());

    for (int i = 1; i <= numOfTasks; i++) {
      LearningEngineAnalysisTask analysisTask =
          learningEngineService.getNextLearningEngineAnalysisTask(ServiceApiVersion.V1);
      assertEquals(ExecutionStatus.RUNNING, analysisTask.getExecutionStatus());

      assertEquals(numOfTasks - i,
          wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
              .field("lastUpdatedAt")
              .greaterThan(startTime)
              .filter("retry", 0)
              .asList()
              .size());
      assertEquals(i,
          wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
              .field("lastUpdatedAt")
              .greaterThan(startTime)
              .filter("retry", 1)
              .asList()
              .size());
    }

    assertNull(learningEngineService.getNextLearningEngineAnalysisTask(ServiceApiVersion.V1));
  }

  @Test
  @Ignore
  public void testInitializeServiceSecretKeys() {
    assertTrue(wingsPersistence.createQuery(ServiceSecretKey.class).asList().isEmpty());
    learningEngineService.initializeServiceSecretKeys();
    List<ServiceSecretKey> serviceSecretKeys = wingsPersistence.createQuery(ServiceSecretKey.class)
                                                   .filter("serviceType", ServiceType.LEARNING_ENGINE)
                                                   .asList();
    assertEquals(1, serviceSecretKeys.size());

    String secretKey = serviceSecretKeys.get(0).getServiceSecret();

    int numOfTries = 24;
    for (int i = 0; i < numOfTries; i++) {
      learningEngineService.initializeServiceSecretKeys();
    }

    serviceSecretKeys = wingsPersistence.createQuery(ServiceSecretKey.class)
                            .filter("serviceType", ServiceType.LEARNING_ENGINE)
                            .asList();
    assertEquals(1, serviceSecretKeys.size());

    assertEquals(secretKey, serviceSecretKeys.get(0).getServiceSecret());
  }

  @Test
  public void testParseVersion() {
    ServiceApiVersion latestVersion = ServiceApiVersion.values()[ServiceApiVersion.values().length - 1];
    assertEquals(latestVersion, Misc.parseApisVersion("application/json"));

    for (ServiceApiVersion serviceApiVersion : ServiceApiVersion.values()) {
      String headerString = "application/" + serviceApiVersion.name().toLowerCase() + "+json, application/json";
      assertEquals(serviceApiVersion, Misc.parseApisVersion(headerString));
    }
  }

  @Test
  public void testUniqueIndexExperimentalTask() {
    assertTrue(
        learningEngineService.addLearningEngineExperimentalAnalysisTask(LearningEngineExperimentalAnalysisTask.builder()
                                                                            .state_execution_id(stateExecutionId)
                                                                            .workflow_execution_id(workflowExecutionId)
                                                                            .stateType(StateType.ELK)
                                                                            .executionStatus(ExecutionStatus.QUEUED)
                                                                            .analysis_minute(14)
                                                                            .build()));

    assertFalse(
        learningEngineService.addLearningEngineExperimentalAnalysisTask(LearningEngineExperimentalAnalysisTask.builder()
                                                                            .state_execution_id(stateExecutionId)
                                                                            .workflow_execution_id(workflowExecutionId)
                                                                            .stateType(StateType.ELK)
                                                                            .executionStatus(ExecutionStatus.QUEUED)
                                                                            .analysis_minute(14)
                                                                            .build()));
  }

  @Test
  public void testCV247TaskQueue() {
    NewRelicCVServiceConfiguration cvServiceConfiguration =
        NewRelicCVServiceConfiguration.builder().applicationId(generateUUID()).build();
    cvServiceConfiguration.setName(generateUUID());
    cvServiceConfiguration.setConnectorId(generateUUID());
    cvServiceConfiguration.setEnvId(generateUUID());
    cvServiceConfiguration.setServiceId(generateUUID());
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.LOW);
    String cvConfigId =
        cvConfigurationResource.saveCVConfiguration(accountId, appId, StateType.NEW_RELIC, cvServiceConfiguration)
            .getResource();

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

    continuousVerificationService.triggerDataAnalysis(accountId);

    List<LearningEngineAnalysisTask> analysisTasks = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                                                         .filter("appId", this.appId)
                                                         .order("-analysis_minute")
                                                         .asList();
    assertEquals(numOfUnitsToBeAnalyized, analysisTasks.size());
    for (int i = 0; i < numOfUnitsToBeAnalyized; i++) {
      LearningEngineAnalysisTask analysisTask = analysisTasks.get(i);
      assertEquals(currentMinute - i * CRON_POLL_INTERVAL_IN_MINUTES, analysisTask.getAnalysis_minute());
      assertEquals(analysisTask.getAnalysis_minute() - PREDECTIVE_HISTORY_MINUTES - CRON_POLL_INTERVAL_IN_MINUTES + 1,
          analysisTask.getAnalysis_start_min());
      assertEquals(analysisTask.getAnalysis_minute() - CRON_POLL_INTERVAL_IN_MINUTES + 1,
          analysisTask.getPrediction_start_time());
    }

    continuousVerificationService.triggerDataAnalysis(accountId);
    analysisTasks = wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
                        .filter("appId", this.appId)
                        .order("-analysis_minute")
                        .asList();
    assertEquals(numOfUnitsToBeAnalyized, analysisTasks.size());

    for (int i = 0; i < numOfUnitsToBeAnalyized; i++) {
      LearningEngineAnalysisTask analysisTask = analysisTasks.get(i);
      assertEquals(currentMinute - i * CRON_POLL_INTERVAL_IN_MINUTES, analysisTask.getAnalysis_minute());
      assertEquals(analysisTask.getAnalysis_minute() - PREDECTIVE_HISTORY_MINUTES - CRON_POLL_INTERVAL_IN_MINUTES + 1,
          analysisTask.getAnalysis_start_min());
      assertEquals(analysisTask.getAnalysis_minute() - CRON_POLL_INTERVAL_IN_MINUTES + 1,
          analysisTask.getPrediction_start_time());
    }
  }
}
