/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.UJJAWAL;

import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;

import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.security.EncryptionUtils;
import io.harness.time.Timestamp;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.LicenseInfo;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.common.VerificationConstants;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.MetricType;
import software.wings.metrics.TimeSeriesDataRecord;
import software.wings.resources.CVConfigurationResource;
import software.wings.resources.ContinuousVerificationDashboardResource;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.service.impl.analysis.TimeSeriesFilter;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLHostSummary;
import software.wings.service.impl.analysis.TimeSeriesMLMetricSummary;
import software.wings.service.impl.analysis.TimeSeriesMLTxnSummary;
import software.wings.service.impl.analysis.TimeSeriesRiskSummary;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.HeatMap;
import software.wings.verification.TimeSeriesDataPoint;
import software.wings.verification.TimeSeriesOfMetric;
import software.wings.verification.TimeSeriesRisk;
import software.wings.verification.TransactionTimeSeries;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.cloudwatch.CloudWatchCVServiceConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import com.google.common.collect.Lists;
import com.google.common.collect.TreeBasedTable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Vaibhav Tulsyan
 * 19/Oct/2018
 */
@Slf4j
public class HeatMapApiUnitTest extends WingsBaseTest {
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

    account.setEncryptedLicenseInfo(EncryptionUtils.encrypt(
        LicenseUtils.convertToString(LicenseInfo.builder().accountType(AccountType.PAID).build())
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
  @Owner(developers = UJJAWAL)
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

    assertThat(heatMaps).hasSize(1);

    HeatMap heatMapSummary = heatMaps.get(0);
    assertThat(heatMapSummary.getRiskLevelSummary().size())
        .isEqualTo(TimeUnit.HOURS.toMinutes(hoursToAsk) / CRON_POLL_INTERVAL_IN_MINUTES);
    heatMapSummary.getRiskLevelSummary().forEach(riskLevel -> {
      assertThat(riskLevel.getHighRisk()).isEqualTo(0);
      assertThat(riskLevel.getMediumRisk()).isEqualTo(0);
      assertThat(riskLevel.getLowRisk()).isEqualTo(0);
      assertThat(riskLevel.getNa()).isEqualTo(1);
    });
  }

  @Test
  @Owner(developers = UJJAWAL)
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
      analysisRecord.setRiskScore(0.76);

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

    assertThat(heatMaps).hasSize(1);

    HeatMap heatMapSummary = heatMaps.get(0);
    assertThat(heatMapSummary.getRiskLevelSummary().size())
        .isEqualTo(TimeUnit.HOURS.toMinutes(hoursToAsk) / CRON_POLL_INTERVAL_IN_MINUTES);
    heatMapSummary.getRiskLevelSummary().forEach(riskLevel -> {
      assertThat(riskLevel.getHighRisk()).isEqualTo(1);
      assertThat(riskLevel.getMediumRisk()).isEqualTo(0);
      assertThat(riskLevel.getLowRisk()).isEqualTo(0);
      assertThat(riskLevel.getNa()).isEqualTo(0);
    });

    // ask for 35 mins before and 48 mins after
    heatMaps = continuousVerificationService.getHeatMap(accountId, appId, serviceId,
        endTime - TimeUnit.HOURS.toMillis(hoursToAsk) - TimeUnit.MINUTES.toMillis(35),
        endTime + TimeUnit.MINUTES.toMillis(48), false);

    assertThat(heatMaps).hasSize(1);

    heatMapSummary = heatMaps.get(0);

    // The inverval is > 12 hours, hence resolution is that of 24hrs
    // total small units should be 53, resolved units should be 26
    assertThat(heatMapSummary.getRiskLevelSummary().size())
        .isEqualTo(1 + ((5 + TimeUnit.HOURS.toMinutes(hoursToAsk) / CRON_POLL_INTERVAL_IN_MINUTES) / 2));
    AtomicInteger index = new AtomicInteger();
    heatMapSummary.getRiskLevelSummary().forEach(riskLevel -> {
      if (index.get() == 0 || index.get() >= 25) {
        assertThat(riskLevel.getHighRisk()).isEqualTo(0);
        assertThat(riskLevel.getNa()).isEqualTo(1);
      } else {
        assertThat(riskLevel.getHighRisk()).isEqualTo(1);
        assertThat(riskLevel.getNa()).isEqualTo(0);
      }
      assertThat(riskLevel.getMediumRisk()).isEqualTo(0);
      assertThat(riskLevel.getLowRisk()).isEqualTo(0);
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
    assertThat(heatMaps).hasSize(1);

    heatMapSummary = heatMaps.get(0);

    assertThat(heatMapSummary.getRiskLevelSummary().size())
        .isEqualTo(1 + ((5 + TimeUnit.HOURS.toMinutes(hoursToAsk) / CRON_POLL_INTERVAL_IN_MINUTES) / 2));
    index.set(0);
    heatMapSummary.getRiskLevelSummary().forEach(riskLevel -> {
      if (index.get() == 0 || index.get() >= 25) {
        assertThat(riskLevel.getHighRisk()).isEqualTo(0);
        assertThat(riskLevel.getNa()).isEqualTo(1);
      } else if (index.get() == 7 || index.get() == 21 || index.get() == 22) {
        assertThat(riskLevel.getHighRisk()).isEqualTo(1);
        assertThat(riskLevel.getNa()).isEqualTo(0);
      } else {
        assertThat(riskLevel.getHighRisk()).isEqualTo(1);
        assertThat(riskLevel.getNa()).isEqualTo(0);
      }
      assertThat(riskLevel.getMediumRisk()).isEqualTo(0);
      assertThat(riskLevel.getLowRisk()).isEqualTo(0);
      index.incrementAndGet();
    });
  }

  // Test to be un-ignored as per https://harness.atlassian.net/browse/LE-1150
  @Test
  @Owner(developers = RAGHU, intermittent = true)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
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
      saveMetricDataToDb(analysisMinute, cvConfiguration, StateType.APP_DYNAMICS);
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
    assertThat(timeSeries.containsKey("/login")).isTrue();
    metricMap = timeSeries.get("/login");
    dataPoints = metricMap.get("95th Percentile Response Time (ms)").getTimeSeries();
    assertThat(dataPoints).hasSize(70);
  }

  private void testTSFor20Mins(long endTime, CVConfiguration cvConfiguration) {
    Map<String, Map<String, TimeSeriesOfMetric>> timeSeries;
    Map<String, TimeSeriesOfMetric> metricMap;
    Collection<TimeSeriesDataPoint> dataPoints;
    long start20MinutesAgo = endTime - TimeUnit.MINUTES.toMillis(20);
    long historyStartTime = start20MinutesAgo - TimeUnit.HOURS.toMillis(1);

    timeSeries = continuousVerificationService.fetchObservedTimeSeries(
        start20MinutesAgo + 1, endTime, cvConfiguration, historyStartTime + 1);
    assertThat(timeSeries.containsKey("/login")).isTrue();
    metricMap = timeSeries.get("/login");
    dataPoints = metricMap.get("95th Percentile Response Time (ms)").getTimeSeries();
    assertThat(dataPoints).hasSize(80);
  }

  @NotNull
  private void testTSFor15Mins(long startTime, long endTime, CVConfiguration cvConfiguration) {
    Map<String, Map<String, TimeSeriesOfMetric>> timeSeries;
    Map<String, TimeSeriesOfMetric> metricMap;
    Collection<TimeSeriesDataPoint> dataPoints;
    long historyStartTime = startTime - TimeUnit.HOURS.toMillis(1);
    timeSeries = continuousVerificationService.fetchObservedTimeSeries(
        startTime + 1, endTime, cvConfiguration, historyStartTime + 1);
    assertThat(timeSeries.containsKey("/login")).isTrue();

    metricMap = timeSeries.get("/login");
    assertThat(metricMap.containsKey("95th Percentile Response Time (ms)")).isTrue();

    dataPoints = metricMap.get("95th Percentile Response Time (ms)").getTimeSeries();

    assertThat(dataPoints).hasSize(75);

    // [-inf, end-15) => risk=0
    // [end-15, end] => risk=1
    // history = last 60 mins => risk = 0
    // current ts = 15 mins => risk = 1
    // After overlap, risk of metric should be 1, not 0
    // Risk of current ts *overrides* risk of history ts
    assertThat(metricMap.get("95th Percentile Response Time (ms)").getRisk()).isEqualTo(1);
  }

  private void saveMetricDataToDb(int analysisMinute, CVConfiguration cvConfiguration, StateType stateType) {
    for (int min = analysisMinute; min > analysisMinute - CRON_POLL_INTERVAL_IN_MINUTES; min--) {
      Map<String, Double> metricMap = new HashMap<>();
      metricMap.put("95th Percentile Response Time (ms)", ThreadLocalRandom.current().nextDouble(0, 30));
      NewRelicMetricDataRecord record = NewRelicMetricDataRecord.builder()
                                            .appId(cvConfiguration.getAppId())
                                            .serviceId(cvConfiguration.getServiceId())
                                            .cvConfigId(cvConfiguration.getUuid())
                                            .dataCollectionMinute(min)
                                            .stateType(stateType)
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
    record.setStateType(cvConfiguration.getStateType());
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

  private void createCloudwatchMetricRecord(int minute, String cvConfigId) throws Exception {
    File file = new File("400-rest/src/test/resources/verification/cloudwatchAnalysis.json");
    final Gson gson = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<List<TimeSeriesMLAnalysisRecord>>() {}.getType();
      List<TimeSeriesMLAnalysisRecord> timeSeriesMLAnalysisRecords = gson.fromJson(br, type);
      timeSeriesMLAnalysisRecords.forEach(timeSeriesMLAnalysisRecord -> {
        timeSeriesMLAnalysisRecord.setAppId(appId);
        timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
        timeSeriesMLAnalysisRecord.setAnalysisMinute(minute);
      });
      wingsPersistence.save(timeSeriesMLAnalysisRecords);
    }
    File file1 = new File("400-rest/src/test/resources/verification/cloudwatchData.json");

    try (BufferedReader br = new BufferedReader(new FileReader(file1))) {
      Type type = new TypeToken<List<NewRelicMetricDataRecord>>() {}.getType();
      List<NewRelicMetricDataRecord> metricDataRecords = gson.fromJson(br, type);
      metricDataRecords.forEach(cwRecord -> {
        cwRecord.setAppId(appId);
        cwRecord.setCvConfigId(cvConfigId);
        cwRecord.setDataCollectionMinute(minute);
      });
      wingsPersistence.save(metricDataRecords);
    }
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testCloudwatchMetricType() throws Exception {
    long endTime = Timestamp.currentMinuteBoundary();
    long startTime = endTime - TimeUnit.MINUTES.toMillis(15);
    long historyStartTime = endTime - TimeUnit.MINUTES.toMillis(135);
    Map<String, List<CloudWatchMetric>> lbMetrics = new HashMap<>();
    lbMetrics.put("dummyELB",
        Arrays.asList(CloudWatchMetric.builder()
                          .metricName("EstimatedProcessedBytes")
                          .metricType(MetricType.ERROR.name())
                          .build()));
    CVConfiguration cvConfiguration = CloudWatchCVServiceConfiguration.builder().loadBalancerMetrics(lbMetrics).build();
    cvConfiguration.setUuid(generateUuid());
    cvConfiguration.setStateType(StateType.CLOUD_WATCH);

    wingsPersistence.save(cvConfiguration);

    createCloudwatchMetricRecord((int) TimeUnit.MICROSECONDS.toMinutes(startTime), cvConfiguration.getUuid());

    SortedSet<TransactionTimeSeries> timeSeries =
        continuousVerificationService
            .getTimeSeriesOfHeatMapUnitV2(TimeSeriesFilter.builder()
                                              .cvConfigId(cvConfiguration.getUuid())
                                              .startTime(startTime)
                                              .endTime(endTime)
                                              .historyStartTime(historyStartTime)
                                              .build(),
                Optional.empty(), Optional.empty())
            .getTimeSeriesSet();

    assertThat(timeSeries).isNotNull();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testWithActualData() throws IOException {
    readAndSaveAnalysisRecords();

    List<HeatMap> heatMaps =
        cvDashboardResource.getHeatMapSummary(accountId, appId, serviceId, 1541083500000L, 1541126700000L)
            .getResource();
    assertThat(heatMaps).hasSize(1);
    assertThat(heatMaps.get(0).getRiskLevelSummary()).hasSize(48);
  }

  @Test
  @Owner(developers = {PRAVEEN}, intermittent = true)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void testSortingFromDB() throws IOException {
    String cvConfigId = readAndSaveAnalysisRecords();
    long startTime = TimeUnit.MINUTES.toMillis(25685446);
    long endTime = TimeUnit.MINUTES.toMillis(25685461);
    long historyStart = TimeUnit.MINUTES.toMillis(25685326);

    RestResponse<SortedSet<TransactionTimeSeries>> timeSeries =
        cvDashboardResource.getFilteredTimeSeriesOfHeatMapUnit(accountId, cvConfigId, startTime + 1, 1541177160000L, 0,
            Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList());

    assertThat(timeSeries.getResource()).hasSize(7);
  }

  @Test
  @Owner(developers = {PRAVEEN}, intermittent = true)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void testDeeplinkUrlAppDynamicsFromDB() throws IOException {
    String cvConfigId = readAndSaveAnalysisRecords();
    long startTime = TimeUnit.MINUTES.toMillis(25685446);

    SortedSet<TransactionTimeSeries> timeSeries = continuousVerificationService
                                                      .getTimeSeriesOfHeatMapUnitV2(TimeSeriesFilter.builder()
                                                                                        .cvConfigId(cvConfigId)
                                                                                        .startTime(startTime + 1)
                                                                                        .endTime(1541177160000L)
                                                                                        .historyStartTime(0)
                                                                                        .build(),
                                                          Optional.empty(), Optional.empty())
                                                      .getTimeSeriesSet();
    assertThat(timeSeries).hasSize(7);
    TransactionTimeSeries insideTimeSeries = null;
    for (TransactionTimeSeries series : timeSeries) {
      if (series.getTransactionName().equals("/todolist/exception")) {
        insideTimeSeries = series;
        break;
      }
    }
    for (TimeSeriesOfMetric metric : insideTimeSeries.getMetricTimeSeries()) {
      if (isNotEmpty(metric.getMetricDeeplinkUrl())) {
        assertThat(metric.getMetricDeeplinkUrl().contains("https://harness-test.saas.appdynamics.com/controller/"))
            .isTrue();
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

    File file1 = new File("400-rest/src/test/resources/verification/dataForTimeSeries.json");
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

    File file = new File("400-rest/src/test/resources/verification/24_7_ts_analysis.json");
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
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
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
    File file = new File("400-rest/src/test/resources/verification/cv_24_7_analysis_record.json");
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

    assertThat(timeSeries).hasSize(9);
    ArrayList<TransactionTimeSeries> timeSeriesList = new ArrayList<>(timeSeries);
    for (int i = 0; i < 8; i++) {
      assertThat(timeSeriesList.get(i).getMetricTimeSeries().first().compareTo(
                     timeSeriesList.get(i + 1).getMetricTimeSeries().first())
          <= 0)
          .isTrue();
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

    assertThat(timeSeries).hasSize(9);
    timeSeriesList = new ArrayList<>(timeSeries);
    assertThat(timeSeriesList.get(0).getTransactionName())
        .isEqualTo(timeSeriesMLAnalysisRecord.getTransactions().get("6").getTxn_name());
    assertThat(timeSeriesList.get(1).getTransactionName())
        .isEqualTo(timeSeriesMLAnalysisRecord.getTransactions().get("4").getTxn_name());
    assertThat(timeSeriesList.get(2).getTransactionName())
        .isEqualTo(timeSeriesMLAnalysisRecord.getTransactions().get("2").getTxn_name());
    assertThat(timeSeriesList.get(3).getTransactionName())
        .isEqualTo(timeSeriesMLAnalysisRecord.getTransactions().get("9").getTxn_name());
    assertThat(timeSeriesList.get(4).getTransactionName())
        .isEqualTo(timeSeriesMLAnalysisRecord.getTransactions().get("7").getTxn_name());
    assertThat(timeSeriesList.get(5).getTransactionName())
        .isEqualTo(timeSeriesMLAnalysisRecord.getTransactions().get("3").getTxn_name());
    assertThat(timeSeriesList.get(6).getTransactionName())
        .isEqualTo(timeSeriesMLAnalysisRecord.getTransactions().get("0").getTxn_name());
    assertThat(timeSeriesList.get(7).getTransactionName())
        .isEqualTo(timeSeriesMLAnalysisRecord.getTransactions().get("8").getTxn_name());
    assertThat(timeSeriesList.get(8).getTransactionName())
        .isEqualTo(timeSeriesMLAnalysisRecord.getTransactions().get("1").getTxn_name());

    List<TimeSeriesOfMetric> metrics = new ArrayList<>(timeSeriesList.get(0).getMetricTimeSeries());
    assertThat(metrics.get(0).getMetricName())
        .isEqualTo(timeSeriesMLAnalysisRecord.getTransactions().get("6").getMetrics().get("1").getMetric_name());
    assertThat(metrics.get(1).getMetricName())
        .isEqualTo(timeSeriesMLAnalysisRecord.getTransactions().get("6").getMetrics().get("0").getMetric_name());
    assertThat(metrics.get(2).getMetricName())
        .isEqualTo(timeSeriesMLAnalysisRecord.getTransactions().get("6").getMetrics().get("2").getMetric_name());
  }

  @Test
  @Owner(developers = PRAVEEN)
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

    File file1 = new File("400-rest/src/test/resources/verification/metricsForRisk.json");
    final Gson gson1 = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file1))) {
      Type type = new TypeToken<List<NewRelicMetricDataRecord>>() {}.getType();
      List<NewRelicMetricDataRecord> metricDataRecords = gson1.fromJson(br, type);
      metricDataRecords.forEach(metricDataRecord -> {
        metricDataRecord.setAppId(appId);
        metricDataRecord.setCvConfigId(cvConfigId);
      });
      final List<TimeSeriesDataRecord> dataRecords =
          TimeSeriesDataRecord.getTimeSeriesDataRecordsFromNewRelicDataRecords(metricDataRecords);
      dataRecords.forEach(TimeSeriesDataRecord::compress);

      wingsPersistence.save(dataRecords);
    }

    File file = new File("400-rest/src/test/resources/verification/24_7_ts_analysis.json");
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
        continuousVerificationService
            .getTimeSeriesOfHeatMapUnitV2(TimeSeriesFilter.builder()
                                              .cvConfigId(cvConfigId)
                                              .startTime(startTime + 1)
                                              .endTime(endTime)
                                              .historyStartTime(historyStart + 1)
                                              .build(),
                Optional.empty(), Optional.empty())
            .getTimeSeriesSet();
    assertThat(timeseries).hasSize(5);
    assertThat(timeseries.first().getMetricTimeSeries()).isNotNull();
    assertThat(timeseries.first().getMetricTimeSeries().first().getRisksForTimeSeries()).hasSize(9);
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
    assertThat(timeSeriesRisk.getRisk()).isEqualTo(2);
    assertThat(timeSeriesRisk.getEndTime()).isEqualTo(TimeUnit.MINUTES.toMillis(25685341));
    assertThat(TimeUnit.MINUTES.toMillis(25685341 - CRON_POLL_INTERVAL_IN_MINUTES) + 1)
        .isEqualTo(timeSeriesRisk.getStartTime());
  }

  @Test
  @Owner(developers = UJJAWAL)
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

    File file1 = new File("400-rest/src/test/resources/verification/24x7_ts_transactionMetricRisk_MetricRecords");
    final Gson gson1 = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file1))) {
      Type type = new TypeToken<List<NewRelicMetricDataRecord>>() {}.getType();
      List<NewRelicMetricDataRecord> metricDataRecords = gson1.fromJson(br, type);
      metricDataRecords.forEach(metricDataRecord -> {
        metricDataRecord.setAppId(appId);
        metricDataRecord.setCvConfigId(cvConfigId);
        metricDataRecord.setStateType(StateType.APP_DYNAMICS);
      });
      wingsPersistence.save(metricDataRecords);

      final List<TimeSeriesDataRecord> dataRecords =
          TimeSeriesDataRecord.getTimeSeriesDataRecordsFromNewRelicDataRecords(metricDataRecords);
      dataRecords.forEach(TimeSeriesDataRecord::compress);

      wingsPersistence.save(dataRecords);
    }

