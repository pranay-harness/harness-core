package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.time.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptionUtils;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisStatus;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.service.impl.splunk.SplunkAnalysisCluster.MessageFrequency;
import software.wings.service.intfc.verification.CV24x7DashboardService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by Praveen
 */
public class CV24x7DashboardServiceTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;
  @Inject CV24x7DashboardService cv24x7DashboardService;
  @Inject CVConfigurationService cvConfigurationService;

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
  @Category(UnitTests.class)
  public void testGetTagsForCvConfig() throws Exception {
    String cvConfigId = createDDCVConfig();

    // test behavior

    Map<String, Double> result = cv24x7DashboardService.getMetricTags(accountId, appId, cvConfigId, 0l, 0l);

    // assert
    assertEquals("There are 2 tags", 2, result.size());
    assertTrue("Docker is one of the tags", result.containsKey("Docker"));
    assertTrue("ECS is one of the tags", result.containsKey("ECS"));
  }

  @Test
  @Category(UnitTests.class)
  public void testGetTagsForCvConfigNoTags() throws Exception {
    String cvConfigId = createNRConfig();

    // test behavior
    Map<String, Double> result = cv24x7DashboardService.getMetricTags(accountId, appId, cvConfigId, 0l, 0l);

    // assert
    assertEquals("There are 0 tags", 0, result.size());
  }

  private LogMLAnalysisRecord buildAnalysisRecord(int analysisMin, LogMLAnalysisStatus status, String cvConfigId) {
    LogMLAnalysisRecord firstRec = LogMLAnalysisRecord.builder()
                                       .uuid(generateUuid())
                                       .cvConfigId(cvConfigId)
                                       .logCollectionMinute(analysisMin)
                                       .appId(appId)
                                       .build();

    Map<String, Map<String, SplunkAnalysisCluster>> unknownClusters = new HashMap<>();
    List<SplunkAnalysisCluster> clusterEvents = new ArrayList<>();
    Set<String> hosts = new HashSet<>();
    for (int i = 0; i < 10; i++) {
      SplunkAnalysisCluster cluster = getRandomClusterEvent();
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
    Random r = new Random(System.currentTimeMillis());
    SplunkAnalysisCluster analysisCluster = new SplunkAnalysisCluster();
    analysisCluster.setCluster_label(r.nextInt(100));
    analysisCluster.setAnomalous_counts(Lists.newArrayList(r.nextInt(100), r.nextInt(100), r.nextInt(100)));
    analysisCluster.setText(UUID.randomUUID().toString());
    analysisCluster.setTags(
        Lists.newArrayList(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    analysisCluster.setDiff_tags(
        Lists.newArrayList(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    analysisCluster.setX(r.nextDouble());
    analysisCluster.setY(r.nextDouble());
    analysisCluster.setUnexpected_freq(r.nextBoolean());
    List<MessageFrequency> frequencyMapList = new ArrayList<>();
    for (int i = 0; i < 1 + r.nextInt(10); i++) {
      frequencyMapList.add(MessageFrequency.builder().count(r.nextInt(100)).build());
    }

    analysisCluster.setMessage_frequencies(frequencyMapList);
    return analysisCluster;
  }

  @Test
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

    wingsPersistence.save(buildAnalysisRecord(firstRecMinute, LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE, cvConfigId));
    wingsPersistence.save(buildAnalysisRecord(secondRecMinute, LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE, cvConfigId));
    wingsPersistence.save(
        buildAnalysisRecord(secondRecMinute, LogMLAnalysisStatus.FEEDBACK_ANALYSIS_COMPLETE, cvConfigId));

    LogMLAnalysisSummary summary = cv24x7DashboardService.getAnalysisSummary(cvConfigId, startTime, endTime, appId);

    assertEquals(10, summary.getUnknownClusters().size());
  }

  @Test
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

    wingsPersistence.save(buildAnalysisRecord(firstRecMinute, LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE, cvConfigId));
    wingsPersistence.save(buildAnalysisRecord(secondRecMinute, LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE, cvConfigId));
    wingsPersistence.save(
        buildAnalysisRecord(secondRecMinute, LogMLAnalysisStatus.FEEDBACK_ANALYSIS_COMPLETE, cvConfigId));

    LogMLAnalysisSummary summary = cv24x7DashboardService.getAnalysisSummary(cvConfigId, startTime, endTime, appId);

    assertEquals(20, summary.getUnknownClusters().size());
  }
}
