/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.NANDAN;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;
import static software.wings.common.VerificationConstants.PREDECTIVE_HISTORY_MINUTES;

import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.security.EncryptionUtils;
import io.harness.time.Timestamp;

import software.wings.FeatureTestHelper;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.AnalysisServiceImpl.CLUSTER_TYPE;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.CVFeedbackRecord;
import software.wings.service.impl.analysis.FeedbackAction;
import software.wings.service.impl.analysis.FeedbackPriority;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisStatus;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogMLClusterSummary;
import software.wings.service.impl.analysis.LogMLFeedbackSummary;
import software.wings.service.impl.analysis.LogMLHostSummary;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.service.impl.splunk.SplunkAnalysisCluster.MessageFrequency;
import software.wings.service.intfc.verification.CV24x7DashboardService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.HeatMap;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Created by Praveen
 */
public class CV24x7DashboardServiceTest extends WingsBaseTest {
  private static final SecureRandom random = new SecureRandom();

  @Inject WingsPersistence wingsPersistence;
  @Inject CV24x7DashboardService cv24x7DashboardService;
  @Inject CVConfigurationService cvConfigurationService;
  @Inject private FeatureTestHelper featureTestHelper;

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

  private String createDDCVConfig() {
    DatadogCVServiceConfiguration cvServiceConfiguration = DatadogCVServiceConfiguration.builder().build();

    Map<String, String> dockerMetrics = new HashMap<>();
    dockerMetrics.put("service_name:harness", "docker.cpu.usage, docker.mem.rss");
    cvServiceConfiguration.setDockerMetrics(dockerMetrics);

    Map<String, String> ecsMetrics = new HashMap<>();
    ecsMetrics.put("service_name:harness", "ecs.fargate.cpu.user");
    cvServiceConfiguration.setEcsMetrics(ecsMetrics);

    cvServiceConfiguration.setName(generateUUID());
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.LOW);
    return cvConfigurationService.saveConfiguration(accountId, appId, StateType.DATA_DOG, cvServiceConfiguration);
  }

  private String createNRConfig() {
    NewRelicCVServiceConfiguration cvServiceConfiguration =
        NewRelicCVServiceConfiguration.builder().applicationId(generateUUID()).build();
    cvServiceConfiguration.setName(generateUUID());
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.LOW);
    return cvConfigurationService.saveConfiguration(accountId, appId, StateType.NEW_RELIC, cvServiceConfiguration);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetTagsForCvConfig() throws Exception {
    String cvConfigId = createDDCVConfig();

    // test behavior

    Map<String, Double> result = cv24x7DashboardService.getMetricTags(accountId, appId, cvConfigId, 0l, 0l);

    // assert
    assertThat(result).hasSize(2);
    assertThat(result.containsKey("Docker")).isTrue();
    assertThat(result.containsKey("ECS")).isTrue();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetTagsForCvConfigNoTags() throws Exception {
    String cvConfigId = createNRConfig();

    // test behavior
    Map<String, Double> result = cv24x7DashboardService.getMetricTags(accountId, appId, cvConfigId, 0l, 0l);

    // assert
    assertThat(result).hasSize(0);
  }

  private LogMLAnalysisRecord buildAnalysisRecord(
      int analysisMin, LogMLAnalysisStatus status, String cvConfigId, Optional<String> feedbackId) {
    LogMLAnalysisRecord firstRec = LogMLAnalysisRecord.builder()
                                       .uuid(generateUuid())
                                       .cvConfigId(cvConfigId)
                                       .accountId(accountId)
                                       .logCollectionMinute(analysisMin)
                                       .appId(appId)
                                       .build();

    Map<String, Map<String, SplunkAnalysisCluster>> unknownClusters = new HashMap<>();
    List<SplunkAnalysisCluster> clusterEvents = new ArrayList<>();
    Set<String> hosts = new HashSet<>();
    for (int i = 0; i < 10; i++) {
      SplunkAnalysisCluster cluster = getRandomClusterEvent();
      cluster.setCluster_label(i);
      if (feedbackId.isPresent()) {
        cluster.setFeedback_id(feedbackId.get());
      }
      clusterEvents.add(cluster);
      Map<String, SplunkAnalysisCluster> hostMap = new HashMap<>();
      String host = UUID.randomUUID().toString() + ".harness.com";
      hostMap.put(host, cluster);
      hosts.add(host);
      unknownClusters.put(UUID.randomUUID().toString(), hostMap);
    }
    firstRec.setAnalysisStatus(status);
    firstRec.setUnknown_clusters(unknownClusters);
    firstRec.compressLogAnalysisRecord();
    return firstRec;
  }

  private SplunkAnalysisCluster getRandomClusterEvent() {
    SplunkAnalysisCluster analysisCluster = new SplunkAnalysisCluster();
    analysisCluster.setCluster_label(random.nextInt(100));
    analysisCluster.setAnomalous_counts(
        Lists.newArrayList(random.nextInt(100), random.nextInt(100), random.nextInt(100)));
    analysisCluster.setText(UUID.randomUUID().toString());
    analysisCluster.setTags(
        Lists.newArrayList(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    analysisCluster.setDiff_tags(
        Lists.newArrayList(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    analysisCluster.setX(random.nextDouble());
    analysisCluster.setY(random.nextDouble());
    analysisCluster.setUnexpected_freq(random.nextBoolean());
    List<MessageFrequency> frequencyMapList = new ArrayList<>();
    for (int i = 0; i < 1 + random.nextInt(10); i++) {
      frequencyMapList.add(MessageFrequency.builder().count(random.nextInt(100)).build());
    }

    analysisCluster.setMessage_frequencies(frequencyMapList);
    return analysisCluster;
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetAnalysisSummary() throws Exception {
    String cvConfigId = generateUuid();
    LogsCVConfiguration sumoCOnfig = new LogsCVConfiguration();
    sumoCOnfig.setAppId(appId);
    sumoCOnfig.setQuery("exception");
    sumoCOnfig.setStateType(StateType.SUMO);
    sumoCOnfig.setUuid(cvConfigId);

    wingsPersistence.save(sumoCOnfig);
    long endTime = Timestamp.currentMinuteBoundary() - TimeUnit.MINUTES.toMillis(10);
    long startTime = Timestamp.currentMinuteBoundary() - TimeUnit.MINUTES.toMillis(25);

    int firstRecMinute = (int) TimeUnit.MILLISECONDS.toMinutes(startTime) - 15;
    int secondRecMinute = (int) TimeUnit.MILLISECONDS.toMinutes(endTime);

    wingsPersistence.save(
        buildAnalysisRecord(firstRecMinute, LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE, cvConfigId, Optional.empty()));
    wingsPersistence.save(
        buildAnalysisRecord(secondRecMinute, LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE, cvConfigId, Optional.empty()));
    wingsPersistence.save(buildAnalysisRecord(
        secondRecMinute, LogMLAnalysisStatus.FEEDBACK_ANALYSIS_COMPLETE, cvConfigId, Optional.empty()));

    LogMLAnalysisSummary summary = cv24x7DashboardService.getAnalysisSummary(cvConfigId, startTime, endTime, appId);

    assertThat(summary.getUnknownClusters()).hasSize(10);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testFeedbackSummary() {
    String cvConfigId = generateUuid();
    createAndSaveSumoConfig(cvConfigId, true);
    featureTestHelper.disableFeatureFlag(FeatureName.DISABLE_LOGML_NEURAL_NET);

    long endTime = Timestamp.currentMinuteBoundary() - TimeUnit.MINUTES.toMillis(10);
    long startTime = Timestamp.currentMinuteBoundary() - TimeUnit.MINUTES.toMillis(25);
    int firstRecMinute = (int) TimeUnit.MILLISECONDS.toMinutes(startTime) - 15;
    int secondRecMinute = (int) TimeUnit.MILLISECONDS.toMinutes(endTime);
    CVFeedbackRecord record = createFeedbackRecordInDB(cvConfigId, firstRecMinute);
    wingsPersistence.save(
        buildAnalysisRecord(secondRecMinute, LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE, cvConfigId, Optional.empty()));
    LogMLAnalysisRecord analysisRecord = buildAnalysisRecord(
        secondRecMinute, LogMLAnalysisStatus.FEEDBACK_ANALYSIS_COMPLETE, cvConfigId, Optional.of(record.getUuid()));

    wingsPersistence.save(analysisRecord);

    LogMLAnalysisSummary summary = cv24x7DashboardService.getAnalysisSummary(cvConfigId, startTime, endTime, appId);

    assertThat(summary.getUnknownClusters()).hasSize(10);
    assertThat(summary.getUnknownClusters().get(0).getLogMLFeedbackId()).isEqualTo(record.getUuid());
    assertThat(summary.getUnknownClusters().get(0).getFeedbackSummary()).isNotNull();
    assertThat(summary.getUnknownClusters().get(0).getFeedbackSummary().getJiraLink()).isEqualTo("testJiraLink");
  }

  private LogsCVConfiguration createAndSaveSumoConfig(String cvConfigId, boolean enabled247SG) {
    LogsCVConfiguration sumoCOnfig = new LogsCVConfiguration();
    sumoCOnfig.setAppId(appId);
    sumoCOnfig.setServiceId(serviceId);
    sumoCOnfig.setQuery("exception");
    sumoCOnfig.setStateType(StateType.SUMO);
    sumoCOnfig.setUuid(cvConfigId);
    sumoCOnfig.setEnabled24x7(enabled247SG);
    wingsPersistence.save(sumoCOnfig);
    return sumoCOnfig;
  }

  private CVFeedbackRecord createFeedbackRecordInDB(String cvConfigId, long analysisMin) {
    String uuid = generateUuid();
    CVFeedbackRecord feedbackRecord =
        CVFeedbackRecord.builder()
            .accountId(accountId)
            .cvConfigId(cvConfigId)
            .serviceId(serviceId)
            .analysisMinute(analysisMin)
            .clusterType(CLUSTER_TYPE.UNKNOWN)
            .actionTaken(FeedbackAction.UPDATE_PRIORITY)
            .priority(FeedbackPriority.P4)
            .lastUpdatedBy(EmbeddedUser.builder().name("HarnessTestUser").uuid(generateUuid()).build())
            .lastUpdatedAt(System.currentTimeMillis())
            .jiraLink("testJiraLink")
            .uuid(uuid)
            .build();

    wingsPersistence.save(feedbackRecord);
    return feedbackRecord;
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetAnalysisSummaryFeedbackData() throws Exception {
    String cvConfigId = generateUuid();
    createAndSaveSumoConfig(cvConfigId, true);
    featureTestHelper.disableFeatureFlag(FeatureName.DISABLE_LOGML_NEURAL_NET);

    long endTime = Timestamp.currentMinuteBoundary() - TimeUnit.MINUTES.toMillis(10);
    long startTime = Timestamp.currentMinuteBoundary() - TimeUnit.MINUTES.toMillis(25);
    int firstRecMinute = (int) TimeUnit.MILLISECONDS.toMinutes(startTime) - 15;
    int secondRecMinute = (int) TimeUnit.MILLISECONDS.toMinutes(endTime);

    // create feedbacks
    CVFeedbackRecord feedbackRecord = createFeedbackRecordInDB(cvConfigId, secondRecMinute);

    wingsPersistence.save(
        buildAnalysisRecord(firstRecMinute, LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE, cvConfigId, Optional.empty()));
    wingsPersistence.save(
        buildAnalysisRecord(secondRecMinute, LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE, cvConfigId, Optional.empty()));
    wingsPersistence.save(buildAnalysisRecord(
        secondRecMinute, LogMLAnalysisStatus.FEEDBACK_ANALYSIS_COMPLETE, cvConfigId, Optional.empty()));

    LogMLAnalysisSummary summary = cv24x7DashboardService.getAnalysisSummary(cvConfigId, startTime, endTime, appId);

    assertThat(summary.getUnknownClusters()).hasSize(10);
    LogMLClusterSummary feedbackCluster = null;
    for (LogMLClusterSummary clusterSummary : summary.getUnknownClusters()) {
      if (isNotEmpty(clusterSummary.getLogMLFeedbackId())) {
        feedbackCluster = clusterSummary;
        break;
      }
    }

    assertThat(feedbackCluster).isNotNull();
    LogMLFeedbackSummary feedbackSummary = feedbackCluster.getFeedbackSummary();
    assertThat(feedbackSummary).isNotNull();
    assertThat(feedbackSummary.getPriority().name()).isEqualTo(feedbackRecord.getPriority().name());
    assertThat(feedbackSummary.getLastUpdatedBy()).isEqualTo(feedbackRecord.getLastUpdatedBy());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetAnalysisSummaryAcrossFFBorder() throws Exception {
    String cvConfigId = generateUuid();
    LogsCVConfiguration sumoCOnfig = new LogsCVConfiguration();
    sumoCOnfig.setAppId(appId);
    sumoCOnfig.setQuery("exception");
    sumoCOnfig.setStateType(StateType.SUMO);
    sumoCOnfig.setUuid(cvConfigId);

    wingsPersistence.save(sumoCOnfig);
    long endTime = Timestamp.currentMinuteBoundary() - TimeUnit.MINUTES.toMillis(10);
    long startTime = Timestamp.currentMinuteBoundary() - TimeUnit.MINUTES.toMillis(40);

    int firstRecMinute = (int) TimeUnit.MILLISECONDS.toMinutes(startTime) + 15;
    int secondRecMinute = (int) TimeUnit.MILLISECONDS.toMinutes(endTime);

    wingsPersistence.save(
        buildAnalysisRecord(firstRecMinute, LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE, cvConfigId, Optional.empty()));
    wingsPersistence.save(
        buildAnalysisRecord(secondRecMinute, LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE, cvConfigId, Optional.empty()));
    wingsPersistence.save(buildAnalysisRecord(
        secondRecMinute, LogMLAnalysisStatus.FEEDBACK_ANALYSIS_COMPLETE, cvConfigId, Optional.empty()));

    LogMLAnalysisSummary summary = cv24x7DashboardService.getAnalysisSummary(cvConfigId, startTime, endTime, appId);

    assertThat(summary.getUnknownClusters()).hasSize(20);
  }

  private void createSumoConfigWithBaseline(String cvConfigId, long currentTime, long baselineEndDelta) {
    LogsCVConfiguration sumoCOnfig = new LogsCVConfiguration();
    sumoCOnfig.setAppId(appId);
    sumoCOnfig.setQuery("exception");
    sumoCOnfig.setStateType(StateType.SUMO);
    sumoCOnfig.setUuid(cvConfigId);
    sumoCOnfig.setBaselineStartMinute(currentTime - baselineEndDelta * 2);
    sumoCOnfig.setBaselineEndMinute(currentTime - baselineEndDelta);
    wingsPersistence.save(sumoCOnfig);
  }
  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCurrentWindowLogsHappyCase() {
    long currentTime = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());

    String cvConfigId = generateUuid();
    createSumoConfigWithBaseline(cvConfigId, currentTime, 30);

    LogMLAnalysisRecord previousAnalysis = LogMLAnalysisRecord.builder()
                                               .cvConfigId(cvConfigId)
                                               .accountId(accountId)
                                               .logCollectionMinute((int) currentTime - 15)
                                               .query("exception")
                                               .build();

    wingsPersistence.save(previousAnalysis);

    long currentEndTime = cv24x7DashboardService.getCurrentAnalysisWindow(cvConfigId);
    assertThat(currentEndTime).isEqualTo(TimeUnit.MINUTES.toMillis(currentTime));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCurrentWindowLogsNoPreviousAnalysis() {
    long currentTime = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    String cvConfigId = generateUuid();
    createSumoConfigWithBaseline(cvConfigId, currentTime, 30);
    long currentEndTime = cv24x7DashboardService.getCurrentAnalysisWindow(cvConfigId);
    LogsCVConfiguration config = (LogsCVConfiguration) cvConfigurationService.getConfiguration(cvConfigId);
    assertThat(currentEndTime).isEqualTo(TimeUnit.MINUTES.toMillis(config.getBaselineStartMinute() + 15));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCurrentWindowLogsMoreThan2HoursSinceLast() {
    long currentTime = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    String cvConfigId = generateUuid();
    createSumoConfigWithBaseline(cvConfigId, currentTime, 150);

    LogMLAnalysisRecord previousAnalysis = LogMLAnalysisRecord.builder()
                                               .cvConfigId(cvConfigId)
                                               .accountId(accountId)
                                               .logCollectionMinute((int) currentTime - 135)
                                               .query("exception")
                                               .build();
    wingsPersistence.save(previousAnalysis);
    long currentEndTime = cv24x7DashboardService.getCurrentAnalysisWindow(cvConfigId);
    long expectedCurrentWindowEnd = currentTime - PREDECTIVE_HISTORY_MINUTES + CRON_POLL_INTERVAL_IN_MINUTES;
    if (Math.floorMod(expectedCurrentWindowEnd - 1, CRON_POLL_INTERVAL_IN_MINUTES) != 0) {
      expectedCurrentWindowEnd -= Math.floorMod(expectedCurrentWindowEnd - 1, CRON_POLL_INTERVAL_IN_MINUTES);
    }

    assertThat(currentEndTime).isEqualTo(TimeUnit.MINUTES.toMillis(expectedCurrentWindowEnd));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCurrentWindowLogsWithinBaselineWindow() {
    long currentTime = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    String cvConfigId = generateUuid();
    createSumoConfigWithBaseline(cvConfigId, currentTime, 150);

    LogMLAnalysisRecord previousAnalysis = LogMLAnalysisRecord.builder()
                                               .cvConfigId(cvConfigId)
                                               .accountId(accountId)
                                               .logCollectionMinute((int) currentTime - 200)
                                               .query("exception")
                                               .build();
    wingsPersistence.save(previousAnalysis);
    long currentEndTime = cv24x7DashboardService.getCurrentAnalysisWindow(cvConfigId);
    long expectedCurrentWindowEnd = currentTime - 200 + CRON_POLL_INTERVAL_IN_MINUTES;
    assertThat(currentEndTime).isEqualTo(TimeUnit.MINUTES.toMillis(expectedCurrentWindowEnd));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCurrentWindowTimeSeriesHappyCase() {
    long currentTime = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    String cvConfigId = createNRConfig();

    TimeSeriesMLAnalysisRecord record = TimeSeriesMLAnalysisRecord.builder().build();
    record.setCvConfigId(cvConfigId);
    record.setAnalysisMinute((int) currentTime - CRON_POLL_INTERVAL_IN_MINUTES * 2);
    wingsPersistence.save(record);

    long currentEndTime = cv24x7DashboardService.getCurrentAnalysisWindow(cvConfigId);

    assertThat(currentEndTime).isEqualTo(TimeUnit.MINUTES.toMillis(currentTime - CRON_POLL_INTERVAL_IN_MINUTES));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCurrentWindowTimeSeriesNoPreviousAnalysis() {
    String cvConfigId = createNRConfig();
    long currentTime = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    long currentEndTime = cv24x7DashboardService.getCurrentAnalysisWindow(cvConfigId);
    assertThat(currentEndTime).isEqualTo(TimeUnit.MINUTES.toMillis(currentTime));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCurrentWindowTimeSeriesMoreThan2HoursSinceLastAnalysis() {
    long currentTime = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    String cvConfigId = createNRConfig();

    TimeSeriesMLAnalysisRecord record = TimeSeriesMLAnalysisRecord.builder().build();
    record.setCvConfigId(cvConfigId);
    record.setAnalysisMinute((int) currentTime - PREDECTIVE_HISTORY_MINUTES * 2);
    wingsPersistence.save(record);

    long currentEndTime = cv24x7DashboardService.getCurrentAnalysisWindow(cvConfigId);

    assertThat(currentEndTime)
        .isEqualTo(TimeUnit.MINUTES.toMillis(currentTime - PREDECTIVE_HISTORY_MINUTES + CRON_POLL_INTERVAL_IN_MINUTES));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetHeatmapForEnabledConfigsOnly() {
    String id1 = generateUuid(), id2 = generateUuid();
    createAndSaveSumoConfig(id1, true);
    createAndSaveSumoConfig(id2, false);

    LogMLAnalysisRecord record =
        LogMLAnalysisRecord.builder()
            .appId(appId)
            .cvConfigId(id1)
            .accountId(accountId)
            .logCollectionMinute((int) TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary()) - 30)
            .score(1)
            .build();

    wingsPersistence.save(record);

    List<HeatMap> heatMaps = cv24x7DashboardService.getHeatMapForLogs(accountId, appId, serviceId,
        Timestamp.currentMinuteBoundary() - TimeUnit.MINUTES.toMillis(120), Timestamp.currentMinuteBoundary(), false);

    assertThat(heatMaps).isNotEmpty();
    assertThat(heatMaps.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = NANDAN)
  @Category(UnitTests.class)
  public void testUpdateClustersFrequencyMapV2_checkIfClustersHostFrequencyMapRepresentsFrequencyForLogsV2() {
    String cvConfigId = generateUuid();

    LogsCVConfiguration cvConfiguration = createAndSaveSumoConfig(cvConfigId, true);

    cvConfiguration.set247LogsV2(true);

    wingsPersistence.save(cvConfiguration);

    long endTime = Timestamp.currentMinuteBoundary() - TimeUnit.MINUTES.toMillis(10);
    long startTime = Timestamp.currentMinuteBoundary() - TimeUnit.MINUTES.toMillis(25);

    int analysisRecMinute = (int) TimeUnit.MILLISECONDS.toMinutes(startTime) + 5;

    wingsPersistence.save(
        buildAnalysisRecord(analysisRecMinute, LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE, cvConfigId, Optional.empty()));

    LogMLAnalysisSummary summary = cv24x7DashboardService.getAnalysisSummary(cvConfigId, startTime, endTime, appId);

    List<LogMLClusterSummary> summaryUnknownClusters = summary.getUnknownClusters();
    for (LogMLClusterSummary unknownCluster : summaryUnknownClusters) {
      for (Map.Entry<String, LogMLHostSummary> hostEntry : unknownCluster.getHostSummary().entrySet()) {
        List<Integer> frequencies = hostEntry.getValue().getFrequencies();
        List<Integer> frequencyMapValues = new ArrayList(hostEntry.getValue().getFrequencyMap().values());
        assertThat(frequencies).isEqualTo(frequencyMapValues);
      }
    }
  }
}