    File file = new File("400-rest/src/test/resources/verification/24_7_ts_transactionMetricRisk_regression.json");
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
    SortedSet<TransactionTimeSeries> timeseries = continuousVerificationService
                                                      .getTimeSeriesOfHeatMapUnitV2(TimeSeriesFilter.builder()
                                                                                        .cvConfigId(cvConfigId)
                                                                                        .startTime(startTime)
                                                                                        .endTime(endTime)
                                                                                        .historyStartTime(historyStart)
                                                                                        .build(),
                                                          Optional.of(0), Optional.of(1000))
                                                      .getTimeSeriesSet();
    for (TransactionTimeSeries s : timeseries) {
      for (TimeSeriesOfMetric tms : s.getMetricTimeSeries()) {
        if (tms.isLongTermPattern()) {
          longterm = true;
        }
      }
    }
    assertThat(longterm).isTrue();
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

    assertThat(apiArtifactsRespTimeTS.getRisk()).isEqualTo(-1);

    List<Double> actualTimeSeries = new ArrayList<>();
    for (TimeSeriesDataPoint datapoint : apiArtifactsRespTimeTS.getTimeSeries()) {
      actualTimeSeries.add(datapoint.getValue());
    }
    assertThat(actualTimeSeries).hasSize(135);
    assertThat(actualTimeSeries).isEqualTo(expectedTimeSeries);
  }

  // Test to be un-ignored as per https://harness.atlassian.net/browse/LE-1150
  @Test
  @Owner(developers = RAGHU, intermittent = true)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
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

    File file1 = new File("400-rest/src/test/resources/verification/metricsForRisk.json");
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

    File file = new File("400-rest/src/test/resources/verification/24_7_ts_analysis.json");
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
        continuousVerificationService
            .getTimeSeriesOfHeatMapUnitV2(TimeSeriesFilter.builder()
                                              .cvConfigId(cvConfigId)
                                              .startTime(startTime + 1)
                                              .endTime(endTime)
                                              .historyStartTime(historyStart + 1)
                                              .build(),
                Optional.empty(), Optional.empty())
            .getTimeSeriesSet();
    for (Iterator<TransactionTimeSeries> txnIterator = timeseries.iterator(); txnIterator.hasNext();) {
      TransactionTimeSeries txnTimeSeries = txnIterator.next();
      for (Iterator<TimeSeriesOfMetric> metricTSIterator = txnTimeSeries.getMetricTimeSeries().iterator();
           metricTSIterator.hasNext();) {
        TimeSeriesOfMetric metricTimeSeries = metricTSIterator.next();
        SortedMap<Long, TimeSeriesDataPoint> datapoints = (SortedMap) metricTimeSeries.getTimeSeriesMap();
        for (TimeSeriesRisk tsRisk : metricTimeSeries.getRisksForTimeSeries()) {
          long startTimeOfRisk = tsRisk.getStartTime();
          long endTimeOfRisk = tsRisk.getEndTime();
          assertThat(datapoints.containsKey(startTimeOfRisk + TimeUnit.MINUTES.toMillis(1) - 1)).isTrue();
          assertThat(datapoints.containsKey(endTimeOfRisk)).isTrue();
        }
      }
    }
  }

  @Test
  @Owner(developers = RAGHU)
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

    saveMetricRecords(cvConfigId);

    File file = new File("400-rest/src/test/resources/verification/multi-risk-sorting.json");
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
      assertThat(timeSeriesList.get(i).getMetricTimeSeries().first().getRisk()
          >= timeSeriesList.get(i + 1).getMetricTimeSeries().first().getRisk())
          .isTrue();
    }
    assertThat(timeSeriesList.get(0).getTransactionName()).isEqualTo("/api/setup-as-code");
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
  @Owner(developers = RAGHU)
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

    saveMetricRecords(cvConfigId);

    final SortedSet<TransactionTimeSeries> timeSeries =
        cvDashboardResource
            .getFilteredTimeSeriesOfHeatMapUnit(accountId, cvConfigId, 1541678400000L, 1541685600000L, 0,
                Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList())
            .getResource();
    assertThat(timeSeries).hasSize(43);
  }

  @Test
  @Owner(developers = RAGHU)
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

    saveMetricRecords(cvConfigId);

    final SortedSet<TransactionTimeSeries> timeSeries =
        cvDashboardResource
            .getFilteredTimeSeriesOfHeatMapUnit(accountId, cvConfigId, 1541678400000L, 1541685600000L, 0,
                Lists.newArrayList("/api/setup-as-code", "/api/infrastructure-mappings", "/api/userGroups"),
                Lists.newArrayList(), Lists.newArrayList())
            .getResource();
    List<TransactionTimeSeries> timeSeriesList = new ArrayList<>(timeSeries);
    assertThat(timeSeries).hasSize(3);
    assertThat(timeSeriesList.get(0).getTransactionName()).isEqualTo("/api/userGroups");
    assertThat(timeSeriesList.get(0).getMetricTimeSeries()).hasSize(1);
    assertThat(timeSeriesList.get(0).getMetricTimeSeries().first().getMetricName())
        .isEqualTo("95th Percentile Response Time (ms)");

    assertThat(timeSeriesList.get(1).getTransactionName()).isEqualTo("/api/setup-as-code");
    assertThat(timeSeriesList.get(1).getMetricTimeSeries()).hasSize(2);
    assertThat(timeSeriesList.get(1).getMetricTimeSeries().first().getMetricName())
        .isEqualTo("95th Percentile Response Time (ms)");
    assertThat(timeSeriesList.get(1).getMetricTimeSeries().last().getMetricName()).isEqualTo("Number of Slow Calls");

    assertThat(timeSeriesList.get(2).getTransactionName()).isEqualTo("/api/infrastructure-mappings");
    assertThat(timeSeriesList.get(2).getMetricTimeSeries()).hasSize(2);
    assertThat(timeSeriesList.get(2).getMetricTimeSeries().first().getMetricName())
        .isEqualTo("95th Percentile Response Time (ms)");
    assertThat(timeSeriesList.get(2).getMetricTimeSeries().last().getMetricName()).isEqualTo("Number of Slow Calls");
  }

  @Test
  @Owner(developers = RAGHU)
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

    saveMetricRecords(cvConfigId);

    SortedSet<TransactionTimeSeries> timeSeries =
        cvDashboardResource
            .getFilteredTimeSeriesOfHeatMapUnit(accountId, cvConfigId, 1541678400000L, 1541685600000L, 0,
                Lists.newArrayList(), Lists.newArrayList("95th Percentile Response Time (ms)"), Lists.newArrayList())
            .getResource();
    assertThat(timeSeries).hasSize(43);
    timeSeries.forEach(transactionTimeSeries -> {
      assertThat(transactionTimeSeries.getMetricTimeSeries()).hasSize(1);
      assertThat(transactionTimeSeries.getMetricTimeSeries().first().getMetricName())
          .isEqualTo("95th Percentile Response Time (ms)");
    });

    timeSeries = cvDashboardResource
                     .getFilteredTimeSeriesOfHeatMapUnit(accountId, cvConfigId, 1541678400000L, 1541685600000L, 0,
                         Lists.newArrayList(), Lists.newArrayList("Number of Slow Calls"), Lists.newArrayList())
                     .getResource();
    assertThat(timeSeries).hasSize(17);
    timeSeries.forEach(transactionTimeSeries -> {
      assertThat(transactionTimeSeries.getMetricTimeSeries()).hasSize(1);
      assertThat(transactionTimeSeries.getMetricTimeSeries().first().getMetricName()).isEqualTo("Number of Slow Calls");
    });
  }

  @Test
  @Owner(developers = RAGHU)
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

    saveMetricRecords(cvConfigId);

    SortedSet<TransactionTimeSeries> timeSeries =
        cvDashboardResource
            .getFilteredTimeSeriesOfHeatMapUnit(accountId, cvConfigId, 1541678400000L, 1541685600000L, 0,
                Lists.newArrayList("/api/setup-as-code", "/api/infrastructure-mappings"),
                Lists.newArrayList("95th Percentile Response Time (ms)", "Number of Slow Calls"), Lists.newArrayList())
            .getResource();
    assertThat(timeSeries).hasSize(2);
    assertThat(timeSeries.first().getTransactionName()).isEqualTo("/api/setup-as-code");
    assertThat(timeSeries.last().getTransactionName()).isEqualTo("/api/infrastructure-mappings");
    timeSeries.forEach(transactionTimeSeries -> {
      assertThat(transactionTimeSeries.getMetricTimeSeries()).hasSize(2);
      assertThat(transactionTimeSeries.getMetricTimeSeries().first().getMetricName())
          .isEqualTo("95th Percentile Response Time (ms)");
      assertThat(transactionTimeSeries.getMetricTimeSeries().last().getMetricName()).isEqualTo("Number of Slow Calls");
    });

    timeSeries = cvDashboardResource
                     .getFilteredTimeSeriesOfHeatMapUnit(accountId, cvConfigId, 1541678400000L, 1541685600000L, 0,
                         Lists.newArrayList("/api/setup-as-code", "/api/infrastructure-mappings"),
                         Lists.newArrayList("Number of Slow Calls"), Lists.newArrayList())
                     .getResource();

    assertThat(timeSeries).hasSize(2);
    assertThat(timeSeries.first().getTransactionName()).isEqualTo("/api/setup-as-code");
    assertThat(timeSeries.last().getTransactionName()).isEqualTo("/api/infrastructure-mappings");
    timeSeries.forEach(transactionTimeSeries -> {
      assertThat(transactionTimeSeries.getMetricTimeSeries()).hasSize(1);
      assertThat(transactionTimeSeries.getMetricTimeSeries().first().getMetricName()).isEqualTo("Number of Slow Calls");
    });
  }

  private void saveMetricRecords(String cvConfigId) throws IOException {
    File file1 = new File("400-rest/src/test/resources/verification/metricRecords.json");
    final Gson gson1 = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file1))) {
      Type type = new TypeToken<List<NewRelicMetricDataRecord>>() {}.getType();
      List<NewRelicMetricDataRecord> metricDataRecords = gson1.fromJson(br, type);
      metricDataRecords.forEach(metricDataRecord -> {
        metricDataRecord.setAppId(appId);
        metricDataRecord.setCvConfigId(cvConfigId);
        metricDataRecord.setStateType(StateType.APP_DYNAMICS);
      });

      final List<TimeSeriesDataRecord> dataRecords =
          TimeSeriesDataRecord.getTimeSeriesDataRecordsFromNewRelicDataRecords(metricDataRecords);
      dataRecords.forEach(TimeSeriesDataRecord::compress);

      wingsPersistence.save(dataRecords);
    }
  }
}
