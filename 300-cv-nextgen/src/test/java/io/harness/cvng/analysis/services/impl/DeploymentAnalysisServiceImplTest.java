package io.harness.cvng.analysis.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.NEMANJA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.DeploymentActivity;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.analysis.beans.CanaryDeploymentAdditionalInfo;
import io.harness.cvng.analysis.beans.CanaryDeploymentAdditionalInfo.HostSummaryInfo;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.Cluster;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ClusterCoordinates;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ClusterSummary;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ClusterType;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ControlClusterSummary;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.HostSummary;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ResultSummary;
import io.harness.cvng.analysis.beans.DeploymentTimeSeriesAnalysisDTO.HostData;
import io.harness.cvng.analysis.beans.DeploymentTimeSeriesAnalysisDTO.HostInfo;
import io.harness.cvng.analysis.beans.DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData;
import io.harness.cvng.analysis.entities.DeploymentLogAnalysis;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis;
import io.harness.cvng.analysis.services.api.DeploymentAnalysisService;
import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.HostRecordDTO;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.core.beans.LoadTestAdditionalInfo;
import io.harness.cvng.core.services.api.HostRecordService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.dashboard.beans.TimeSeriesMetricDataDTO.TimeSeriesRisk;
import io.harness.cvng.verificationjob.beans.CanaryVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.cvng.verificationjob.beans.TestVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobInstanceDTO;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DeploymentAnalysisServiceImplTest extends CvNextGenTest {
  @Inject HPersistence hPersistence;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private HostRecordService hostRecordService;
  @Inject private VerificationJobService verificationJobService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private DeploymentTimeSeriesAnalysisService deploymentTimeSeriesAnalysisService;
  @Inject private DeploymentLogAnalysisService deploymentLogAnalysisService;
  @Inject private DeploymentAnalysisService deploymentAnalysisService;
  @Inject private ActivityService activityService;

  private String accountId;
  private String cvConfigId;
  private String identifier;
  private String serviceIdentifier;
  private String projectIdentifier;
  private String orgIdentifier;
  private String envIdentifier;

  @Before
  public void setUp() {
    accountId = generateUuid();
    cvConfigId = generateUuid();
    serviceIdentifier = generateUuid();
    identifier = generateUuid();
    projectIdentifier = generateUuid();
    orgIdentifier = generateUuid();
    envIdentifier = generateUuid();
  }

  private CanaryVerificationJobDTO createCanaryVerificationJobDTO() {
    CanaryVerificationJobDTO canaryVerificationJobDTO = new CanaryVerificationJobDTO();
    canaryVerificationJobDTO.setIdentifier(identifier);
    canaryVerificationJobDTO.setJobName("jobName");
    canaryVerificationJobDTO.setDuration("100");
    canaryVerificationJobDTO.setServiceIdentifier(serviceIdentifier);
    canaryVerificationJobDTO.setProjectIdentifier(projectIdentifier);
    canaryVerificationJobDTO.setOrgIdentifier(orgIdentifier);
    canaryVerificationJobDTO.setEnvIdentifier(envIdentifier);
    canaryVerificationJobDTO.setDataSources(Arrays.asList(DataSourceType.APP_DYNAMICS));
    canaryVerificationJobDTO.setSensitivity(Sensitivity.LOW.name());
    return canaryVerificationJobDTO;
  }

  private TestVerificationJobDTO createTestVerificationJobDTO(String baselineVerificationJobInstanceId) {
    TestVerificationJobDTO testVerificationJobDTO = new TestVerificationJobDTO();
    testVerificationJobDTO.setIdentifier(identifier);
    testVerificationJobDTO.setJobName("jobName");
    testVerificationJobDTO.setDuration("100");
    testVerificationJobDTO.setServiceIdentifier(serviceIdentifier);
    testVerificationJobDTO.setProjectIdentifier(projectIdentifier);
    testVerificationJobDTO.setOrgIdentifier(orgIdentifier);
    testVerificationJobDTO.setEnvIdentifier(envIdentifier);
    testVerificationJobDTO.setDataSources(Arrays.asList(DataSourceType.APP_DYNAMICS));
    testVerificationJobDTO.setSensitivity(Sensitivity.LOW.name());
    testVerificationJobDTO.setBaselineVerificationJobInstanceId(baselineVerificationJobInstanceId);
    return testVerificationJobDTO;
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetCanaryDeploymentAdditionalInfo_withBothTimeSeriesAndLogsAnalyses() {
    verificationJobService.upsert(accountId, createCanaryVerificationJobDTO());
    VerificationJob verificationJob = verificationJobService.getVerificationJob(accountId, identifier);
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, createVerificationJobInstanceDTO());
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    verificationJobInstance.setResolvedJob(verificationJob);
    hPersistence.save(verificationJobInstance);
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);

    Set<String> preDeploymentHosts = new HashSet<>();
    preDeploymentHosts.add("node1");

    HostRecordDTO hostRecordDTO = createHostRecordDTO(preDeploymentHosts, verificationTaskId);

    hostRecordService.save(hostRecordDTO);

    deploymentTimeSeriesAnalysisService.save(createDeploymentTimeSeriesAnalysis(verificationTaskId));
    deploymentLogAnalysisService.save(createDeploymentLogAnalysis(verificationTaskId));

    CanaryDeploymentAdditionalInfo canaryDeploymentAdditionalInfo =
        deploymentAnalysisService.getCanaryDeploymentAdditionalInfo(accountId, verificationJobInstance);

    assertThat(canaryDeploymentAdditionalInfo).isNotNull();
    assertThat(canaryDeploymentAdditionalInfo.getPrimary().size()).isEqualTo(1);
    List<HostSummaryInfo> primaryHosts = new ArrayList<>(canaryDeploymentAdditionalInfo.getPrimary());
    assertThat(primaryHosts.get(0).getHostName()).contains("node1");
    assertThat(canaryDeploymentAdditionalInfo.getCanary()).isNotNull();
    assertThat(canaryDeploymentAdditionalInfo.getCanary().size()).isEqualTo(2);
    List<HostSummaryInfo> canaryHosts = new ArrayList<>(canaryDeploymentAdditionalInfo.getCanary());
    assertThat(canaryHosts.get(0).getHostName()).isEqualTo("node1");
    assertThat(canaryHosts.get(0).getRiskScore()).isEqualTo(TimeSeriesRisk.MEDIUM_RISK);
    assertThat(canaryHosts.get(0).getAnomalousMetricsCount()).isEqualTo(0);
    assertThat(canaryHosts.get(0).getAnomalousLogClustersCount()).isEqualTo(2);
    assertThat(canaryHosts.get(1).getHostName()).isEqualTo("node2");
    assertThat(canaryHosts.get(1).getRiskScore()).isEqualTo(TimeSeriesRisk.HIGH_RISK);
    assertThat(canaryHosts.get(1).getAnomalousMetricsCount()).isEqualTo(2);
    assertThat(canaryHosts.get(1).getAnomalousLogClustersCount()).isEqualTo(3);
    assertThat(canaryDeploymentAdditionalInfo.getTrafficSplitPercentage()).isNull();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetCanaryDeploymentAdditionalInfo_withoutAnalysesAndHostRecords() {
    verificationJobService.upsert(accountId, createCanaryVerificationJobDTO());
    VerificationJob verificationJob = verificationJobService.getVerificationJob(accountId, identifier);
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, createVerificationJobInstanceDTO());
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    verificationJobInstance.setResolvedJob(verificationJob);
    hPersistence.save(verificationJobInstance);
    verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);

    CanaryDeploymentAdditionalInfo canaryDeploymentAdditionalInfo =
        deploymentAnalysisService.getCanaryDeploymentAdditionalInfo(accountId, verificationJobInstance);

    assertThat(canaryDeploymentAdditionalInfo).isNotNull();
    assertThat(canaryDeploymentAdditionalInfo.getPrimary()).isEmpty();
    assertThat(canaryDeploymentAdditionalInfo.getCanary()).isEmpty();
    assertThat(canaryDeploymentAdditionalInfo.getTrafficSplitPercentage()).isNull();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetCanaryDeploymentAdditionalInfo_withTimeSeriesAnalysisOnly() {
    verificationJobService.upsert(accountId, createCanaryVerificationJobDTO());
    VerificationJob verificationJob = verificationJobService.getVerificationJob(accountId, identifier);
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, createVerificationJobInstanceDTO());
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    verificationJobInstance.setResolvedJob(verificationJob);
    hPersistence.save(verificationJobInstance);
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);

    Set<String> preDeploymentHosts = new HashSet<>();
    preDeploymentHosts.add("node1");

    HostRecordDTO hostRecordDTO = createHostRecordDTO(preDeploymentHosts, verificationTaskId);
    hostRecordService.save(hostRecordDTO);

    deploymentTimeSeriesAnalysisService.save(createDeploymentTimeSeriesAnalysis(verificationTaskId));

    CanaryDeploymentAdditionalInfo canaryDeploymentAdditionalInfo =
        deploymentAnalysisService.getCanaryDeploymentAdditionalInfo(accountId, verificationJobInstance);

    assertThat(canaryDeploymentAdditionalInfo).isNotNull();
    assertThat(canaryDeploymentAdditionalInfo.getPrimary().size()).isEqualTo(1);
    List<HostSummaryInfo> primaryHosts = new ArrayList<>(canaryDeploymentAdditionalInfo.getPrimary());
    assertThat(primaryHosts.get(0).getHostName()).contains("node1");
    assertThat(canaryDeploymentAdditionalInfo.getCanary()).isNotNull();
    assertThat(canaryDeploymentAdditionalInfo.getCanary().size()).isEqualTo(1);
    List<HostSummaryInfo> canaryHosts = new ArrayList<>(canaryDeploymentAdditionalInfo.getCanary());
    assertThat(canaryHosts.get(0).getHostName()).isEqualTo("node2");
    assertThat(canaryHosts.get(0).getRiskScore()).isEqualTo(TimeSeriesRisk.HIGH_RISK);
    assertThat(canaryHosts.get(0).getAnomalousMetricsCount()).isEqualTo(2);
    assertThat(canaryHosts.get(0).getAnomalousLogClustersCount()).isEqualTo(0);
    assertThat(canaryDeploymentAdditionalInfo.getTrafficSplitPercentage()).isNull();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetCanaryDeploymentAdditionalInfo_withLogAnalysisOnly() {
    verificationJobService.upsert(accountId, createCanaryVerificationJobDTO());
    VerificationJob verificationJob = verificationJobService.getVerificationJob(accountId, identifier);
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, createVerificationJobInstanceDTO());
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    verificationJobInstance.setResolvedJob(verificationJob);
    hPersistence.save(verificationJobInstance);
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);

    Set<String> preDeploymentHosts = new HashSet<>();
    preDeploymentHosts.add("node1");

    HostRecordDTO hostRecordDTO = createHostRecordDTO(preDeploymentHosts, verificationTaskId);

    hostRecordService.save(hostRecordDTO);

    deploymentLogAnalysisService.save(createDeploymentLogAnalysis(verificationTaskId));

    CanaryDeploymentAdditionalInfo canaryDeploymentAdditionalInfo =
        deploymentAnalysisService.getCanaryDeploymentAdditionalInfo(accountId, verificationJobInstance);

    assertThat(canaryDeploymentAdditionalInfo).isNotNull();
    assertThat(canaryDeploymentAdditionalInfo.getPrimary().size()).isEqualTo(1);
    List<HostSummaryInfo> primaryHosts = new ArrayList<>(canaryDeploymentAdditionalInfo.getPrimary());
    assertThat(primaryHosts.get(0).getHostName()).contains("node1");
    assertThat(canaryDeploymentAdditionalInfo.getCanary()).isNotNull();
    assertThat(canaryDeploymentAdditionalInfo.getCanary().size()).isEqualTo(2);
    List<HostSummaryInfo> canaryHosts = new ArrayList<>(canaryDeploymentAdditionalInfo.getCanary());
    assertThat(canaryHosts.get(0).getHostName()).isEqualTo("node1");
    assertThat(canaryHosts.get(0).getRiskScore()).isEqualTo(TimeSeriesRisk.MEDIUM_RISK);
    assertThat(canaryHosts.get(0).getAnomalousMetricsCount()).isEqualTo(0);
    assertThat(canaryHosts.get(0).getAnomalousLogClustersCount()).isEqualTo(2);
    assertThat(canaryHosts.get(1).getHostName()).isEqualTo("node2");
    assertThat(canaryHosts.get(1).getRiskScore()).isEqualTo(TimeSeriesRisk.HIGH_RISK);
    assertThat(canaryHosts.get(1).getAnomalousMetricsCount()).isEqualTo(0);
    assertThat(canaryHosts.get(1).getAnomalousLogClustersCount()).isEqualTo(3);
    assertThat(canaryDeploymentAdditionalInfo.getTrafficSplitPercentage()).isNull();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetCanaryDeploymentAdditionalInfo_withImprovisedCanary() {
    verificationJobService.upsert(accountId, createCanaryVerificationJobDTO());
    VerificationJob verificationJob = verificationJobService.getVerificationJob(accountId, identifier);
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, createVerificationJobInstanceDTO());
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    verificationJobInstance.setResolvedJob(verificationJob);
    hPersistence.save(verificationJobInstance);
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);

    Set<String> preDeploymentHosts = new HashSet<>();
    preDeploymentHosts.add("node1");

    HostRecordDTO hostRecordDTO = createHostRecordDTO(preDeploymentHosts, verificationTaskId);

    hostRecordService.save(hostRecordDTO);

    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis = createDeploymentTimeSeriesAnalysis(verificationTaskId);

    HostInfo hostInfo1 = createHostInfo("node1", 1, 1.1, true, false);
    HostInfo hostInfo2 = createHostInfo("node2", 2, 2.2, true, false);

    deploymentTimeSeriesAnalysis.setHostSummaries(Arrays.asList(hostInfo1, hostInfo2));

    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysis.setHostSummaries(new ArrayList<>());

    deploymentTimeSeriesAnalysisService.save(deploymentTimeSeriesAnalysis);
    deploymentLogAnalysisService.save(deploymentLogAnalysis);

    CanaryDeploymentAdditionalInfo canaryDeploymentAdditionalInfo =
        deploymentAnalysisService.getCanaryDeploymentAdditionalInfo(accountId, verificationJobInstance);

    assertThat(canaryDeploymentAdditionalInfo).isNotNull();
    assertThat(canaryDeploymentAdditionalInfo.getPrimary().size()).isEqualTo(2);
    assertThat(canaryDeploymentAdditionalInfo.getCanary().size()).isEqualTo(2);
    assertThat(canaryDeploymentAdditionalInfo.getPrimary()).isEqualTo(canaryDeploymentAdditionalInfo.getCanary());
    assertThat(canaryDeploymentAdditionalInfo.getTrafficSplitPercentage()).isNull();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetCanaryDeploymentAdditionalInfo_withImprovisedCanaryAndAdditionalPreDeploymentHost() {
    CanaryVerificationJobDTO canaryVerificationJobDTO = createCanaryVerificationJobDTO();
    canaryVerificationJobDTO.setTrafficSplitPercentage(60);
    verificationJobService.upsert(accountId, canaryVerificationJobDTO);
    VerificationJob verificationJob = verificationJobService.getVerificationJob(accountId, identifier);
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, createVerificationJobInstanceDTO());
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    verificationJobInstance.setResolvedJob(verificationJob);
    hPersistence.save(verificationJobInstance);
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);

    Set<String> preDeploymentHosts = new HashSet<>();
    preDeploymentHosts.add("node1");
    preDeploymentHosts.add("node2");
    preDeploymentHosts.add("node3");

    HostRecordDTO hostRecordDTO = createHostRecordDTO(preDeploymentHosts, verificationTaskId);

    hostRecordService.save(hostRecordDTO);

    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis = createDeploymentTimeSeriesAnalysis(verificationTaskId);

    HostInfo hostInfo1 = createHostInfo("node1", 1, 1.1, true, false);
    HostInfo hostInfo2 = createHostInfo("node2", 2, 2.2, true, false);

    deploymentTimeSeriesAnalysis.setHostSummaries(Arrays.asList(hostInfo1, hostInfo2));

    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysis.setHostSummaries(new ArrayList<>());

    deploymentTimeSeriesAnalysisService.save(deploymentTimeSeriesAnalysis);
    deploymentLogAnalysisService.save(deploymentLogAnalysis);

    CanaryDeploymentAdditionalInfo canaryDeploymentAdditionalInfo =
        deploymentAnalysisService.getCanaryDeploymentAdditionalInfo(accountId, verificationJobInstance);

    assertThat(canaryDeploymentAdditionalInfo).isNotNull();
    assertThat(canaryDeploymentAdditionalInfo.getPrimary().size()).isEqualTo(3);
    // verifies that primary nodes no longer contain riskScore
    canaryDeploymentAdditionalInfo.getPrimary().forEach(
        hostSummaryInfo -> assertThat(hostSummaryInfo.getRiskScore()).isNull());
    assertThat(canaryDeploymentAdditionalInfo.getCanary().size()).isEqualTo(3);
    assertThat(canaryDeploymentAdditionalInfo.getPrimary()).isEqualTo(canaryDeploymentAdditionalInfo.getCanary());
    assertThat(canaryDeploymentAdditionalInfo.getTrafficSplitPercentage()).isNotNull();
    assertThat(canaryDeploymentAdditionalInfo.getTrafficSplitPercentage().getPreDeploymentPercentage()).isEqualTo(60);
    assertThat(canaryDeploymentAdditionalInfo.getTrafficSplitPercentage().getPostDeploymentPercentage()).isEqualTo(40);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLoadTestAdditionalInfo_withBaselineVerificationJobInstanceId() {
    verificationJobService.upsert(accountId, createTestVerificationJobDTO(null));
    String baselineVerificationJobInstanceId =
        verificationJobInstanceService.create(accountId, createVerificationJobInstanceDTO());
    VerificationJob verificationJob = verificationJobService.getVerificationJob(accountId, identifier);
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(baselineVerificationJobInstanceId);
    verificationJobInstance.setResolvedJob(verificationJob);
    Instant baselineStartTIme = Instant.now().minus(10, ChronoUnit.MINUTES);
    verificationJobInstance.setStartTime(baselineStartTIme);
    hPersistence.save(verificationJobInstance);

    DeploymentActivity deploymentActivity = DeploymentActivity.builder()
                                                .verificationStartTime(baselineStartTIme.toEpochMilli())
                                                .deploymentTag("Build1")
                                                .build();
    Activity activity = deploymentActivity;
    activity.setActivityName("baselineActivity");
    activity.setAccountId(accountId);
    activity.setVerificationJobInstanceIds(Arrays.asList(baselineVerificationJobInstanceId));
    activity.setType(ActivityType.DEPLOYMENT);
    activityService.createActivity(activity);

    verificationJobService.upsert(accountId, createTestVerificationJobDTO(baselineVerificationJobInstanceId));
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, createVerificationJobInstanceDTO());
    VerificationJobInstance currentVerificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    VerificationJob modifiedVerificationJob = verificationJobService.getVerificationJob(accountId, identifier);
    currentVerificationJobInstance.setResolvedJob(modifiedVerificationJob);
    Instant currentTime = Instant.now();
    currentVerificationJobInstance.setStartTime(currentTime);
    hPersistence.save(currentVerificationJobInstance);

    DeploymentActivity currentDeploymentActivity =
        DeploymentActivity.builder().verificationStartTime(currentTime.toEpochMilli()).deploymentTag("Build2").build();
    Activity currentActivity = currentDeploymentActivity;
    currentActivity.setActivityName("currentActivity");
    currentActivity.setAccountId(accountId);
    currentActivity.setVerificationJobInstanceIds(Arrays.asList(verificationJobInstanceId));
    currentActivity.setType(ActivityType.DEPLOYMENT);
    activityService.createActivity(currentActivity);
    LoadTestAdditionalInfo loadTestAdditionalInfo =
        deploymentAnalysisService.getLoadTestAdditionalInfo(accountId, currentVerificationJobInstance);

    assertThat(loadTestAdditionalInfo).isNotNull();
    assertThat(loadTestAdditionalInfo.getBaselineDeploymentTag()).isEqualTo("Build1");
    assertThat(loadTestAdditionalInfo.getBaselineStartTime()).isEqualTo(baselineStartTIme.toEpochMilli());
    assertThat(loadTestAdditionalInfo.getCurrentDeploymentTag()).isEqualTo("Build2");
    assertThat(loadTestAdditionalInfo.getCurrentStartTime()).isEqualTo(currentTime.toEpochMilli());
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLoadTestAdditionalInfo_withoutBaselineVerificationJobInstanceId() {
    verificationJobService.upsert(accountId, createTestVerificationJobDTO(null));
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, createVerificationJobInstanceDTO());
    VerificationJob verificationJob = verificationJobService.getVerificationJob(accountId, identifier);
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    verificationJobInstance.setResolvedJob(verificationJob);
    Instant currentTime = Instant.now();
    verificationJobInstance.setStartTime(currentTime);
    hPersistence.save(verificationJobInstance);

    DeploymentActivity deploymentActivity =
        DeploymentActivity.builder().verificationStartTime(currentTime.toEpochMilli()).deploymentTag("Build1").build();
    Activity activity = deploymentActivity;
    activity.setActivityName("activity");
    activity.setAccountId(accountId);
    activity.setVerificationJobInstanceIds(Arrays.asList(verificationJobInstanceId));
    activity.setType(ActivityType.DEPLOYMENT);
    activityService.createActivity(activity);

    LoadTestAdditionalInfo loadTestAdditionalInfo =
        deploymentAnalysisService.getLoadTestAdditionalInfo(accountId, verificationJobInstance);

    assertThat(loadTestAdditionalInfo).isNotNull();
    assertThat(loadTestAdditionalInfo.getBaselineDeploymentTag()).isNull();
    assertThat(loadTestAdditionalInfo.getBaselineStartTime()).isEqualTo(null);
    assertThat(loadTestAdditionalInfo.getCurrentDeploymentTag()).isEqualTo("Build1");
    assertThat(loadTestAdditionalInfo.getCurrentStartTime()).isEqualTo(currentTime.toEpochMilli());
  }
  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLoadTestAdditionalInfo_withoutActivityForVerificationJobInstanceId() {
    verificationJobService.upsert(accountId, createTestVerificationJobDTO(null));
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, createVerificationJobInstanceDTO());
    VerificationJob verificationJob = verificationJobService.getVerificationJob(accountId, identifier);
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    verificationJobInstance.setResolvedJob(verificationJob);
    Instant currentTime = Instant.now();
    verificationJobInstance.setStartTime(currentTime);
    hPersistence.save(verificationJobInstance);

    DeploymentActivity deploymentActivity =
        DeploymentActivity.builder().verificationStartTime(currentTime.toEpochMilli()).deploymentTag("Build1").build();
    Activity activity = deploymentActivity;
    activity.setActivityName("randomActivity");
    activity.setAccountId(accountId);
    activity.setVerificationJobInstanceIds(Arrays.asList(generateUuid()));
    activity.setType(ActivityType.DEPLOYMENT);
    activityService.createActivity(activity);

    assertThatThrownBy(() -> deploymentAnalysisService.getLoadTestAdditionalInfo(accountId, verificationJobInstance))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Activity not found for verificationJobInstanceId: " + verificationJobInstanceId);
  }

  private HostRecordDTO createHostRecordDTO(Set<String> preDeploymentHosts, String verificationTaskId) {
    return HostRecordDTO.builder()
        .hosts(preDeploymentHosts)
        .verificationTaskId(verificationTaskId)
        .startTime(Instant.now().minus(5, ChronoUnit.MINUTES))
        .endTime(Instant.now().minus(2, ChronoUnit.MINUTES))
        .build();
  }

  private VerificationJobInstanceDTO createVerificationJobInstanceDTO() {
    return VerificationJobInstanceDTO.builder()
        .verificationJobIdentifier(identifier)
        .verificationTaskStartTimeMs(Instant.now().toEpochMilli())
        .deploymentStartTimeMs(Instant.now().toEpochMilli())
        .build();
  }

  private HostInfo createHostInfo(String hostName, int risk, Double score, boolean primary, boolean canary) {
    return HostInfo.builder().hostName(hostName).risk(risk).score(score).primary(primary).canary(canary).build();
  }

  private HostData createHostData(
      String hostName, int risk, Double score, List<Double> controlData, List<Double> testData) {
    return HostData.builder()
        .hostName(hostName)
        .risk(risk)
        .score(score)
        .controlData(controlData)
        .testData(testData)
        .build();
  }

  private TransactionMetricHostData createTransactionMetricHostData(
      String transactionName, String metricName, int risk, Double score, List<HostData> hostDataList) {
    return TransactionMetricHostData.builder()
        .transactionName(transactionName)
        .metricName(metricName)
        .risk(risk)
        .score(score)
        .hostData(hostDataList)
        .build();
  }

  private DeploymentTimeSeriesAnalysis createDeploymentTimeSeriesAnalysis(String verificationTaskId) {
    HostInfo hostInfo1 = createHostInfo("node1", 1, 1.1, false, false);
    HostInfo hostInfo2 = createHostInfo("node2", 2, 2.2, false, true);
    HostData hostData1 = createHostData("node1", 0, 0.0, Arrays.asList(1D), Arrays.asList(1D));
    HostData hostData2 = createHostData("node2", 2, 2.0, Arrays.asList(1D), Arrays.asList(1D));

    TransactionMetricHostData transactionMetricHostData1 = createTransactionMetricHostData(
        "/todolist/inside", "Errors per Minute", 0, 0.5, Arrays.asList(hostData1, hostData2));

    HostData hostData3 = createHostData("node1", 0, 0.0, Arrays.asList(1D), Arrays.asList(1D));
    HostData hostData4 = createHostData("node2", 2, 2.0, Arrays.asList(1D), Arrays.asList(1D));

    TransactionMetricHostData transactionMetricHostData2 = createTransactionMetricHostData(
        "/todolist/exception", "Calls per Minute", 2, 2.5, Arrays.asList(hostData3, hostData4));
    return DeploymentTimeSeriesAnalysis.builder()
        .accountId(accountId)
        .score(1.0)
        .risk(1)
        .verificationTaskId(verificationTaskId)
        .transactionMetricSummaries(Arrays.asList(transactionMetricHostData1, transactionMetricHostData2))
        .hostSummaries(Arrays.asList(hostInfo1, hostInfo2))
        .startTime(Instant.now())
        .endTime(Instant.now().plus(1, ChronoUnit.MINUTES))
        .build();
  }

  private DeploymentLogAnalysis createDeploymentLogAnalysis(String verificationTaskId) {
    Cluster cluster1 = createCluster("Error in cluster 1", 1);
    Cluster cluster2 = createCluster("Error in cluster 2", 2);
    Cluster cluster3 = createCluster("Error in cluster 3", 3);
    List<Cluster> clusters = Arrays.asList(cluster1, cluster2, cluster3);

    ClusterCoordinates clusterCoordinates1 = createClusterCoordinates("node1", 1, 0.6464, 0.717171);
    ClusterCoordinates clusterCoordinates2 = createClusterCoordinates("node2", 2, 0.4525, 0.542524);
    ClusterCoordinates clusterCoordinates3 = createClusterCoordinates("node3", 3, 0.4525, 0.542524);
    List<ClusterCoordinates> clusterCoordinatesList =
        Arrays.asList(clusterCoordinates1, clusterCoordinates2, clusterCoordinates3);

    ClusterSummary clusterSummary1 = createClusterSummary(1, 0.7, 36, 1, Arrays.asList(2D), ClusterType.KNOWN_EVENT);
    ClusterSummary clusterSummary2 = createClusterSummary(0, 0, 3, 2, Arrays.asList(2D), ClusterType.KNOWN_EVENT);
    ClusterSummary clusterSummary3 = createClusterSummary(2, 2.2, 55, 3, Arrays.asList(4D), ClusterType.KNOWN_EVENT);

    ResultSummary resultSummary =
        createResultSummary(1, 1, Arrays.asList(clusterSummary1, clusterSummary2, clusterSummary3), null);

    ClusterSummary clusterSummary4 = createClusterSummary(2, 0.7, 36, 1, Arrays.asList(2D), ClusterType.KNOWN_EVENT);
    ClusterSummary clusterSummary5 = createClusterSummary(2, 0, 3, 2, Arrays.asList(2D), ClusterType.KNOWN_EVENT);
    ClusterSummary clusterSummary6 = createClusterSummary(2, 2.2, 55, 3, Arrays.asList(4D), ClusterType.KNOWN_EVENT);

    ResultSummary resultSummary2 =
        createResultSummary(2, 1, Arrays.asList(clusterSummary4, clusterSummary5, clusterSummary6), null);

    HostSummary hostSummary1 = createHostSummary("node1", resultSummary);
    HostSummary hostSummary2 = createHostSummary("node2", resultSummary2);
    return DeploymentLogAnalysis.builder()
        .accountId(accountId)
        .clusters(clusters)
        .clusterCoordinates(clusterCoordinatesList)
        .verificationTaskId(verificationTaskId)
        .resultSummary(resultSummary)
        .hostSummaries(Arrays.asList(hostSummary1, hostSummary2))
        .startTime(Instant.now())
        .endTime(Instant.now().plus(10, ChronoUnit.MINUTES))
        .build();
  }

  private ResultSummary createResultSummary(int risk, double score, List<ClusterSummary> testClusterSummaries,
      List<ControlClusterSummary> controlClusterSummaries) {
    return ResultSummary.builder()
        .risk(risk)
        .score(score)
        .controlClusterSummaries(controlClusterSummaries)
        .testClusterSummaries(testClusterSummaries)
        .build();
  }

  private ClusterCoordinates createClusterCoordinates(String hostName, int label, double x, double y) {
    return ClusterCoordinates.builder().host(hostName).label(label).x(x).y(y).build();
  }

  private Cluster createCluster(String text, int label) {
    return Cluster.builder().text(text).label(label).build();
  }

  private ClusterSummary createClusterSummary(
      int risk, double score, int count, int label, List<Double> testFrequencyData, ClusterType clusterType) {
    return ClusterSummary.builder()
        .risk(risk)
        .clusterType(clusterType)
        .score(score)
        .count(count)
        .label(label)
        .testFrequencyData(testFrequencyData)
        .build();
  }

  private HostSummary createHostSummary(String host, ResultSummary resultSummary) {
    return HostSummary.builder().host(host).resultSummary(resultSummary).build();
  }
}
