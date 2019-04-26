package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;

import com.google.common.collect.Lists;
import com.google.common.collect.TreeBasedTable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rest.RestResponse;
import io.harness.rule.OwnerRule.Owner;
import io.harness.time.Timestamp;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.LicenseInfo;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.common.VerificationConstants;
import software.wings.dl.WingsPersistence;
import software.wings.resources.CVConfigurationResource;
import software.wings.resources.ContinuousVerificationDashboardResource;
import software.wings.security.encryption.EncryptionUtils;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.service.impl.analysis.TimeSeriesFilter;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLHostSummary;
import software.wings.service.impl.analysis.TimeSeriesMLMetricSummary;
import software.wings.service.impl.analysis.TimeSeriesMLTxnSummary;
import software.wings.service.impl.analysis.TimeSeriesRiskSummary;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.HeatMap;
import software.wings.verification.TimeSeriesDataPoint;
import software.wings.verification.TimeSeriesOfMetric;
import software.wings.verification.TimeSeriesRisk;
import software.wings.verification.TransactionTimeSeries;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Vaibhav Tulsyan
 * 19/Oct/2018
 */
@Slf4j
public class HeatMapApiUnitTest extends WingsBaseTest {
  //@Mock private SettingsService mockSettingsService;
  @Inject WingsPersistence wingsPersistence;
  @Inject ContinuousVerificationService continuousVerificationService;
  @Inject private CVConfigurationResource cvConfigurationResource;
  @Inject private ContinuousVerificationDashboardResource cvDashboardResource;

  private String accountId;
  private String appId;
  private String serviceId;
  private String envId;
  private String envName;
  private String connectorId;

  @Before
  public void setup() {
    Account account = anAccount().withAccountName(generateUUID()).build();

    account.setEncryptedLicenseInfo(
        EncryptionUtils.encrypt(LicenseUtil.convertToString(LicenseInfo.builder().accountType(AccountType.PAID).build())
                                    .getBytes(Charset.forName("UTF-8")),
            null));
    accountId = wingsPersistence.save(account);
    appId = wingsPersistence.save(anApplication().accountId(accountId).name(generateUUID()).build());
    envName = generateUuid();
    connectorId = generateUuid();
    serviceId = wingsPersistence.save(Service.builder().appId(appId).name(generateUuid()).build());
    envId = wingsPersistence.save(anEnvironment().appId(appId).name(envName).build());
  }

  @Test
  @Category(UnitTests.class)
  public void testNoAnalysisRecords() {
    NewRelicCVServiceConfiguration cvServiceConfiguration =
        NewRelicCVServiceConfiguration.builder().applicationId(generateUUID()).build();
    cvServiceConfiguration.setName(generateUUID());
    cvServiceConfiguration.setConnectorId(generateUUID());
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.LOW);
    cvConfigurationResource.saveCVConfiguration(accountId, appId, StateType.NEW_RELIC, cvServiceConfiguration)
        .getResource();

    // ask for last 6 hours
    long hoursToAsk = 6;
    long endTime = System.currentTimeMillis();
    List<HeatMap> heatMaps = continuousVerificationService.getHeatMap(
        accountId, appId, serviceId, endTime - TimeUnit.HOURS.toMillis(hoursToAsk), endTime, false);

    assertEquals(1, heatMaps.size());

