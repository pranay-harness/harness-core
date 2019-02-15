package io.harness.apm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rest.RestResponse.Builder.aRestResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;

import io.harness.VerificationBaseTest;
import io.harness.event.usagemetrics.UsageMetricsHelper;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.Application;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
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
import java.util.UUID;
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
  private String delegateTaskId;
  @Mock private VerificationManagerClientHelper managerClientHelper;
  @Mock private UsageMetricsHelper usageMetricsHelper;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private TimeSeriesAnalysisService metricDataAnalysisService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(managerClientHelper.callManagerWithRetry(any())).thenReturn(aRestResponse().withResource(false).build());
    when(usageMetricsHelper.getCVConfig(anyString())).thenReturn(mockCVConfig());
    when(usageMetricsHelper.getApplication(anyString())).thenReturn(mockApplication());
    setInternalState(metricDataAnalysisService, "managerClientHelper", managerClientHelper);
    setInternalState(metricDataAnalysisService, "usageMetricsHelper", usageMetricsHelper);
    accountId = wingsPersistence.save(anAccount().withAccountName(generateUuid()).build());
    appId = wingsPersistence.save(anApplication().withName(generateUuid()).withAccountId(accountId).build());
    stateExecutionId = generateUuid();
    workflowExecutionId = generateUuid();
    serviceId = generateUuid();
    cvConfigId = generateUuid();
    groupName = "groupName-";
    delegateTaskId = UUID.randomUUID().toString();
  }

  @Test
  public void testSaveAnalysisRecordsML() {
    int numOfGroups = 5;
    int numOfMinutes = 10;
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
            workflowExecutionId, groupName + i, j, delegateTaskId, "-1", cvConfigId, mlAnalysisResponse);
      }
    }

    assertEquals(numOfGroups * numOfMinutes,
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
            .filter("stateExecutionId", stateExecutionId)
            .filter("appId", appId)
            .asList()
            .size());
    List<NewRelicMetricAnalysisRecord> resultList =
        metricDataAnalysisService.getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId);

    assertNotNull(resultList);
    assertEquals(numOfGroups, resultList.size());
    resultList.forEach(record -> assertEquals(numOfMinutes, record.getAnalysisMinute()));
  }

  @Test
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

      TimeSeriesMLAnalysisRecord red = wingsPersistence.saveAndGet(TimeSeriesMLAnalysisRecord.class, testRecord);
      sampleMinute = sampleMinute - (int) TimeUnit.DAYS.toMinutes(7);
      testRecord = TimeSeriesMLAnalysisRecord.builder().build();
    }

    // test behavior
    List<TimeSeriesMLAnalysisRecord> historicalRecords =
        metricDataAnalysisService.getHistoricalAnalysis(accountId, appId, serviceId, cvConfigId, 1000000);

    // verify
    assertNotNull(historicalRecords);
    assertEquals("Historical list should be of size 4", 4, historicalRecords.size());
  }

  @Test
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

      TimeSeriesMLAnalysisRecord red = wingsPersistence.saveAndGet(TimeSeriesMLAnalysisRecord.class, testRecord);
      sampleMinute = sampleMinute - (int) TimeUnit.DAYS.toMinutes(7);
      testRecord = TimeSeriesMLAnalysisRecord.builder().build();
    }

    // test behavior
    List<TimeSeriesMLAnalysisRecord> historicalRecords =
        metricDataAnalysisService.getHistoricalAnalysis(accountId, appId, serviceId, cvConfigId, 1000);

    // verify
    assertNotNull(historicalRecords);
    assertEquals("Historical list should be of size 0", 0, historicalRecords.size());
  }

  @Test
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

      TimeSeriesMLAnalysisRecord red = wingsPersistence.saveAndGet(TimeSeriesMLAnalysisRecord.class, testRecord);
      sampleMinute = sampleMinute - (int) TimeUnit.DAYS.toMinutes(7);
      testRecord = TimeSeriesMLAnalysisRecord.builder().build();
    }

    // test behavior
    List<TimeSeriesMLAnalysisRecord> historicalRecordsBadAppId =
        metricDataAnalysisService.getHistoricalAnalysis(accountId, appId + "-bad", serviceId, cvConfigId, 1000000);

    List<TimeSeriesMLAnalysisRecord> historicalRecordsBadCvConfigId =
        metricDataAnalysisService.getHistoricalAnalysis(accountId, appId, serviceId, cvConfigId + "-bad", 1000000);

    // verify
    assertNotNull(historicalRecordsBadAppId);
    assertEquals("Historical list should be of size 0", 0, historicalRecordsBadAppId.size());
    assertNotNull(historicalRecordsBadCvConfigId);
    assertEquals("Historical list should be of size 0", 0, historicalRecordsBadCvConfigId.size());
  }

  @Test
  public void testCompression() throws IOException {
    long analysisMinute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    String cvConfigId = generateUuid();
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
    assertFalse(isEmpty(timeSeriesMLAnalysisRecord.getTransactions()));
    assertNull(timeSeriesMLAnalysisRecord.getTransactionsCompressedJson());
    metricDataAnalysisService.saveAnalysisRecordsML(accountId, StateType.APP_DYNAMICS, appId, stateExecutionId,
        workflowExecutionId, generateUuid(), (int) analysisMinute, generateUuid(), generateUuid(), cvConfigId,
        timeSeriesMLAnalysisRecord);

    final TimeSeriesMLAnalysisRecord savedRecord = wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
                                                       .filter("appId", appId)
                                                       .filter("stateExecutionId", stateExecutionId)
                                                       .get();
    assertNull(savedRecord.getTransactions());
    assertFalse(isEmpty(timeSeriesMLAnalysisRecord.getTransactionsCompressedJson()));
    TimeSeriesMLAnalysisRecord readRecord =
        metricDataAnalysisService.getPreviousAnalysis(appId, cvConfigId, analysisMinute);
    assertFalse(isEmpty(readRecord.getTransactions()));
    assertNull(readRecord.getTransactionsCompressedJson());
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
