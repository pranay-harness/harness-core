package io.harness.apm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rest.RestResponse.Builder.aRestResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;

import io.harness.VerificationBaseTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.event.usagemetrics.UsageMetricsHelper;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.service.intfc.ContinuousVerificationService;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.Application;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ExperimentalLogMLAnalysisRecord;
import software.wings.service.impl.analysis.MetricDataAnalysisServiceImpl;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLScores;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 9/7/18.
 */
public class MetricDataAnalysisServiceTest extends VerificationBaseTest {
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowExecutionId;
  private String serviceId;
  private String cvConfigId;
  private String groupName;
  @Mock private VerificationManagerClientHelper managerClientHelper;
  @Mock private UsageMetricsHelper usageMetricsHelper;

  @Inject private WingsPersistence wingsPersistence;
  @Inject @InjectMocks private TimeSeriesAnalysisService metricDataAnalysisService;
  @Inject @InjectMocks private ContinuousVerificationService continuousVerificationService;
  @Inject @InjectMocks private LearningEngineService learningEngineService;
  private MetricDataAnalysisService managerAnalysisService;

  @Before
  public void setUp() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    when(managerClientHelper.callManagerWithRetry(any())).thenReturn(aRestResponse().withResource(false).build());
    when(usageMetricsHelper.getCVConfig(anyString())).thenReturn(mockCVConfig());
    when(usageMetricsHelper.getApplication(anyString())).thenReturn(mockApplication());
    accountId = wingsPersistence.save(anAccount().withAccountName(generateUuid()).build());
    appId = wingsPersistence.save(anApplication().name(generateUuid()).accountId(accountId).build());
    stateExecutionId = generateUuid();
    workflowExecutionId = generateUuid();
    serviceId = generateUuid();
    cvConfigId = wingsPersistence.save(new CVConfiguration());
    groupName = "groupName-";
    managerAnalysisService = new MetricDataAnalysisServiceImpl();
    FieldUtils.writeField(managerAnalysisService, "wingsPersistence", wingsPersistence, true);
    FieldUtils.writeField(
        metricDataAnalysisService, "continuousVerificationService", continuousVerificationService, true);
  }

  @Test
  @Category(UnitTests.class)
  public void testSaveAnalysisRecordsML() {
    int numOfGroups = 5;
    int numOfMinutes = 10;
    LearningEngineAnalysisTask learningEngineAnalysisTask = getLearningEngineAnalysisTask();
    for (int i = 0; i < numOfGroups; i++) {
      for (int j = 1; j <= numOfMinutes; j++) {
        TimeSeriesMLAnalysisRecord mlAnalysisResponse = TimeSeriesMLAnalysisRecord.builder().build();
        mlAnalysisResponse.setCreatedAt(j);
        mlAnalysisResponse.setTransactions(Collections.EMPTY_MAP);
        mlAnalysisResponse.setOverallMetricScores(new HashMap<String, Double>() {
          {
            put("key1", 0.76);
            put("key2", 0.5);
          }
        });
        metricDataAnalysisService.saveAnalysisRecordsML(accountId, StateType.DYNA_TRACE, appId, stateExecutionId,
            workflowExecutionId, groupName + i, j, learningEngineAnalysisTask.getUuid(), "-1", cvConfigId,
            mlAnalysisResponse, null);
      }
    }

    assertEquals(numOfGroups * numOfMinutes,
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
            .filter("stateExecutionId", stateExecutionId)
            .filter("appId", appId)
            .asList()
            .size());
    Set<NewRelicMetricAnalysisRecord> resultList =
        managerAnalysisService.getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId);

    assertThat(resultList).isNotNull();
    assertEquals(numOfGroups, resultList.size());
    resultList.forEach(record -> assertEquals(numOfMinutes, record.getAnalysisMinute()));
  }

  private LearningEngineAnalysisTask getLearningEngineAnalysisTask() {
    LearningEngineAnalysisTask learningEngineAnalysisTask = LearningEngineAnalysisTask.builder()
                                                                .state_execution_id(stateExecutionId)
                                                                .workflow_execution_id(workflowExecutionId)
                                                                .executionStatus(ExecutionStatus.QUEUED)
                                                                .build();
    learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask);
    return learningEngineAnalysisTask;
  }

  @Test
  @Category(UnitTests.class)
  public void testGetHistoricalAnalysis() {
    // setup
    int sampleMinute = 1000000;
    TimeSeriesMLAnalysisRecord testRecord = TimeSeriesMLAnalysisRecord.builder().build();
    testRecord.setAnalysisMinute(sampleMinute);

    for (int j = 1; j <= 5; j++) {
      testRecord.setCreatedAt(j);
      testRecord.setTransactions(Collections.EMPTY_MAP);
      testRecord.setAppId(appId);
      testRecord.setCvConfigId(cvConfigId);
      testRecord.setAnalysisMinute(sampleMinute);

      wingsPersistence.save(testRecord);
      sampleMinute = sampleMinute - (int) TimeUnit.DAYS.toMinutes(7);
      testRecord = TimeSeriesMLAnalysisRecord.builder().build();
    }

    // test behavior
    List<TimeSeriesMLAnalysisRecord> historicalRecords =
        metricDataAnalysisService.getHistoricalAnalysis(accountId, appId, serviceId, cvConfigId, 1000000, null);

    // verify
    assertThat(historicalRecords).isNotNull();
    assertEquals("Historical list should be of size 4", 4, historicalRecords.size());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetHistoricalAnalysisNoHistorical() {
    // setup
    int sampleMinute = 1000000;
    TimeSeriesMLAnalysisRecord testRecord = TimeSeriesMLAnalysisRecord.builder().build();
    testRecord.setAnalysisMinute(sampleMinute);

    for (int j = 1; j <= 5; j++) {
      testRecord.setCreatedAt(j);
      testRecord.setTransactions(Collections.EMPTY_MAP);
      testRecord.setAppId(appId);
      testRecord.setCvConfigId(cvConfigId);
      testRecord.setAnalysisMinute(sampleMinute);

      wingsPersistence.save(testRecord);
      sampleMinute = sampleMinute - (int) TimeUnit.DAYS.toMinutes(7);
      testRecord = TimeSeriesMLAnalysisRecord.builder().build();
    }

    // test behavior
    List<TimeSeriesMLAnalysisRecord> historicalRecords =
        metricDataAnalysisService.getHistoricalAnalysis(accountId, appId, serviceId, cvConfigId, 1000, null);

    // verify
    assertThat(historicalRecords).isNotNull();
    assertEquals("Historical list should be of size 0", 0, historicalRecords.size());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetHistoricalAnalysisBadAppIdCvConfigId() {
    // setup
    int sampleMinute = 1000000;
    TimeSeriesMLAnalysisRecord testRecord = TimeSeriesMLAnalysisRecord.builder().build();
    testRecord.setAnalysisMinute(sampleMinute);

    for (int j = 1; j <= 5; j++) {
      testRecord.setCreatedAt(j);
      testRecord.setTransactions(Collections.EMPTY_MAP);
      testRecord.setAppId(appId);
      testRecord.setCvConfigId(cvConfigId);
      testRecord.setAnalysisMinute(sampleMinute);

      wingsPersistence.save(testRecord);
      sampleMinute = sampleMinute - (int) TimeUnit.DAYS.toMinutes(7);
      testRecord = TimeSeriesMLAnalysisRecord.builder().build();
    }

    List<TimeSeriesMLAnalysisRecord> historicalRecordsBadCvConfigId = metricDataAnalysisService.getHistoricalAnalysis(
        accountId, appId, serviceId, cvConfigId + "-bad", 1000000, null);

    // verify
    assertThat(historicalRecordsBadCvConfigId).isNotNull();
    assertEquals("Historical list should be of size 0", 0, historicalRecordsBadCvConfigId.size());
  }

  @Test
  @Category(UnitTests.class)
  public void testCompression() throws IOException {
    long analysisMinute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    final CVConfiguration cvConfiguration = new CVConfiguration();
    cvConfiguration.setAppId(appId);
    String cvConfigId = wingsPersistence.save(cvConfiguration);
    LearningEngineAnalysisTask learningEngineAnalysisTask = getLearningEngineAnalysisTask();
    final Gson gson = new Gson();
    File file = new File(getClass().getClassLoader().getResource("./ts_analysis_record.json").getFile());
    TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord;
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<TimeSeriesMLAnalysisRecord>() {}.getType();
      timeSeriesMLAnalysisRecord = gson.fromJson(br, type);
      timeSeriesMLAnalysisRecord.setAppId(appId);
      timeSeriesMLAnalysisRecord.setCvConfigId(generateUuid());
      timeSeriesMLAnalysisRecord.setAnalysisMinute((int) analysisMinute);
      timeSeriesMLAnalysisRecord.setOverallMetricScores(new HashMap<String, Double>() {
        {
          put("key1", 0.76);
          put("key2", 0.5);
        }
      });
    }
    assertThat(isEmpty(timeSeriesMLAnalysisRecord.getTransactions())).isFalse();
    assertThat(timeSeriesMLAnalysisRecord.getTransactionsCompressedJson()).isNull();
    metricDataAnalysisService.saveAnalysisRecordsML(accountId, StateType.APP_DYNAMICS, appId, stateExecutionId,
        workflowExecutionId, generateUuid(), (int) analysisMinute, learningEngineAnalysisTask.getUuid(), generateUuid(),
        cvConfigId, timeSeriesMLAnalysisRecord, null);

    final TimeSeriesMLAnalysisRecord savedRecord = wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
                                                       .filter("appId", appId)
                                                       .filter("stateExecutionId", stateExecutionId)
                                                       .get();
    assertThat(savedRecord.getTransactions()).isNull();
    assertThat(isEmpty(timeSeriesMLAnalysisRecord.getTransactionsCompressedJson())).isFalse();
    TimeSeriesMLAnalysisRecord readRecord =
        metricDataAnalysisService.getPreviousAnalysis(appId, cvConfigId, analysisMinute, null);
    assertThat(isEmpty(readRecord.getTransactions())).isFalse();
    assertThat(readRecord.getTransactionsCompressedJson()).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testCleanup() {
    int numOfRecords = 10;
    for (int i = 0; i < numOfRecords; i++) {
      wingsPersistence.save(
          TimeSeriesMetricTemplates.builder().stateExecutionId(stateExecutionId).cvConfigId("cv" + i).build());
      NewRelicMetricDataRecord newRelicMetricDataRecord = new NewRelicMetricDataRecord();
      newRelicMetricDataRecord.setStateExecutionId(stateExecutionId);
      newRelicMetricDataRecord.setAppId(appId);
      newRelicMetricDataRecord.setDataCollectionMinute(i);
      wingsPersistence.save(newRelicMetricDataRecord);
      wingsPersistence.save(NewRelicMetricAnalysisRecord.builder()
                                .stateExecutionId(stateExecutionId)
                                .analysisMinute(i)
                                .appId(appId)
                                .build());
      TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord = TimeSeriesMLAnalysisRecord.builder().build();
      timeSeriesMLAnalysisRecord.setStateExecutionId(stateExecutionId);
      timeSeriesMLAnalysisRecord.setAnalysisMinute(i);
      wingsPersistence.save(timeSeriesMLAnalysisRecord);
      wingsPersistence.save(TimeSeriesMLScores.builder().stateExecutionId(stateExecutionId).analysisMinute(i).build());
      wingsPersistence.save(
          ContinuousVerificationExecutionMetaData.builder().stateExecutionId(stateExecutionId).build());
      wingsPersistence.save(
          LearningEngineAnalysisTask.builder().state_execution_id(stateExecutionId).analysis_minute(i).build());
      wingsPersistence.save(LearningEngineExperimentalAnalysisTask.builder()
                                .state_execution_id(stateExecutionId)
                                .analysis_minute(i)
                                .build());
      ExperimentalLogMLAnalysisRecord experimentalLogMLAnalysisRecord = new ExperimentalLogMLAnalysisRecord();
      experimentalLogMLAnalysisRecord.setStateExecutionId(stateExecutionId);
      experimentalLogMLAnalysisRecord.setLogCollectionMinute(i);
      wingsPersistence.save(experimentalLogMLAnalysisRecord);

      wingsPersistence.save(
          TimeSeriesMetricGroup.builder().stateExecutionId(stateExecutionId).stateType(StateType.values()[i]).build());
      wingsPersistence.save(
          AnalysisContext.builder().stateExecutionId(stateExecutionId).serviceId("service-" + i).build());
    }

    assertEquals(numOfRecords, wingsPersistence.createQuery(TimeSeriesMetricTemplates.class).count());
    assertEquals(numOfRecords, wingsPersistence.createQuery(NewRelicMetricDataRecord.class).count());
    assertEquals(numOfRecords, wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class).count());
    assertEquals(numOfRecords, wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class).count());
    assertEquals(numOfRecords, wingsPersistence.createQuery(TimeSeriesMLScores.class).count());
    assertEquals(numOfRecords, wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class).count());
    assertEquals(numOfRecords, wingsPersistence.createQuery(LearningEngineAnalysisTask.class).count());
    assertEquals(numOfRecords, wingsPersistence.createQuery(TimeSeriesMetricGroup.class).count());
    assertEquals(numOfRecords, wingsPersistence.createQuery(AnalysisContext.class).count());

    managerAnalysisService.cleanUpForMetricRetry(stateExecutionId);
    assertEquals(0, wingsPersistence.createQuery(TimeSeriesMetricTemplates.class).count());
    assertEquals(0, wingsPersistence.createQuery(NewRelicMetricDataRecord.class).count());
    assertEquals(0, wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class).count());
    assertEquals(0, wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class).count());
    assertEquals(0, wingsPersistence.createQuery(TimeSeriesMLScores.class).count());
    assertEquals(0, wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class).count());
    assertEquals(0, wingsPersistence.createQuery(LearningEngineAnalysisTask.class).count());
    assertEquals(0, wingsPersistence.createQuery(TimeSeriesMetricGroup.class).count());
    assertEquals(0, wingsPersistence.createQuery(AnalysisContext.class).count());
  }

  private Application mockApplication() {
    Application app = new Application();
    app.setUuid(APP_ID);
    app.setName(APP_NAME);
    return app;
  }

  private CVConfiguration mockCVConfig() {
    AppDynamicsCVServiceConfiguration config =
        AppDynamicsCVServiceConfiguration.builder().appDynamicsApplicationId("1234").tierId("5678").build();
    config.setConnectorId("connectorId");
    config.setName("test Config");
    config.setUuid("cvConfigId");
    config.setAppId("appId");
    config.setServiceId("serviceId");
    return config;
  }
}