    HeatMap heatMapSummary = heatMaps.get(0);
    assertEquals(TimeUnit.HOURS.toMinutes(hoursToAsk) / CRON_POLL_INTERVAL_IN_MINUTES,
        heatMapSummary.getRiskLevelSummary().size());
    heatMapSummary.getRiskLevelSummary().forEach(riskLevel -> {
      assertEquals(0, riskLevel.getHighRisk());
      assertEquals(0, riskLevel.getMediumRisk());
      assertEquals(0, riskLevel.getLowRisk());
      assertEquals(1, riskLevel.getNa());
    });
  }

  @Test
  @Category(UnitTests.class)
  public void testNoMergingWithoutGap() {
    NewRelicCVServiceConfiguration cvServiceConfiguration =
        NewRelicCVServiceConfiguration.builder().applicationId(generateUUID()).build();
    cvServiceConfiguration.setName(generateUUID());
    cvServiceConfiguration.setConnectorId(generateUUID());
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.LOW);
    String cvConfigId =
        cvConfigurationResource.saveCVConfiguration(accountId, appId, StateType.NEW_RELIC, cvServiceConfiguration)
            .getResource();

    // ask for last 6 hours
    long hoursToAsk = 12;
    long endTime = System.currentTimeMillis();
    long endMinute = TimeUnit.MILLISECONDS.toMinutes(endTime);
    for (long analysisMinute = endMinute; analysisMinute > endMinute - TimeUnit.HOURS.toMinutes(hoursToAsk);
         analysisMinute -= CRON_POLL_INTERVAL_IN_MINUTES) {
      TimeSeriesMLAnalysisRecord analysisRecord = TimeSeriesMLAnalysisRecord.builder().build();
      analysisRecord.setAppId(appId);
      analysisRecord.setCvConfigId(cvConfigId);
      analysisRecord.setAnalysisMinute((int) analysisMinute);
      analysisRecord.setOverallMetricScores(new HashMap<String, Double>() {
        {
          put("key1", 0.76);
          put("key2", 0.5);
        }
      });

      Map<String, TimeSeriesMLTxnSummary> txnSummaryMap = new HashMap<>();
      TimeSeriesMLTxnSummary timeSeriesMLTxnSummary = new TimeSeriesMLTxnSummary();
      Map<String, TimeSeriesMLMetricSummary> timeSeriesMLMetricSummaryMap = new HashMap<>();
      timeSeriesMLTxnSummary.setMetrics(timeSeriesMLMetricSummaryMap);
      txnSummaryMap.put(generateUuid(), timeSeriesMLTxnSummary);

      TimeSeriesMLMetricSummary timeSeriesMLMetricSummary = new TimeSeriesMLMetricSummary();
      Map<String, TimeSeriesMLHostSummary> timeSeriesMLHostSummaryMap = new HashMap<>();
      timeSeriesMLMetricSummary.setResults(timeSeriesMLHostSummaryMap);
      timeSeriesMLMetricSummaryMap.put(generateUuid(), timeSeriesMLMetricSummary);
      timeSeriesMLHostSummaryMap.put(generateUuid(), TimeSeriesMLHostSummary.builder().risk(2).build());
      timeSeriesMLMetricSummary.setMax_risk(2);

      analysisRecord.setTransactions(txnSummaryMap);
      analysisRecord.setAggregatedRisk(2);
      wingsPersistence.save(analysisRecord);
    }
    List<HeatMap> heatMaps = continuousVerificationService.getHeatMap(
        accountId, appId, serviceId, endTime - TimeUnit.HOURS.toMillis(hoursToAsk), endTime, false);

    assertEquals(1, heatMaps.size());

    HeatMap heatMapSummary = heatMaps.get(0);
    assertEquals(TimeUnit.HOURS.toMinutes(hoursToAsk) / CRON_POLL_INTERVAL_IN_MINUTES,
        heatMapSummary.getRiskLevelSummary().size());
    heatMapSummary.getRiskLevelSummary().forEach(riskLevel -> {
      assertEquals(1, riskLevel.getHighRisk());
      assertEquals(0, riskLevel.getMediumRisk());
      assertEquals(0, riskLevel.getLowRisk());
      assertEquals(0, riskLevel.getNa());
    });

    // ask for 35 mins before and 48 mins after
    heatMaps = continuousVerificationService.getHeatMap(accountId, appId, serviceId,
        endTime - TimeUnit.HOURS.toMillis(hoursToAsk) - TimeUnit.MINUTES.toMillis(35),
        endTime + TimeUnit.MINUTES.toMillis(48), false);

    assertEquals(1, heatMaps.size());

    heatMapSummary = heatMaps.get(0);

    // The inverval is > 12 hours, hence resolution is that of 24hrs
    // total small units should be 53, resolved units should be 26
    assertEquals(1 + ((5 + TimeUnit.HOURS.toMinutes(hoursToAsk) / CRON_POLL_INTERVAL_IN_MINUTES) / 2),
        heatMapSummary.getRiskLevelSummary().size());
    AtomicInteger index = new AtomicInteger();
    heatMapSummary.getRiskLevelSummary().forEach(riskLevel -> {
      if (index.get() == 0 || index.get() >= 25) {
        assertEquals(0, riskLevel.getHighRisk());
        assertEquals(1, riskLevel.getNa());
      } else {
        assertEquals(1, riskLevel.getHighRisk());
        assertEquals(0, riskLevel.getNa());
      }
      assertEquals(0, riskLevel.getMediumRisk());
      assertEquals(0, riskLevel.getLowRisk());
      index.incrementAndGet();
    });

    // create gaps in between and test
    // remove 5th, 6th and 34th from endMinute
    wingsPersistence.delete(wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
                                .filter("analysisMinute", endMinute - 5 * CRON_POLL_INTERVAL_IN_MINUTES));
    wingsPersistence.delete(wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
                                .filter("analysisMinute", endMinute - 6 * CRON_POLL_INTERVAL_IN_MINUTES));
    wingsPersistence.delete(wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
                                .filter("analysisMinute", endMinute - 34 * CRON_POLL_INTERVAL_IN_MINUTES));

    // ask for 35 mins before and 48 mins after
    heatMaps = continuousVerificationService.getHeatMap(accountId, appId, serviceId,
        endTime - TimeUnit.HOURS.toMillis(hoursToAsk) - TimeUnit.MINUTES.toMillis(35),
        endTime + TimeUnit.MINUTES.toMillis(48), false);
    assertEquals(1, heatMaps.size());

    heatMapSummary = heatMaps.get(0);

    assertEquals(1 + ((5 + TimeUnit.HOURS.toMinutes(hoursToAsk) / CRON_POLL_INTERVAL_IN_MINUTES) / 2),
        heatMapSummary.getRiskLevelSummary().size());
    index.set(0);
    heatMapSummary.getRiskLevelSummary().forEach(riskLevel -> {
      if (index.get() == 0 || index.get() >= 25) {
        assertEquals(0, riskLevel.getHighRisk());
        assertEquals(1, riskLevel.getNa());
      } else if (index.get() == 7 || index.get() == 21 || index.get() == 22) {
        assertEquals(1, riskLevel.getHighRisk());
        assertEquals(0, riskLevel.getNa());
      } else {
        assertEquals(1, riskLevel.getHighRisk());
        assertEquals(0, riskLevel.getNa());
      }
      assertEquals(0, riskLevel.getMediumRisk());
      assertEquals(0, riskLevel.getLowRisk());
      index.incrementAndGet();
    });
  }

  // Test to be un-ignored as per https://harness.atlassian.net/browse/LE-1150
  @Test
  @Owner(emails = "vaibhav.tulsyan@harness.io", intermittent = true)
  @Category(UnitTests.class)
  @Ignore
  public void testTimeSeries() {
    final int DURATION_IN_HOURS = 12;

    CVConfiguration cvConfiguration = new CVConfiguration();
    cvConfiguration.setName(generateUuid());
    cvConfiguration.setAccountId(accountId);
    cvConfiguration.setAppId(appId);
    cvConfiguration.setAnalysisTolerance(AnalysisTolerance.LOW);
    cvConfiguration.setConnectorId(connectorId);
    cvConfiguration.setConnectorName(generateUuid());
    cvConfiguration.setEnabled24x7(true);
    cvConfiguration.setEnvId(envId);
    cvConfiguration.setEnvName(envName);
    cvConfiguration.setServiceId(serviceId);
    cvConfiguration.setServiceName(generateUuid());
    cvConfiguration.setStateType(StateType.APP_DYNAMICS);
    cvConfiguration = wingsPersistence.saveAndGet(CVConfiguration.class, cvConfiguration);

    createAppDConnector();

    long endTime = Timestamp.currentMinuteBoundary();
    long start12HoursAgo = endTime - TimeUnit.HOURS.toMillis(DURATION_IN_HOURS);
    int startMinuteFor12Hrs = (int) TimeUnit.MILLISECONDS.toMinutes(start12HoursAgo);

    int endMinute = (int) TimeUnit.MILLISECONDS.toMinutes(endTime);

    // Generate data for 12 hours
    for (int analysisMinute = endMinute, j = 0; analysisMinute >= startMinuteFor12Hrs;
         analysisMinute -= VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES, j++) {
      // for last 15 minutes, set risk as 1. For everything before that, set it to 0.
      saveMetricDataToDb(analysisMinute, cvConfiguration);
      saveTimeSeriesRecordToDb(
          analysisMinute, cvConfiguration, j >= 15 ? 0 : 1); // analysis minute = end minute of analyis
    }

    long start15MinutesAgo = endTime - TimeUnit.MINUTES.toMillis(VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES);

    // First case: Fetch an exact block of 15 minutes
    testTSFor15Mins(start15MinutesAgo, endTime, cvConfiguration);

    // Second case: Fetch last 5 data points from one record, all 15 from the next records
    testTSFor20Mins(endTime, cvConfiguration);

    // Third case: Fetch last 5 data points from one record, first 5 data points from next record
    testOverlappingQuery(endTime, cvConfiguration);
  }

  private void testOverlappingQuery(long endTime, CVConfiguration cvConfiguration) {
    Map<String, Map<String, TimeSeriesOfMetric>> timeSeries;
    Map<String, TimeSeriesOfMetric> metricMap;
    Collection<TimeSeriesDataPoint> dataPoints;
    long startEpoch20MinutesAgo = endTime - TimeUnit.MINUTES.toMillis(20);
    long endEpoch10MinutesAgo = endTime - TimeUnit.MINUTES.toMillis(10);
    long historyStartTime = startEpoch20MinutesAgo - TimeUnit.HOURS.toMillis(1);

    timeSeries = continuousVerificationService.fetchObservedTimeSeries(
        startEpoch20MinutesAgo + 1, endEpoch10MinutesAgo, cvConfiguration, historyStartTime + 1);
    assertTrue(timeSeries.containsKey("/login"));
    metricMap = timeSeries.get("/login");
    dataPoints = metricMap.get("95th Percentile Response Time (ms)").getTimeSeries();
    assertEquals(70, dataPoints.size());
  }

  private void testTSFor20Mins(long endTime, CVConfiguration cvConfiguration) {
    Map<String, Map<String, TimeSeriesOfMetric>> timeSeries;
    Map<String, TimeSeriesOfMetric> metricMap;
    Collection<TimeSeriesDataPoint> dataPoints;
    long start20MinutesAgo = endTime - TimeUnit.MINUTES.toMillis(20);
    long historyStartTime = start20MinutesAgo - TimeUnit.HOURS.toMillis(1);

    timeSeries = continuousVerificationService.fetchObservedTimeSeries(
        start20MinutesAgo + 1, endTime, cvConfiguration, historyStartTime + 1);
    assertTrue(timeSeries.containsKey("/login"));
    metricMap = timeSeries.get("/login");
    dataPoints = metricMap.get("95th Percentile Response Time (ms)").getTimeSeries();
    assertEquals(80, dataPoints.size());
  }

  @NotNull
  private void testTSFor15Mins(long startTime, long endTime, CVConfiguration cvConfiguration) {
    Map<String, Map<String, TimeSeriesOfMetric>> timeSeries;
    Map<String, TimeSeriesOfMetric> metricMap;
    Collection<TimeSeriesDataPoint> dataPoints;
    long historyStartTime = startTime - TimeUnit.HOURS.toMillis(1);
    timeSeries = continuousVerificationService.fetchObservedTimeSeries(
        startTime + 1, endTime, cvConfiguration, historyStartTime + 1);
    assertTrue(timeSeries.containsKey("/login"));

    metricMap = timeSeries.get("/login");
    assertTrue(metricMap.containsKey("95th Percentile Response Time (ms)"));

    dataPoints = metricMap.get("95th Percentile Response Time (ms)").getTimeSeries();

    assertEquals(75, dataPoints.size());

    // [-inf, end-15) => risk=0
    // [end-15, end] => risk=1
    // history = last 60 mins => risk = 0
    // current ts = 15 mins => risk = 1
    // After overlap, risk of metric should be 1, not 0
    // Risk of current ts *overrides* risk of history ts
    assertEquals(1, metricMap.get("95th Percentile Response Time (ms)").getRisk());
  }

  private void saveMetricDataToDb(int analysisMinute, CVConfiguration cvConfiguration) {
    for (int min = analysisMinute; min > analysisMinute - CRON_POLL_INTERVAL_IN_MINUTES; min--) {
      Map<String, Double> metricMap = new HashMap<>();
      metricMap.put("95th Percentile Response Time (ms)", ThreadLocalRandom.current().nextDouble(0, 30));
      NewRelicMetricDataRecord record = NewRelicMetricDataRecord.builder()
                                            .appId(cvConfiguration.getAppId())
                                            .serviceId(cvConfiguration.getServiceId())
                                            .cvConfigId(cvConfiguration.getUuid())
                                            .dataCollectionMinute(min)
                                            .stateType(StateType.APP_DYNAMICS)
                                            .name("/login")
                                            .values(metricMap)
                                            .build();
      wingsPersistence.save(record);
    }
  }

  private void saveTimeSeriesRecordToDb(int analysisMinute, CVConfiguration cvConfiguration, int risk) {
    final String DEFAULT_GROUP_NAME = "default";
    final String DEFAULT_RESULTS_KEY = "docker-test/tier";

    TimeSeriesMLAnalysisRecord record = TimeSeriesMLAnalysisRecord.builder().build();
    record.setAnalysisMinute(analysisMinute);
    record.setStateType(StateType.APP_DYNAMICS);
    record.setGroupName(DEFAULT_GROUP_NAME);
    record.setCvConfigId(cvConfiguration.getUuid());
    record.setAppId(appId);

    // transactions
    Map<String, TimeSeriesMLTxnSummary> txnMap = new HashMap<>();
    TimeSeriesMLTxnSummary txnSummary = new TimeSeriesMLTxnSummary();

    // txn => 0
    txnSummary.setTxn_name("/login");
    txnSummary.setGroup_name(DEFAULT_GROUP_NAME);

    // txn => 0 => metrics
    Map<String, TimeSeriesMLMetricSummary> metricSummaryMap = new HashMap<>();

    // txn => 0 => metrics => 0
    TimeSeriesMLMetricSummary metricSummary = new TimeSeriesMLMetricSummary();
    metricSummary.setMetric_name("95th Percentile Response Time (ms)");

    // txn => 0 => metrics => 0 => results
    Map<String, TimeSeriesMLHostSummary> results = new HashMap<>();

    // txn => 0 => metrics => 0 => results => docker-test/tier
    List<Double> test_data = new ArrayList<>();
    for (int i = 0; i < VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES; i++) {
      test_data.add(ThreadLocalRandom.current().nextDouble(0, 30));
    }
    TimeSeriesMLHostSummary timeSeriesMLHostSummary =
        TimeSeriesMLHostSummary.builder().risk(risk).test_data(test_data).build();
    results.put(DEFAULT_RESULTS_KEY, timeSeriesMLHostSummary);

    // Set/put everything we have constructed so far
    metricSummary.setResults(results);
    metricSummary.setMax_risk(risk);
    metricSummaryMap.put("0", metricSummary);
    txnSummary.setMetrics(metricSummaryMap);
    txnSummary.setMetrics(metricSummaryMap);
    txnMap.put("0", txnSummary);
    record.setTransactions(txnMap);

    // Save to DB
    wingsPersistence.save(record);
  }

  @Test
  @Category(UnitTests.class)
  public void testWithActualData() throws IOException {
    readAndSaveAnalysisRecords();

    List<HeatMap> heatMaps =
        cvDashboardResource.getHeatMapSummary(accountId, appId, serviceId, 1541083500000L, 1541126700000L)
            .getResource();
    assertEquals(1, heatMaps.size());
    assertEquals(48, heatMaps.get(0).getRiskLevelSummary().size());
  }

  @Test
  @Owner(emails = {"praveen.sugavanam@harness.io"}, intermittent = true)
  @Category(UnitTests.class)
  @Ignore
  public void testSortingFromDB() throws IOException {
    String cvConfigId = readAndSaveAnalysisRecords();
    long startTime = TimeUnit.MINUTES.toMillis(25685446);
    long endTime = TimeUnit.MINUTES.toMillis(25685461);
    long historyStart = TimeUnit.MINUTES.toMillis(25685326);

    RestResponse<SortedSet<TransactionTimeSeries>> timeSeries =
        cvDashboardResource.getFilteredTimeSeriesOfHeatMapUnit(accountId, cvConfigId, startTime + 1, 1541177160000L, 0,
            Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList());

    assertEquals(7, timeSeries.getResource().size());
  }

  @Test
  @Owner(emails = {"praveen.sugavanam@harness.io"}, intermittent = true)
  @Category(UnitTests.class)
  @Ignore
  public void testDeeplinkUrlAppDynamicsFromDB() throws IOException {
    String cvConfigId = readAndSaveAnalysisRecords();
    long startTime = TimeUnit.MINUTES.toMillis(25685446);

    SortedSet<TransactionTimeSeries> timeSeries =
        continuousVerificationService.getTimeSeriesOfHeatMapUnit(TimeSeriesFilter.builder()
                                                                     .cvConfigId(cvConfigId)
                                                                     .startTime(startTime + 1)
                                                                     .endTime(1541177160000L)
                                                                     .historyStartTime(0)
                                                                     .build());
    assertEquals(7, timeSeries.size());
    TransactionTimeSeries insideTimeSeries = null;
    for (TransactionTimeSeries series : timeSeries) {
      if (series.getTransactionName().equals("/todolist/exception")) {
        insideTimeSeries = series;
        break;
      }
    }
    for (TimeSeriesOfMetric metric : insideTimeSeries.getMetricTimeSeries()) {
      if (isNotEmpty(metric.getMetricDeeplinkUrl())) {
        assertTrue(metric.getMetricDeeplinkUrl().contains("https://harness-test.saas.appdynamics.com/controller/"));
      }
    }
  }

  private void createAppDConnector() {
    AppDynamicsConfig appDynamicsConfig =
        AppDynamicsConfig.builder().controllerUrl("https://harness-test.saas.appdynamics.com/controller/").build();
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(appDynamicsConfig);
    attribute.setUuid(connectorId);
    wingsPersistence.save(attribute);
  }
  private String readAndSaveAnalysisRecords() throws IOException {
    AppDynamicsCVServiceConfiguration cvServiceConfiguration = AppDynamicsCVServiceConfiguration.builder()
                                                                   .appDynamicsApplicationId(generateUuid())
                                                                   .tierId(generateUuid())
                                                                   .build();
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setStateType(StateType.APP_DYNAMICS);
    String cvConfigId = wingsPersistence.save(cvServiceConfiguration);

    AppDynamicsConfig appDynamicsConfig =
        AppDynamicsConfig.builder().controllerUrl("https://harness-test.saas.appdynamics.com/controller/").build();
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(appDynamicsConfig);
    attribute.setUuid(connectorId);
    wingsPersistence.save(attribute);

    File file1 = new File(getClass().getClassLoader().getResource("./verification/dataForTimeSeries.json").getFile());
    final Gson gson1 = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file1))) {
      Type type = new TypeToken<List<NewRelicMetricDataRecord>>() {}.getType();
      List<NewRelicMetricDataRecord> metricDataRecords = gson1.fromJson(br, type);
      metricDataRecords.forEach(timeSeriesMLAnalysisRecord -> {
        timeSeriesMLAnalysisRecord.setAppId(appId);
        timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
      });
      wingsPersistence.save(metricDataRecords);
    }

    File file = new File(getClass().getClassLoader().getResource("./verification/24_7_ts_analysis.json").getFile());
    final Gson gson = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<List<TimeSeriesMLAnalysisRecord>>() {}.getType();
      List<TimeSeriesMLAnalysisRecord> timeSeriesMLAnalysisRecords = gson.fromJson(br, type);
      timeSeriesMLAnalysisRecords.forEach(timeSeriesMLAnalysisRecord -> {
        timeSeriesMLAnalysisRecord.setAppId(appId);
        timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
      });
      wingsPersistence.save(timeSeriesMLAnalysisRecords);
    }
    return cvConfigId;
  }

  @Test
  @Category(UnitTests.class)
  @Ignore
  public void testSorting() throws IOException {
    long currentTime = System.currentTimeMillis();
    AppDynamicsCVServiceConfiguration cvServiceConfiguration = AppDynamicsCVServiceConfiguration.builder()
                                                                   .appDynamicsApplicationId(generateUuid())
                                                                   .tierId(generateUuid())
                                                                   .build();
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setConnectorId(generateUuid());
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setStateType(StateType.APP_DYNAMICS);
    String cvConfigId = wingsPersistence.save(cvServiceConfiguration);

    final Gson gson = new Gson();
    File file =
        new File(getClass().getClassLoader().getResource("./verification/cv_24_7_analysis_record.json").getFile());
    TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord;
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<TimeSeriesMLAnalysisRecord>() {}.getType();
      timeSeriesMLAnalysisRecord = gson.fromJson(br, type);
      timeSeriesMLAnalysisRecord.setAppId(appId);
      timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
      timeSeriesMLAnalysisRecord.setAnalysisMinute((int) TimeUnit.MILLISECONDS.toMinutes(currentTime));
      wingsPersistence.save(timeSeriesMLAnalysisRecord);
    }

    SortedSet<TransactionTimeSeries> timeSeries =
        cvDashboardResource
            .getFilteredTimeSeriesOfHeatMapUnit(accountId, cvConfigId,
                currentTime - TimeUnit.MINUTES.toMillis(CRON_POLL_INTERVAL_IN_MINUTES) + 1, currentTime, 0,
                Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList())
            .getResource();

    assertEquals(9, timeSeries.size());
    ArrayList<TransactionTimeSeries> timeSeriesList = new ArrayList<>(timeSeries);
    for (int i = 0; i < 8; i++) {
      assertTrue(timeSeriesList.get(i).getMetricTimeSeries().first().compareTo(
                     timeSeriesList.get(i + 1).getMetricTimeSeries().first())
          <= 0);
    }

    timeSeriesMLAnalysisRecord.getTransactions().get("6").getMetrics().get("1").setMax_risk(2);
    timeSeriesMLAnalysisRecord.getTransactions().get("6").getMetrics().get("0").setMax_risk(1);
    timeSeriesMLAnalysisRecord.getTransactions().get("4").getMetrics().get("0").setMax_risk(2);
    timeSeriesMLAnalysisRecord.getTransactions().get("2").getMetrics().get("0").setMax_risk(1);
    timeSeriesMLAnalysisRecord.getTransactions().get("9").getMetrics().get("0").setMax_risk(1);
    wingsPersistence.save(timeSeriesMLAnalysisRecord);

    timeSeries =
        cvDashboardResource
            .getFilteredTimeSeriesOfHeatMapUnit(accountId, cvConfigId, currentTime - TimeUnit.MINUTES.toMillis(15),
                currentTime, 0, Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList())
            .getResource();

    assertEquals(9, timeSeries.size());
    timeSeriesList = new ArrayList<>(timeSeries);
    assertEquals(timeSeriesMLAnalysisRecord.getTransactions().get("6").getTxn_name(),
        timeSeriesList.get(0).getTransactionName());
    assertEquals(timeSeriesMLAnalysisRecord.getTransactions().get("4").getTxn_name(),
        timeSeriesList.get(1).getTransactionName());
    assertEquals(timeSeriesMLAnalysisRecord.getTransactions().get("2").getTxn_name(),
        timeSeriesList.get(2).getTransactionName());
    assertEquals(timeSeriesMLAnalysisRecord.getTransactions().get("9").getTxn_name(),
        timeSeriesList.get(3).getTransactionName());
    assertEquals(timeSeriesMLAnalysisRecord.getTransactions().get("7").getTxn_name(),
        timeSeriesList.get(4).getTransactionName());
    assertEquals(timeSeriesMLAnalysisRecord.getTransactions().get("3").getTxn_name(),
        timeSeriesList.get(5).getTransactionName());
    assertEquals(timeSeriesMLAnalysisRecord.getTransactions().get("0").getTxn_name(),
        timeSeriesList.get(6).getTransactionName());
    assertEquals(timeSeriesMLAnalysisRecord.getTransactions().get("8").getTxn_name(),
        timeSeriesList.get(7).getTransactionName());
    assertEquals(timeSeriesMLAnalysisRecord.getTransactions().get("1").getTxn_name(),
        timeSeriesList.get(8).getTransactionName());

    List<TimeSeriesOfMetric> metrics = new ArrayList<>(timeSeriesList.get(0).getMetricTimeSeries());
    assertEquals(timeSeriesMLAnalysisRecord.getTransactions().get("6").getMetrics().get("1").getMetric_name(),
        metrics.get(0).getMetricName());
    assertEquals(timeSeriesMLAnalysisRecord.getTransactions().get("6").getMetrics().get("0").getMetric_name(),
        metrics.get(1).getMetricName());
    assertEquals(timeSeriesMLAnalysisRecord.getTransactions().get("6").getMetrics().get("2").getMetric_name(),
        metrics.get(2).getMetricName());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetRiskArray() throws Exception {
    AppDynamicsCVServiceConfiguration cvServiceConfiguration = AppDynamicsCVServiceConfiguration.builder()
                                                                   .appDynamicsApplicationId(generateUuid())
                                                                   .tierId(generateUuid())
                                                                   .build();
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setStateType(StateType.APP_DYNAMICS);
    String cvConfigId = wingsPersistence.save(cvServiceConfiguration);

    createAppDConnector();

    File file1 = new File(getClass().getClassLoader().getResource("./verification/metricsForRisk.json").getFile());
    final Gson gson1 = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file1))) {
      Type type = new TypeToken<List<NewRelicMetricDataRecord>>() {}.getType();
      List<NewRelicMetricDataRecord> metricDataRecords = gson1.fromJson(br, type);
      metricDataRecords.forEach(timeSeriesMLAnalysisRecord -> {
        timeSeriesMLAnalysisRecord.setAppId(appId);
        timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
      });
      wingsPersistence.save(metricDataRecords);
    }

    File file = new File(getClass().getClassLoader().getResource("./verification/24_7_ts_analysis.json").getFile());
    final Gson gson = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<List<TimeSeriesMLAnalysisRecord>>() {}.getType();
      List<TimeSeriesMLAnalysisRecord> timeSeriesMLAnalysisRecords = gson.fromJson(br, type);
      timeSeriesMLAnalysisRecords.forEach(timeSeriesMLAnalysisRecord -> {
        timeSeriesMLAnalysisRecord.setAppId(appId);
        timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
      });
      wingsPersistence.save(timeSeriesMLAnalysisRecords);
      saveRiskSummaries(timeSeriesMLAnalysisRecords);
    }
    long startTime = TimeUnit.MINUTES.toMillis(25685446);
    long endTime = TimeUnit.MINUTES.toMillis(25685461);
    long historyStart = TimeUnit.MINUTES.toMillis(25685326);
    SortedSet<TransactionTimeSeries> timeseries =
        continuousVerificationService.getTimeSeriesOfHeatMapUnit(TimeSeriesFilter.builder()
                                                                     .cvConfigId(cvConfigId)
                                                                     .startTime(startTime + 1)
                                                                     .endTime(endTime)
                                                                     .historyStartTime(historyStart + 1)
                                                                     .build());
    assertEquals(5, timeseries.size());
    assertNotNull("Metric timeseries shouldn't be null", timeseries.first().getMetricTimeSeries());
    assertEquals(9, timeseries.first().getMetricTimeSeries().first().getRisksForTimeSeries().size());
    TimeSeriesRisk timeSeriesRisk =
        timeseries.first().getMetricTimeSeries().first().getRisksForTimeSeries().iterator().next();
    Iterator<TransactionTimeSeries> iterator = timeseries.iterator();
    while (iterator.hasNext()) {
      TransactionTimeSeries transactionTimeSeries = iterator.next();
      if (transactionTimeSeries.getTransactionName().equals("/todolist/inside")) {
        timeSeriesRisk = transactionTimeSeries.getMetricTimeSeries().first().getRisksForTimeSeries().iterator().next();
        break;
      }
    }
    assertEquals(2, timeSeriesRisk.getRisk());
    assertEquals("End time should be correct", TimeUnit.MINUTES.toMillis(25685341), timeSeriesRisk.getEndTime());
    assertEquals("Start time should be correct",
        TimeUnit.MINUTES.toMillis(25685341 - CRON_POLL_INTERVAL_IN_MINUTES) + 1, timeSeriesRisk.getStartTime());
  }

  @Test
  @Category(UnitTests.class)
  public void testTrafficLight() throws Exception {
    AppDynamicsCVServiceConfiguration cvServiceConfiguration = AppDynamicsCVServiceConfiguration.builder()
                                                                   .appDynamicsApplicationId(generateUuid())
                                                                   .tierId(generateUuid())
                                                                   .build();
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setStateType(StateType.APP_DYNAMICS);
    String cvConfigId = wingsPersistence.save(cvServiceConfiguration);

    createAppDConnector();

    TimeSeriesMLAnalysisRecord tsAnalysisRecord = null;
    List<Double> expectedTimeSeries = new ArrayList<>();

    File file1 = new File(getClass()
                              .getClassLoader()
                              .getResource("./verification/24x7_ts_transactionMetricRisk_MetricRecords")
                              .getFile());
    final Gson gson1 = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file1))) {
      Type type = new TypeToken<List<NewRelicMetricDataRecord>>() {}.getType();
      List<NewRelicMetricDataRecord> metricDataRecords = gson1.fromJson(br, type);
      metricDataRecords.forEach(timeSeriesMLAnalysisRecord -> {
        timeSeriesMLAnalysisRecord.setAppId(appId);
        timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
        timeSeriesMLAnalysisRecord.setStateType(StateType.APP_DYNAMICS);
      });
      wingsPersistence.save(metricDataRecords);
    }

    File file = new File(getClass()
                             .getClassLoader()
                             .getResource("./verification/24_7_ts_transactionMetricRisk_regression.json")
                             .getFile());
    final Gson gson = new Gson();

    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<List<TimeSeriesMLAnalysisRecord>>() {}.getType();
      List<TimeSeriesMLAnalysisRecord> timeSeriesMLAnalysisRecords = gson.fromJson(br, type);

      int idx = 0;
      for (int i = 0; i < timeSeriesMLAnalysisRecords.size(); i++) {
        tsAnalysisRecord = timeSeriesMLAnalysisRecords.get(i);
        tsAnalysisRecord.setAppId(appId);
        tsAnalysisRecord.setCvConfigId(cvConfigId);
        // 2nd record in json list contains the expected timeseries
        // metrics.0.test.data contains 135 elements: 2hrs of history + 15mins of current heatmap unit
        tsAnalysisRecord.getTransactions()
            .values()
            .iterator()
            .next()
            .getMetrics()
            .values()
            .iterator()
            .next()
            .setLong_term_pattern(1);
        if (idx == 1
            && tsAnalysisRecord.getTransactions().get("45").getMetrics().get("0").getMetric_name().equals(
                   "95th Percentile Response Time (ms)")) {
          tsAnalysisRecord.getTransactions().get("45").getMetrics().get("0").setLong_term_pattern(1);
          expectedTimeSeries =
              tsAnalysisRecord.getTransactions().get("45").getMetrics().get("0").getTest().getData().get(0);
        }
        idx++;
      }
      wingsPersistence.save(timeSeriesMLAnalysisRecords);
      saveRiskSummaries(timeSeriesMLAnalysisRecords);
    }

    long startTime = 1541522760001L;
    long endTime = 1541523660000L;
    long historyStart = 1541515560001L;
    boolean longterm = false;
    SortedSet<TransactionTimeSeries> timeseries =
        continuousVerificationService.getTimeSeriesOfHeatMapUnit(TimeSeriesFilter.builder()
                                                                     .cvConfigId(cvConfigId)
                                                                     .startTime(startTime)
                                                                     .endTime(endTime)
                                                                     .historyStartTime(historyStart)
                                                                     .build());
    for (TransactionTimeSeries s : timeseries) {
      for (TimeSeriesOfMetric tms : s.getMetricTimeSeries()) {
        if (tms.isLongTermPattern()) {
          longterm = true;
        }
      }
    }
    assertTrue("Atleast one record has longterm set to true", longterm);
    TransactionTimeSeries apiArtifactsTransaction = null;
    for (Iterator<TransactionTimeSeries> it = timeseries.iterator(); it.hasNext();) {
      TransactionTimeSeries txnTimeSeries = it.next();
      if (txnTimeSeries.getTransactionName().equals("/api/artifacts")) {
        apiArtifactsTransaction = txnTimeSeries;
        break;
      }
    }

    TimeSeriesOfMetric apiArtifactsRespTimeTS = null;
    for (Iterator<TimeSeriesOfMetric> it = apiArtifactsTransaction.getMetricTimeSeries().iterator(); it.hasNext();) {
      TimeSeriesOfMetric metricTS = it.next();
      if (metricTS.getMetricName().equals("95th Percentile Response Time (ms)")) {
        apiArtifactsRespTimeTS = metricTS;
        break;
      }
    }

    assertEquals(-1, apiArtifactsRespTimeTS.getRisk());

    List<Double> actualTimeSeries = new ArrayList<>();
    for (TimeSeriesDataPoint datapoint : apiArtifactsRespTimeTS.getTimeSeries()) {
      actualTimeSeries.add(datapoint.getValue());
    }
    assertEquals(135, actualTimeSeries.size());
    assertEquals(expectedTimeSeries, actualTimeSeries);
  }

  // Test to be un-ignored as per https://harness.atlassian.net/browse/LE-1150
  @Test
  @Owner(emails = "vaibhav.tulsyan@harness.io", intermittent = true)
  @Category(UnitTests.class)
  @Ignore
  public void testRiskArrayEndpointContainment() throws Exception {
    AppDynamicsCVServiceConfiguration cvServiceConfiguration = AppDynamicsCVServiceConfiguration.builder()
                                                                   .appDynamicsApplicationId(generateUuid())
                                                                   .tierId(generateUuid())
                                                                   .build();
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setStateType(StateType.APP_DYNAMICS);
    String cvConfigId = wingsPersistence.save(cvServiceConfiguration);

    createAppDConnector();

    File file1 = new File(getClass().getClassLoader().getResource("./verification/metricsForRisk.json").getFile());
    final Gson gson1 = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file1))) {
      Type type = new TypeToken<List<NewRelicMetricDataRecord>>() {}.getType();
      List<NewRelicMetricDataRecord> metricDataRecords = gson1.fromJson(br, type);
      metricDataRecords.forEach(timeSeriesMLAnalysisRecord -> {
        timeSeriesMLAnalysisRecord.setAppId(appId);
        timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
      });
      wingsPersistence.save(metricDataRecords);
    }

    File file = new File(getClass().getClassLoader().getResource("./verification/24_7_ts_analysis.json").getFile());
    final Gson gson = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<List<TimeSeriesMLAnalysisRecord>>() {}.getType();
      List<TimeSeriesMLAnalysisRecord> timeSeriesMLAnalysisRecords = gson.fromJson(br, type);
      timeSeriesMLAnalysisRecords.forEach(timeSeriesMLAnalysisRecord -> {
        timeSeriesMLAnalysisRecord.setAppId(appId);
        timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
      });
      wingsPersistence.save(timeSeriesMLAnalysisRecords);
    }
    long startTime = TimeUnit.MINUTES.toMillis(25685326);
    long endTime = TimeUnit.MINUTES.toMillis(25685461);
    long historyStart = TimeUnit.MINUTES.toMillis(25685326);
    SortedSet<TransactionTimeSeries> timeseries =
        continuousVerificationService.getTimeSeriesOfHeatMapUnit(TimeSeriesFilter.builder()
                                                                     .cvConfigId(cvConfigId)
                                                                     .startTime(startTime + 1)
                                                                     .endTime(endTime)
                                                                     .historyStartTime(historyStart + 1)
                                                                     .build());
    for (Iterator<TransactionTimeSeries> txnIterator = timeseries.iterator(); txnIterator.hasNext();) {
      TransactionTimeSeries txnTimeSeries = txnIterator.next();
      for (Iterator<TimeSeriesOfMetric> metricTSIterator = txnTimeSeries.getMetricTimeSeries().iterator();
           metricTSIterator.hasNext();) {
        TimeSeriesOfMetric metricTimeSeries = metricTSIterator.next();
        SortedMap<Long, TimeSeriesDataPoint> datapoints = (SortedMap) metricTimeSeries.getTimeSeriesMap();
        for (TimeSeriesRisk tsRisk : metricTimeSeries.getRisksForTimeSeries()) {
          long startTimeOfRisk = tsRisk.getStartTime();
          long endTimeOfRisk = tsRisk.getEndTime();
          assertTrue(datapoints.containsKey(startTimeOfRisk + TimeUnit.MINUTES.toMillis(1) - 1));
          assertTrue(datapoints.containsKey(endTimeOfRisk));
        }
      }
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testRiskSortLevel() throws Exception {
    AppDynamicsCVServiceConfiguration cvServiceConfiguration = AppDynamicsCVServiceConfiguration.builder()
                                                                   .appDynamicsApplicationId(generateUuid())
                                                                   .tierId(generateUuid())
                                                                   .build();
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setStateType(StateType.APP_DYNAMICS);
    String cvConfigId = wingsPersistence.save(cvServiceConfiguration);

    createAppDConnector();

    File file1 = new File(getClass().getClassLoader().getResource("./verification/metricRecords.json").getFile());
    final Gson gson1 = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file1))) {
      Type type = new TypeToken<List<NewRelicMetricDataRecord>>() {}.getType();
      List<NewRelicMetricDataRecord> metricDataRecords = gson1.fromJson(br, type);
      metricDataRecords.forEach(timeSeriesMLAnalysisRecord -> {
        timeSeriesMLAnalysisRecord.setAppId(appId);
        timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
      });
      wingsPersistence.save(metricDataRecords);
    }

    File file = new File(getClass().getClassLoader().getResource("./verification/multi-risk-sorting.json").getFile());
    final Gson gson = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<List<TimeSeriesMLAnalysisRecord>>() {}.getType();
      List<TimeSeriesMLAnalysisRecord> timeSeriesMLAnalysisRecords = gson.fromJson(br, type);
      timeSeriesMLAnalysisRecords.forEach(timeSeriesMLAnalysisRecord -> {
        timeSeriesMLAnalysisRecord.setAppId(appId);
        timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
      });
      wingsPersistence.save(timeSeriesMLAnalysisRecords);
      saveRiskSummaries(timeSeriesMLAnalysisRecords);
    }
    SortedSet<TransactionTimeSeries> timeSeries =
        cvDashboardResource
            .getFilteredTimeSeriesOfHeatMapUnit(accountId, cvConfigId, 1541688360001l, 1541689260000l, 1541681160001l,
                Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList())
            .getResource();

    ArrayList<TransactionTimeSeries> timeSeriesList = new ArrayList<>(timeSeries);
    for (int i = 0; i < timeSeriesList.size() - 1; i++) {
      assertTrue(timeSeriesList.get(i).getMetricTimeSeries().first().getRisk()
          >= timeSeriesList.get(i + 1).getMetricTimeSeries().first().getRisk());
    }
    assertEquals("/api/setup-as-code", timeSeriesList.get(0).getTransactionName());
  }

  private void saveRiskSummaries(List<TimeSeriesMLAnalysisRecord> timeSeriesMLAnalysisRecords) {
    List<TimeSeriesRiskSummary> riskSummaries = new ArrayList<>();
    timeSeriesMLAnalysisRecords.forEach(mlAnalysisResponse -> {
      TimeSeriesRiskSummary riskSummary = TimeSeriesRiskSummary.builder()
                                              .analysisMinute(mlAnalysisResponse.getAnalysisMinute())
                                              .cvConfigId(mlAnalysisResponse.getCvConfigId())
                                              .build();

      riskSummary.setAppId(mlAnalysisResponse.getAppId());
      TreeBasedTable<String, String, Integer> risks = TreeBasedTable.create();
      TreeBasedTable<String, String, Integer> longTermPatterns = TreeBasedTable.create();
      for (TimeSeriesMLTxnSummary txnSummary : mlAnalysisResponse.getTransactions().values()) {
        for (TimeSeriesMLMetricSummary mlMetricSummary : txnSummary.getMetrics().values()) {
          if (mlMetricSummary.getResults() != null) {
            risks.put(txnSummary.getTxn_name(), mlMetricSummary.getMetric_name(), mlMetricSummary.getMax_risk());
            longTermPatterns.put(
                txnSummary.getTxn_name(), mlMetricSummary.getMetric_name(), mlMetricSummary.getLong_term_pattern());
          }
        }
      }

      riskSummary.setTxnMetricRisk(risks.rowMap());
      riskSummary.setTxnMetricLongTermPattern(longTermPatterns.rowMap());
      riskSummary.compressMaps();
      riskSummaries.add(riskSummary);
    });

    wingsPersistence.save(riskSummaries);
  }
  @Test
  @Category(UnitTests.class)
  public void testNoTxnMetricFilter() throws Exception {
    AppDynamicsCVServiceConfiguration cvServiceConfiguration = AppDynamicsCVServiceConfiguration.builder()
                                                                   .appDynamicsApplicationId(generateUuid())
                                                                   .tierId(generateUuid())
                                                                   .build();
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setStateType(StateType.APP_DYNAMICS);
    String cvConfigId = wingsPersistence.save(cvServiceConfiguration);

    createAppDConnector();

    File file1 = new File(getClass().getClassLoader().getResource("./verification/metricRecords.json").getFile());
    final Gson gson1 = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file1))) {
      Type type = new TypeToken<List<NewRelicMetricDataRecord>>() {}.getType();
      List<NewRelicMetricDataRecord> metricDataRecords = gson1.fromJson(br, type);
      metricDataRecords.forEach(timeSeriesMLAnalysisRecord -> {
        timeSeriesMLAnalysisRecord.setAppId(appId);
        timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
        timeSeriesMLAnalysisRecord.setStateType(StateType.APP_DYNAMICS);
      });
      wingsPersistence.save(metricDataRecords);
    }

    final SortedSet<TransactionTimeSeries> timeSeries =
        cvDashboardResource
            .getFilteredTimeSeriesOfHeatMapUnit(accountId, cvConfigId, 1541678400000L, 1541685600000L, 0,
                Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList())
            .getResource();
    assertEquals(43, timeSeries.size());
  }

  @Test
  @Category(UnitTests.class)
  public void testTxnFilter() throws Exception {
    AppDynamicsCVServiceConfiguration cvServiceConfiguration = AppDynamicsCVServiceConfiguration.builder()
                                                                   .appDynamicsApplicationId(generateUuid())
                                                                   .tierId(generateUuid())
                                                                   .build();
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setStateType(StateType.APP_DYNAMICS);
    String cvConfigId = wingsPersistence.save(cvServiceConfiguration);

    createAppDConnector();

    File file1 = new File(getClass().getClassLoader().getResource("./verification/metricRecords.json").getFile());
    final Gson gson1 = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file1))) {
      Type type = new TypeToken<List<NewRelicMetricDataRecord>>() {}.getType();
      List<NewRelicMetricDataRecord> metricDataRecords = gson1.fromJson(br, type);
      metricDataRecords.forEach(timeSeriesMLAnalysisRecord -> {
        timeSeriesMLAnalysisRecord.setAppId(appId);
        timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
        timeSeriesMLAnalysisRecord.setStateType(StateType.APP_DYNAMICS);
      });
      wingsPersistence.save(metricDataRecords);
    }

    final SortedSet<TransactionTimeSeries> timeSeries =
        cvDashboardResource
            .getFilteredTimeSeriesOfHeatMapUnit(accountId, cvConfigId, 1541678400000L, 1541685600000L, 0,
                Lists.newArrayList("/api/setup-as-code", "/api/infrastructure-mappings", "/api/userGroups"),
                Lists.newArrayList(), Lists.newArrayList())
            .getResource();
    List<TransactionTimeSeries> timeSeriesList = new ArrayList<>(timeSeries);
    assertEquals(3, timeSeries.size());
    assertEquals("/api/userGroups", timeSeriesList.get(0).getTransactionName());
    assertEquals(1, timeSeriesList.get(0).getMetricTimeSeries().size());
    assertEquals(
        "95th Percentile Response Time (ms)", timeSeriesList.get(0).getMetricTimeSeries().first().getMetricName());

    assertEquals("/api/setup-as-code", timeSeriesList.get(1).getTransactionName());
    assertEquals(2, timeSeriesList.get(1).getMetricTimeSeries().size());
    assertEquals(
        "95th Percentile Response Time (ms)", timeSeriesList.get(1).getMetricTimeSeries().first().getMetricName());
    assertEquals("Number of Slow Calls", timeSeriesList.get(1).getMetricTimeSeries().last().getMetricName());

    assertEquals("/api/infrastructure-mappings", timeSeriesList.get(2).getTransactionName());
    assertEquals(2, timeSeriesList.get(2).getMetricTimeSeries().size());
    assertEquals(
        "95th Percentile Response Time (ms)", timeSeriesList.get(2).getMetricTimeSeries().first().getMetricName());
    assertEquals("Number of Slow Calls", timeSeriesList.get(2).getMetricTimeSeries().last().getMetricName());
  }

  @Test
  @Category(UnitTests.class)
  public void testMetricFilter() throws Exception {
    AppDynamicsCVServiceConfiguration cvServiceConfiguration = AppDynamicsCVServiceConfiguration.builder()
                                                                   .appDynamicsApplicationId(generateUuid())
                                                                   .tierId(generateUuid())
                                                                   .build();
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setStateType(StateType.APP_DYNAMICS);
    String cvConfigId = wingsPersistence.save(cvServiceConfiguration);

    createAppDConnector();

    File file1 = new File(getClass().getClassLoader().getResource("./verification/metricRecords.json").getFile());
    final Gson gson1 = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file1))) {
      Type type = new TypeToken<List<NewRelicMetricDataRecord>>() {}.getType();
      List<NewRelicMetricDataRecord> metricDataRecords = gson1.fromJson(br, type);
      metricDataRecords.forEach(timeSeriesMLAnalysisRecord -> {
        timeSeriesMLAnalysisRecord.setAppId(appId);
        timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
        timeSeriesMLAnalysisRecord.setStateType(StateType.APP_DYNAMICS);
      });
      wingsPersistence.save(metricDataRecords);
    }

    SortedSet<TransactionTimeSeries> timeSeries =
        cvDashboardResource
            .getFilteredTimeSeriesOfHeatMapUnit(accountId, cvConfigId, 1541678400000L, 1541685600000L, 0,
                Lists.newArrayList(), Lists.newArrayList("95th Percentile Response Time (ms)"), Lists.newArrayList())
            .getResource();
    assertEquals(43, timeSeries.size());
    timeSeries.forEach(transactionTimeSeries -> {
      assertEquals(1, transactionTimeSeries.getMetricTimeSeries().size());
      assertEquals(
          "95th Percentile Response Time (ms)", transactionTimeSeries.getMetricTimeSeries().first().getMetricName());
    });

    timeSeries = cvDashboardResource

                     .getFilteredTimeSeriesOfHeatMapUnit(accountId, cvConfigId, 1541678400000L, 1541685600000L, 0,
                         Lists.newArrayList(), Lists.newArrayList("Number of Slow Calls"), Lists.newArrayList())
                     .getResource();
    assertEquals(17, timeSeries.size());
    timeSeries.forEach(transactionTimeSeries -> {
      assertEquals(1, transactionTimeSeries.getMetricTimeSeries().size());
      assertEquals("Number of Slow Calls", transactionTimeSeries.getMetricTimeSeries().first().getMetricName());
    });
  }

  @Test
  @Category(UnitTests.class)
  public void testTxnMetricFilter() throws Exception {
    AppDynamicsCVServiceConfiguration cvServiceConfiguration = AppDynamicsCVServiceConfiguration.builder()
                                                                   .appDynamicsApplicationId(generateUuid())
                                                                   .tierId(generateUuid())
                                                                   .build();
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setStateType(StateType.APP_DYNAMICS);
    String cvConfigId = wingsPersistence.save(cvServiceConfiguration);

    createAppDConnector();

    File file1 = new File(getClass().getClassLoader().getResource("./verification/metricRecords.json").getFile());
    final Gson gson1 = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file1))) {
      Type type = new TypeToken<List<NewRelicMetricDataRecord>>() {}.getType();
      List<NewRelicMetricDataRecord> metricDataRecords = gson1.fromJson(br, type);
      metricDataRecords.forEach(timeSeriesMLAnalysisRecord -> {
        timeSeriesMLAnalysisRecord.setAppId(appId);
        timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
        timeSeriesMLAnalysisRecord.setStateType(StateType.APP_DYNAMICS);
      });
      wingsPersistence.save(metricDataRecords);
    }

    SortedSet<TransactionTimeSeries> timeSeries =
        cvDashboardResource
            .getFilteredTimeSeriesOfHeatMapUnit(accountId, cvConfigId, 1541678400000L, 1541685600000L, 0,
                Lists.newArrayList("/api/setup-as-code", "/api/infrastructure-mappings"),
                Lists.newArrayList("95th Percentile Response Time (ms)", "Number of Slow Calls"), Lists.newArrayList())
            .getResource();
    assertEquals(2, timeSeries.size());
    assertEquals("/api/setup-as-code", timeSeries.first().getTransactionName());
    assertEquals("/api/infrastructure-mappings", timeSeries.last().getTransactionName());
    timeSeries.forEach(transactionTimeSeries -> {
      assertEquals(2, transactionTimeSeries.getMetricTimeSeries().size());
      assertEquals(
          "95th Percentile Response Time (ms)", transactionTimeSeries.getMetricTimeSeries().first().getMetricName());
      assertEquals("Number of Slow Calls", transactionTimeSeries.getMetricTimeSeries().last().getMetricName());
    });

    timeSeries = cvDashboardResource
                     .getFilteredTimeSeriesOfHeatMapUnit(accountId, cvConfigId, 1541678400000L, 1541685600000L, 0,
                         Lists.newArrayList("/api/setup-as-code", "/api/infrastructure-mappings"),
                         Lists.newArrayList("Number of Slow Calls"), Lists.newArrayList())
                     .getResource();

    assertEquals(2, timeSeries.size());
    assertEquals("/api/setup-as-code", timeSeries.first().getTransactionName());
    assertEquals("/api/infrastructure-mappings", timeSeries.last().getTransactionName());
    timeSeries.forEach(transactionTimeSeries -> {
      assertEquals(1, transactionTimeSeries.getMetricTimeSeries().size());
      assertEquals("Number of Slow Calls", transactionTimeSeries.getMetricTimeSeries().first().getMetricName());
    });
  }
}
