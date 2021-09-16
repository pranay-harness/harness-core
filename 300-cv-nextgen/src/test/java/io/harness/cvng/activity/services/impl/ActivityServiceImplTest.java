package io.harness.cvng.activity.services.impl;

import static io.harness.cvng.verificationjob.CVVerificationJobConstants.ENV_IDENTIFIER_KEY;
import static io.harness.cvng.verificationjob.CVVerificationJobConstants.JOB_IDENTIFIER_KEY;
import static io.harness.cvng.verificationjob.CVVerificationJobConstants.SERVICE_IDENTIFIER_KEY;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.ABHIJITH;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.NEMANJA;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.RAGHU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.activity.beans.ActivityDashboardDTO;
import io.harness.cvng.activity.beans.ActivityVerificationResultDTO;
import io.harness.cvng.activity.beans.ActivityVerificationResultDTO.CategoryRisk;
import io.harness.cvng.activity.beans.ActivityVerificationSummary;
import io.harness.cvng.activity.beans.DeploymentActivityPopoverResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivitySummaryDTO;
import io.harness.cvng.activity.beans.DeploymentActivityVerificationResultDTO;
import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.Activity.ActivityKeys;
import io.harness.cvng.activity.entities.CD10ActivitySource;
import io.harness.cvng.activity.entities.DeploymentActivity;
import io.harness.cvng.activity.entities.KubernetesActivity;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.alert.services.api.AlertRuleService;
import io.harness.cvng.alert.util.VerificationStatus;
import io.harness.cvng.analysis.beans.DeploymentTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis;
import io.harness.cvng.analysis.entities.HealthVerificationPeriod;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityDTO.VerificationJobRuntimeDetails;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.beans.activity.DeploymentActivityDTO;
import io.harness.cvng.beans.activity.InfrastructureActivityDTO;
import io.harness.cvng.beans.activity.cd10.CD10ActivitySourceDTO;
import io.harness.cvng.beans.activity.cd10.CD10EnvMappingDTO;
import io.harness.cvng.beans.activity.cd10.CD10ServiceMappingDTO;
import io.harness.cvng.beans.job.Sensitivity;
import io.harness.cvng.beans.job.VerificationJobType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.dashboard.services.api.HealthVerificationHeatMapService;
import io.harness.cvng.verificationjob.entities.CanaryVerificationJob;
import io.harness.cvng.verificationjob.entities.HealthVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob.RuntimeParameter;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ExecutionStatus;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ActivityServiceImplTest extends CvNextGenTestBase {
  @Inject private HPersistence hPersistence;
  @Inject private ActivityService activityService;
  @Inject private VerificationJobService realVerificationJobService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private VerificationJobInstanceService realVerificationJobInstanceService;
  @Inject private DeploymentTimeSeriesAnalysisService deploymentTimeSeriesAnalysisService;
  @Inject private CVConfigService cvConfigService;
  @Mock private VerificationJobService verificationJobService;
  @Mock private VerificationJobInstanceService verificationJobInstanceService;
  @Mock private HealthVerificationHeatMapService healthVerificationHeatMapService;
  @Mock private NextGenService nextGenService;
  @Mock private AlertRuleService alertRuleService;

  private String projectIdentifier;
  private String orgIdentifier;
  private String accountId;
  private Instant instant;
  private String serviceIdentifier;
  private String envIdentifier;
  private String deploymentTag;
  private BuilderFactory builderFactory;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    builderFactory = BuilderFactory.getDefault();
    instant = Instant.parse("2020-07-27T10:44:06.390Z");
    projectIdentifier = builderFactory.getContext().getProjectIdentifier();
    orgIdentifier = builderFactory.getContext().getOrgIdentifier();
    accountId = builderFactory.getContext().getAccountId();
    serviceIdentifier = builderFactory.getContext().getServiceIdentifier();
    envIdentifier = builderFactory.getContext().getEnvIdentifier();
    deploymentTag = "build#1";

    FieldUtils.writeField(activityService, "verificationJobService", verificationJobService, true);
    FieldUtils.writeField(activityService, "verificationJobInstanceService", verificationJobInstanceService, true);
    FieldUtils.writeField(activityService, "nextGenService", nextGenService, true);
    FieldUtils.writeField(activityService, "healthVerificationHeatMapService", healthVerificationHeatMapService, true);
    FieldUtils.writeField(activityService, "alertRuleService", alertRuleService, true);
    when(nextGenService.getService(any(), any(), any(), any()))
        .thenReturn(ServiceResponseDTO.builder().name("service name").build());
    when(verificationJobInstanceService.getCVConfigsForVerificationJob(any()))
        .thenReturn(Lists.newArrayList(new AppDynamicsCVConfig()));
    realVerificationJobService.createDefaultVerificationJobs(accountId, orgIdentifier, projectIdentifier);
    FieldUtils.writeField(
        activityService, "deploymentTimeSeriesAnalysisService", deploymentTimeSeriesAnalysisService, true);
    FieldUtils.writeField(deploymentTimeSeriesAnalysisService, "nextGenService", nextGenService, true);
    when(nextGenService.get(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Optional.of(ConnectorInfoDTO.builder().name("AppDynamics Connector").build()));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetRecentDeploymentActivityVerifications_noData() {
    List<DeploymentActivityVerificationResultDTO> deploymentActivityVerificationResultDTOs =
        activityService.getRecentDeploymentActivityVerifications(accountId, orgIdentifier, projectIdentifier);
    assertThat(deploymentActivityVerificationResultDTOs).isEmpty();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetRecentDeploymentActivityVerifications_withVerificationJobInstanceInQueuedState() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    when(verificationJobInstanceService.create(anyList())).thenReturn(Arrays.asList("taskId1"));
    DeploymentActivityVerificationResultDTO deploymentActivityVerificationResultDTO =
        DeploymentActivityVerificationResultDTO.builder().build();
    when(verificationJobInstanceService.getAggregatedVerificationResult(anyList()))
        .thenReturn(deploymentActivityVerificationResultDTO);
    List<VerificationJobRuntimeDetails> verificationJobDetails = new ArrayList<>();
    Map<String, String> runtimeParams = new HashMap<>();
    runtimeParams.put(JOB_IDENTIFIER_KEY, verificationJob.getIdentifier());
    runtimeParams.put(SERVICE_IDENTIFIER_KEY, "cvngService");
    runtimeParams.put(ENV_IDENTIFIER_KEY, "production");
    VerificationJobRuntimeDetails runtimeDetails = VerificationJobRuntimeDetails.builder()
                                                       .verificationJobIdentifier(verificationJob.getIdentifier())
                                                       .runtimeValues(runtimeParams)
                                                       .build();
    verificationJobDetails.add(runtimeDetails);
    Instant now = Instant.now();
    ActivityDTO activityDTO =
        getDeploymentActivityDTO(verificationJobDetails, now, "build#1", generateUuid(), serviceIdentifier);
    activityService.register(accountId, activityDTO);

    List<DeploymentActivityVerificationResultDTO> deploymentActivityVerificationResultDTOs =
        activityService.getRecentDeploymentActivityVerifications(accountId, orgIdentifier, projectIdentifier);
    assertThat(deploymentActivityVerificationResultDTOs)
        .isEqualTo(Collections.singletonList(deploymentActivityVerificationResultDTO));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetRecentDeploymentActivityVerifications_groupByBuildAndServiceIdentifier() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    when(verificationJobInstanceService.create(anyList())).thenReturn(Arrays.asList("taskId1"));
    DeploymentActivityVerificationResultDTO deploymentActivityVerificationResultDTO =
        DeploymentActivityVerificationResultDTO.builder().build();
    when(verificationJobInstanceService.getAggregatedVerificationResult(anyList()))
        .thenReturn(deploymentActivityVerificationResultDTO);
    List<VerificationJobRuntimeDetails> verificationJobDetails = new ArrayList<>();
    Map<String, String> runtimeParams = new HashMap<>();
    runtimeParams.put(JOB_IDENTIFIER_KEY, verificationJob.getIdentifier());
    runtimeParams.put(SERVICE_IDENTIFIER_KEY, "cvngService");
    runtimeParams.put(ENV_IDENTIFIER_KEY, "production");
    VerificationJobRuntimeDetails runtimeDetails = VerificationJobRuntimeDetails.builder()
                                                       .verificationJobIdentifier(verificationJob.getIdentifier())
                                                       .runtimeValues(runtimeParams)
                                                       .build();
    verificationJobDetails.add(runtimeDetails);
    Instant now = Instant.now();
    ActivityDTO activityDTOManager =
        getDeploymentActivityDTO(verificationJobDetails, now, "build#1", generateUuid(), "manager");
    ActivityDTO activityDTOCVNG1 =
        getDeploymentActivityDTO(verificationJobDetails, now, "build#1", generateUuid(), "cvng");
    ActivityDTO activityDTOCVNG2 =
        getDeploymentActivityDTO(verificationJobDetails, now, "build#2", generateUuid(), "cvng");
    ActivityDTO activityDTOCVNG3 =
        getDeploymentActivityDTO(verificationJobDetails, now, "build#2", generateUuid(), "cvng");

    activityService.register(accountId, activityDTOManager);
    activityService.register(accountId, activityDTOCVNG1);
    activityService.register(accountId, activityDTOCVNG2);
    activityService.register(accountId, activityDTOCVNG3);
    List<DeploymentActivityVerificationResultDTO> deploymentActivityVerificationResultDTOs =
        activityService.getRecentDeploymentActivityVerifications(accountId, orgIdentifier, projectIdentifier);
    assertThat(deploymentActivityVerificationResultDTOs).hasSize(3);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetRecentDeploymentActivityVerificationsByTag() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    when(verificationJobInstanceService.create(anyList())).thenReturn(Arrays.asList("taskId1"));
    doNothing().when(verificationJobInstanceService).addResultsToDeploymentResultSummary(anyString(), anyList(), any());
    ActivityDTO activityDTO = getDeploymentActivity(verificationJob);
    activityService.register(accountId, activityDTO);
    activityService.register(accountId, getDeploymentActivity(verificationJob));
    DeploymentActivityResultDTO result = activityService.getDeploymentActivityVerificationsByTag(
        accountId, orgIdentifier, projectIdentifier, serviceIdentifier, deploymentTag);
    assertThat(result).isNotNull();
    assertThat(result.getDeploymentTag()).isEqualTo(deploymentTag);
    assertThat(result.getServiceName()).isEqualTo("service name");
    assertThat(result.getDeploymentResultSummary().getPreProductionDeploymentVerificationJobInstanceSummaries())
        .isEmpty();
    assertThat(result.getDeploymentResultSummary().getProductionDeploymentVerificationJobInstanceSummaries()).isEmpty();
    assertThat(result.getDeploymentResultSummary().getPostDeploymentVerificationJobInstanceSummaries()).isEmpty();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetRecentDeploymentActivityVerificationsByTag_noData() {
    assertThatThrownBy(()
                           -> activityService.getDeploymentActivityVerificationsByTag(
                               accountId, orgIdentifier, projectIdentifier, "service", "tag"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No Deployment Activities were found for deployment tag:");
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetDeploymentActivityVerificationsPopoverSummary_invalidBuildTag() {
    assertThatThrownBy(()
                           -> activityService.getDeploymentActivityVerificationsPopoverSummary(
                               accountId, orgIdentifier, projectIdentifier, "service", "tag"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No Deployment Activities were found for deployment tag:");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetDeploymentActivityVerificationsPopoverSummary_addBuildAndServiceNameToResult() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    when(verificationJobInstanceService.create(anyList())).thenReturn(Arrays.asList("taskId1"));
    DeploymentActivityDTO deploymentActivityDTO = getDeploymentActivity(verificationJob);
    activityService.register(accountId, deploymentActivityDTO);
    DeploymentActivityPopoverResultDTO deploymentActivityPopoverResultDTO =
        DeploymentActivityPopoverResultDTO.builder().build();
    when(verificationJobInstanceService.getDeploymentVerificationPopoverResult(anyList()))
        .thenReturn(deploymentActivityPopoverResultDTO);
    DeploymentActivityPopoverResultDTO ans = activityService.getDeploymentActivityVerificationsPopoverSummary(
        accountId, orgIdentifier, projectIdentifier, serviceIdentifier, deploymentTag);
    assertThat(ans == deploymentActivityPopoverResultDTO).isTrue();
    assertThat(deploymentActivityPopoverResultDTO.getServiceName()).isEqualTo("service name");
    assertThat(deploymentActivityPopoverResultDTO.getTag()).isEqualTo(deploymentTag);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetActivity() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    when(verificationJobInstanceService.create(anyList())).thenReturn(Arrays.asList("taskId1"));

    activityService.register(accountId, getDeploymentActivity(verificationJob));

    Activity activity = hPersistence.createQuery(Activity.class)
                            .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                            .get();

    String id = activity.getUuid();

    Activity fromDb = activityService.get(id);

    assertThat(activity.getUuid()).isEqualTo(fromDb.getUuid());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetByVerificationJobInstanceId() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    when(verificationJobInstanceService.create(anyList())).thenReturn(Arrays.asList("taskId1"));

    activityService.register(accountId, getDeploymentActivity(verificationJob));

    Activity activity = hPersistence.createQuery(Activity.class)
                            .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                            .get();

    Activity fromDb = activityService.getByVerificationJobInstanceId("taskId1");

    assertThat(activity.getUuid()).isEqualTo(fromDb.getUuid());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetActivityVerificationResult() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    activityService.register(accountId, getDeploymentActivity(verificationJob));

    Activity activity = hPersistence.createQuery(Activity.class)
                            .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                            .get();

    String id = activity.getUuid();
    ActivityVerificationSummary summary = createActivitySummary(Instant.now());
    when(verificationJobInstanceService.getActivityVerificationSummary(anyList())).thenReturn(summary);
    Set<CategoryRisk> preActivityRisks = new HashSet<>();
    preActivityRisks.add(CategoryRisk.builder().category(CVMonitoringCategory.PERFORMANCE).risk(0.2).build());

    Set<CategoryRisk> postActivityRisks = new HashSet<>();
    postActivityRisks.add(CategoryRisk.builder().category(CVMonitoringCategory.PERFORMANCE).risk(0.7).build());

    when(healthVerificationHeatMapService.getAggregatedRisk(id, HealthVerificationPeriod.PRE_ACTIVITY))
        .thenReturn(preActivityRisks);

    when(healthVerificationHeatMapService.getAggregatedRisk(id, HealthVerificationPeriod.POST_ACTIVITY))
        .thenReturn(postActivityRisks);

    ActivityVerificationResultDTO resultDTO = activityService.getActivityVerificationResult(accountId, id);
    assertThat(resultDTO).isNotNull();
    assertThat(resultDTO.getActivityId()).isEqualTo(id);
    assertThat(resultDTO.getActivityType().name()).isEqualTo(activity.getType().name());
    assertThat(resultDTO.getOverallRisk()).isEqualTo(0);
    assertThat(resultDTO.getProgressPercentage()).isEqualTo(summary.getProgressPercentage());

    verify(healthVerificationHeatMapService).getAggregatedRisk(id, HealthVerificationPeriod.PRE_ACTIVITY);
    verify(healthVerificationHeatMapService).getAggregatedRisk(id, HealthVerificationPeriod.POST_ACTIVITY);
    verify(verificationJobInstanceService, times(1)).getActivityVerificationSummary(anyList());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetActivityVerificationResult_validateOverallRisk() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    activityService.register(accountId, getDeploymentActivity(verificationJob));

    Activity activity = hPersistence.createQuery(Activity.class)
                            .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                            .get();

    String id = activity.getUuid();
    ActivityVerificationSummary summary = createActivitySummary(Instant.now());
    when(verificationJobInstanceService.getActivityVerificationSummary(anyList())).thenReturn(summary);
    Set<CategoryRisk> preActivityRisks = new HashSet<>();
    preActivityRisks.add(CategoryRisk.builder().category(CVMonitoringCategory.PERFORMANCE).risk(0.2).build());
    preActivityRisks.add(CategoryRisk.builder().category(CVMonitoringCategory.ERRORS).risk(91.0).build());

    Set<CategoryRisk> postActivityRisks = new HashSet<>();
    postActivityRisks.add(CategoryRisk.builder().category(CVMonitoringCategory.PERFORMANCE).risk(23.0).build());
    postActivityRisks.add(CategoryRisk.builder().category(CVMonitoringCategory.ERRORS).risk(34.0).build());

    when(healthVerificationHeatMapService.getAggregatedRisk(id, HealthVerificationPeriod.PRE_ACTIVITY))
        .thenReturn(preActivityRisks);

    when(healthVerificationHeatMapService.getAggregatedRisk(id, HealthVerificationPeriod.POST_ACTIVITY))
        .thenReturn(postActivityRisks);

    ActivityVerificationResultDTO resultDTO = activityService.getActivityVerificationResult(accountId, id);
    assertThat(resultDTO).isNotNull();
    assertThat(resultDTO.getActivityId()).isEqualTo(id);
    assertThat(resultDTO.getActivityType().name()).isEqualTo(activity.getType().name());

    // overall risk should be max of post deployment risks
    assertThat(resultDTO.getOverallRisk()).isEqualTo(34);
    assertThat(resultDTO.getProgressPercentage()).isEqualTo(summary.getProgressPercentage());

    verify(healthVerificationHeatMapService).getAggregatedRisk(id, HealthVerificationPeriod.PRE_ACTIVITY);
    verify(healthVerificationHeatMapService).getAggregatedRisk(id, HealthVerificationPeriod.POST_ACTIVITY);
    verify(verificationJobInstanceService, times(1)).getActivityVerificationSummary(anyList());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetActivityVerificationResult_noRisks() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    activityService.register(accountId, getDeploymentActivity(verificationJob));

    Activity activity = hPersistence.createQuery(Activity.class)
                            .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                            .get();

    String id = activity.getUuid();
    ActivityVerificationSummary summary = createActivitySummary(Instant.now());
    when(verificationJobInstanceService.getActivityVerificationSummary(anyList())).thenReturn(summary);
    Set<CategoryRisk> preActivityRisks = new HashSet<>();

    Set<CategoryRisk> postActivityRisks = new HashSet<>();

    when(healthVerificationHeatMapService.getAggregatedRisk(id, HealthVerificationPeriod.PRE_ACTIVITY))
        .thenReturn(preActivityRisks);

    when(healthVerificationHeatMapService.getAggregatedRisk(id, HealthVerificationPeriod.POST_ACTIVITY))
        .thenReturn(postActivityRisks);

    ActivityVerificationResultDTO resultDTO = activityService.getActivityVerificationResult(accountId, id);
    assertThat(resultDTO).isNotNull();
    assertThat(resultDTO.getActivityId()).isEqualTo(id);
    assertThat(resultDTO.getActivityType().name()).isEqualTo(activity.getType().name());

    // overall risk should be max of post deployment risks
    assertThat(resultDTO.getOverallRisk()).isEqualTo(-1);
    assertThat(resultDTO.getProgressPercentage()).isEqualTo(summary.getProgressPercentage());

    verify(healthVerificationHeatMapService).getAggregatedRisk(id, HealthVerificationPeriod.PRE_ACTIVITY);
    verify(healthVerificationHeatMapService).getAggregatedRisk(id, HealthVerificationPeriod.POST_ACTIVITY);
    verify(verificationJobInstanceService, times(1)).getActivityVerificationSummary(anyList());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetActivityVerificationResult_noSummary() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    activityService.register(accountId, getDeploymentActivity(verificationJob));

    Activity activity = hPersistence.createQuery(Activity.class)
                            .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                            .get();

    String id = activity.getUuid();
    ActivityVerificationSummary summary = createActivitySummary(Instant.now());
    when(verificationJobInstanceService.getActivityVerificationSummary(anyList())).thenReturn(null);
    Set<CategoryRisk> preActivityRisks = new HashSet<>();
    preActivityRisks.add(CategoryRisk.builder().category(CVMonitoringCategory.PERFORMANCE).risk(20.0).build());

    Set<CategoryRisk> postActivityRisks = new HashSet<>();
    postActivityRisks.add(CategoryRisk.builder().category(CVMonitoringCategory.PERFORMANCE).risk(70.0).build());

    when(healthVerificationHeatMapService.getAggregatedRisk(id, HealthVerificationPeriod.PRE_ACTIVITY))
        .thenReturn(preActivityRisks);

    when(healthVerificationHeatMapService.getAggregatedRisk(id, HealthVerificationPeriod.POST_ACTIVITY))
        .thenReturn(postActivityRisks);

    ActivityVerificationResultDTO resultDTO = activityService.getActivityVerificationResult(accountId, id);
    assertThat(resultDTO).isNull();

    verify(healthVerificationHeatMapService, never()).getAggregatedRisk(id, HealthVerificationPeriod.PRE_ACTIVITY);
    verify(healthVerificationHeatMapService, never()).getAggregatedRisk(id, HealthVerificationPeriod.POST_ACTIVITY);
    verify(verificationJobInstanceService, times(1)).getActivityVerificationSummary(anyList());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetRecentActivityVerificationResults() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    when(verificationJobInstanceService.dedupCreate(anyList())).thenReturn(Lists.newArrayList(generateUuid()));
    instant = Instant.now();
    activityService.register(accountId, getInfrastructureActivity(verificationJob));
    activityService.register(accountId, getInfrastructureActivity(verificationJob));

    ActivityVerificationSummary summary = createActivitySummary(Instant.now());
    when(verificationJobInstanceService.getActivityVerificationSummary(anyList())).thenReturn(summary);
    Set<CategoryRisk> preActivityRisks = new HashSet<>();
    preActivityRisks.add(CategoryRisk.builder().category(CVMonitoringCategory.PERFORMANCE).risk(1.0).build());

    Set<CategoryRisk> postActivityRisks = new HashSet<>();
    postActivityRisks.add(CategoryRisk.builder().category(CVMonitoringCategory.PERFORMANCE).risk(0.7).build());

    when(healthVerificationHeatMapService.getAggregatedRisk(anyString(), eq(HealthVerificationPeriod.PRE_ACTIVITY)))
        .thenReturn(preActivityRisks);

    when(healthVerificationHeatMapService.getAggregatedRisk(anyString(), eq(HealthVerificationPeriod.POST_ACTIVITY)))
        .thenReturn(postActivityRisks);

    List<ActivityVerificationResultDTO> resultDTO =
        activityService.getRecentActivityVerificationResults(accountId, orgIdentifier, projectIdentifier, 3);
    assertThat(resultDTO).isNotNull();
    assertThat(resultDTO.size()).isEqualTo(2);

    List<Activity> activity = hPersistence.createQuery(Activity.class)
                                  .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                                  .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                                  .asList();

    List<String> ids = activity.stream().map(Activity::getUuid).collect(Collectors.toList());

    resultDTO.forEach(result -> {
      assertThat(ids.contains(result.getActivityId())).isTrue();
      assertThat(result.getActivityType().name()).isEqualTo(ActivityType.INFRASTRUCTURE.name());
      assertThat(result.getOverallRisk()).isEqualTo(0);
      assertThat(result.getProgressPercentage()).isEqualTo(summary.getProgressPercentage());
    });

    verify(healthVerificationHeatMapService, times(2))
        .getAggregatedRisk(anyString(), eq(HealthVerificationPeriod.PRE_ACTIVITY));
    verify(healthVerificationHeatMapService, times(2))
        .getAggregatedRisk(anyString(), eq(HealthVerificationPeriod.POST_ACTIVITY));
    verify(verificationJobInstanceService, times(2)).getActivityVerificationSummary(anyList());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testListActivitiesInTimeRange() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);

    ActivityVerificationSummary summary = createActivitySummary(Instant.now());
    when(verificationJobInstanceService.getActivityVerificationSummary(anyList())).thenReturn(summary);
    when(verificationJobInstanceService.create(anyList())).thenAnswer(invocationOnMock -> {
      List<VerificationJobInstance> verificationJobInstances =
          (List<VerificationJobInstance>) invocationOnMock.getArguments()[0];
      return hPersistence.save(verificationJobInstances);
    });

    instant = Instant.now();
    activityService.register(accountId, getDeploymentActivity(verificationJob));
    activityService.register(accountId, getDeploymentActivity(verificationJob));
    List<ActivityDashboardDTO> dashboardDTOList =
        activityService.listActivitiesInTimeRange(builderFactory.getContext().getProjectParams(), serviceIdentifier,
            envIdentifier, Instant.now().minus(15, ChronoUnit.MINUTES), Instant.now().plus(15, ChronoUnit.MINUTES));

    assertThat(dashboardDTOList.size()).isEqualTo(2);
    List<Activity> activity = hPersistence.createQuery(Activity.class, excludeAuthority)
                                  .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                                  .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                                  .asList();

    List<String> ids = activity.stream().map(Activity::getUuid).collect(Collectors.toList());

    dashboardDTOList.forEach(dashboardDTO -> {
      assertThat(ids.contains(dashboardDTO.getActivityId())).isTrue();
      assertThat(dashboardDTO.getActivityType().name()).isEqualTo(ActivityType.DEPLOYMENT.name());
      assertThat(dashboardDTO.getEnvironmentIdentifier()).isEqualTo(envIdentifier);
      assertThat(dashboardDTO.getVerificationStatus().name()).isEqualTo(summary.getAggregatedStatus().name());
    });

    verify(verificationJobInstanceService, times(2)).getActivityVerificationSummary(anyList());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetDeploymentSummary() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    DeploymentActivityDTO deploymentActivityDTO = getDeploymentActivity(verificationJob);
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance();
    CVConfig cvConfig = verificationJobInstance.getCvConfigMap().values().iterator().next();
    String verificationJobInstanceId = realVerificationJobInstanceService.create(verificationJobInstance);
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfig.getUuid(), verificationJobInstanceId, cvConfig.getType());

    deploymentTimeSeriesAnalysisService.save(createDeploymentTimeSeriesAnalysis(verificationTaskId));

    String activityId = activityService.register(accountId, deploymentActivityDTO);
    Activity activity = hPersistence.get(Activity.class, activityId);
    activity.setVerificationJobInstanceIds(Arrays.asList(verificationJobInstanceId));
    hPersistence.save(activity);

    DeploymentActivityResultDTO.DeploymentVerificationJobInstanceSummary deploymentVerificationJobInstanceSummary =
        DeploymentActivityResultDTO.DeploymentVerificationJobInstanceSummary.builder()
            .environmentName("env name")
            .build();
    when(verificationJobInstanceService.getDeploymentVerificationJobInstanceSummary(anyList()))
        .thenReturn(deploymentVerificationJobInstanceSummary);
    assertThat(deploymentVerificationJobInstanceSummary.getActivityId()).isNull();
    DeploymentActivitySummaryDTO deploymentActivitySummaryDTO = activityService.getDeploymentSummary(activityId);
    assertThat(deploymentActivitySummaryDTO.getServiceIdentifier())
        .isEqualTo(deploymentActivityDTO.getServiceIdentifier());
    assertThat(deploymentActivitySummaryDTO.getDeploymentTag()).isEqualTo(deploymentActivityDTO.getDeploymentTag());
    assertThat(deploymentActivitySummaryDTO.getEnvIdentifier())
        .isEqualTo(deploymentActivityDTO.getEnvironmentIdentifier());
    assertThat(deploymentActivitySummaryDTO.getServiceName()).isEqualTo("service name");
    assertThat(deploymentActivitySummaryDTO.getEnvName()).isEqualTo("env name");
    assertThat(deploymentVerificationJobInstanceSummary.getActivityId()).isEqualTo(activityId);
    assertThat(deploymentVerificationJobInstanceSummary.getActivityStartTime())
        .isEqualTo(deploymentActivityDTO.getActivityStartTime());
    assertThat(deploymentVerificationJobInstanceSummary.getTimeSeriesAnalysisSummary()).isNotNull();
    assertThat(deploymentVerificationJobInstanceSummary.getTimeSeriesAnalysisSummary().getTotalNumMetrics())
        .isEqualTo(2);
    assertThat(deploymentVerificationJobInstanceSummary.getTimeSeriesAnalysisSummary().getNumAnomMetrics())
        .isEqualTo(1);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetActivityStatus() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    DeploymentActivityDTO deploymentActivityDTO = getDeploymentActivity(verificationJob);
    String activityId = activityService.register(accountId, deploymentActivityDTO);
    DeploymentActivityResultDTO.DeploymentVerificationJobInstanceSummary deploymentVerificationJobInstanceSummary =
        DeploymentActivityResultDTO.DeploymentVerificationJobInstanceSummary.builder()
            .durationMs(verificationJob.getDuration().toMillis())
            .status(ActivityVerificationStatus.NOT_STARTED)
            .build();
    when(verificationJobInstanceService.getDeploymentVerificationJobInstanceSummary(anyList()))
        .thenReturn(deploymentVerificationJobInstanceSummary);
    assertThat(deploymentVerificationJobInstanceSummary.getActivityId()).isNull();
    ActivityStatusDTO activityStatusDTO = activityService.getActivityStatus(accountId, activityId);
    assertThat(activityStatusDTO.getActivityId()).isEqualTo(activityId);
    assertThat(activityStatusDTO.getDurationMs()).isEqualTo(verificationJob.getDuration().toMillis());
    assertThat(activityStatusDTO.getStatus()).isEqualTo(ActivityVerificationStatus.NOT_STARTED);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category({UnitTests.class})
  public void testCreateVerificationJobInstancesForActivity_defaultJob() throws IllegalAccessException {
    FieldUtils.writeField(activityService, "verificationJobService", realVerificationJobService, true);
    realVerificationJobInstanceService = spy(realVerificationJobInstanceService);
    doReturn(Lists.newArrayList(new AppDynamicsCVConfig()))
        .when(realVerificationJobInstanceService)
        .getCVConfigsForVerificationJob(any());

    FieldUtils.writeField(activityService, "verificationJobInstanceService", realVerificationJobInstanceService, true);

    KubernetesActivity kubernetesActivity = getKubernetesActivity();
    kubernetesActivity.setVerificationJobRuntimeDetails(null);

    assertThat(activityService.createVerificationJobInstancesForActivity(kubernetesActivity)).isNotEmpty();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testCreateVerificationJobInstancesForActivity_whenHealthJob() throws IllegalAccessException {
    FieldUtils.writeField(activityService, "verificationJobService", realVerificationJobService, true);
    realVerificationJobInstanceService = spy(realVerificationJobInstanceService);
    doReturn(Lists.newArrayList(new AppDynamicsCVConfig()))
        .when(realVerificationJobInstanceService)
        .getCVConfigsForVerificationJob(any());
    FieldUtils.writeField(activityService, "verificationJobInstanceService", realVerificationJobInstanceService, true);
    KubernetesActivity kubernetesActivity = getKubernetesActivity();
    realVerificationJobService.save(HealthVerificationJob.builder()
                                        .accountId(accountId)
                                        .jobName("job-name")
                                        .orgIdentifier(kubernetesActivity.getOrgIdentifier())
                                        .projectIdentifier(kubernetesActivity.getProjectIdentifier())
                                        .envIdentifier(RuntimeParameter.builder()
                                                           .isRuntimeParam(false)
                                                           .value(kubernetesActivity.getEnvironmentIdentifier())
                                                           .build())
                                        .serviceIdentifier(RuntimeParameter.builder()
                                                               .isRuntimeParam(false)
                                                               .value(kubernetesActivity.getServiceIdentifier())
                                                               .build())
                                        .duration(RuntimeParameter.builder().isRuntimeParam(false).value("30m").build())
                                        .dataSources(Lists.newArrayList(DataSourceType.APP_DYNAMICS))
                                        .type(VerificationJobType.HEALTH)
                                        .identifier(generateUuid())
                                        .build());
    kubernetesActivity.setVerificationJobRuntimeDetails(null);
    assertThat(activityService.createVerificationJobInstancesForActivity(kubernetesActivity).size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testCreateVerificationJobInstancesForActivity_whenHealthJobRunning() throws IllegalAccessException {
    FieldUtils.writeField(activityService, "verificationJobService", realVerificationJobService, true);
    FieldUtils.writeField(activityService, "verificationJobInstanceService", realVerificationJobInstanceService, true);
    KubernetesActivity kubernetesActivity = getKubernetesActivity();
    HealthVerificationJob healthVerificationJob =
        HealthVerificationJob.builder()
            .accountId(accountId)
            .jobName("job-name")
            .orgIdentifier(kubernetesActivity.getOrgIdentifier())
            .projectIdentifier(kubernetesActivity.getProjectIdentifier())
            .identifier(generateUuid())
            .envIdentifier(RuntimeParameter.builder()
                               .isRuntimeParam(false)
                               .value(kubernetesActivity.getEnvironmentIdentifier())
                               .build())
            .serviceIdentifier(RuntimeParameter.builder()
                                   .isRuntimeParam(false)
                                   .value(kubernetesActivity.getServiceIdentifier())
                                   .build())
            .duration(RuntimeParameter.builder().isRuntimeParam(false).value("30m").build())
            .dataSources(Lists.newArrayList(DataSourceType.APP_DYNAMICS))
            .type(VerificationJobType.HEALTH)
            .build();
    realVerificationJobService.save(healthVerificationJob);
    realVerificationJobInstanceService.create(builderFactory.verificationJobInstanceBuilder()
                                                  .accountId(kubernetesActivity.getAccountId())
                                                  .deploymentStartTime(Instant.now())
                                                  .startTime(Instant.now())
                                                  .resolvedJob(healthVerificationJob)
                                                  .executionStatus(ExecutionStatus.RUNNING)
                                                  .startTime(Instant.now())
                                                  .build());
    assertThat(activityService.createVerificationJobInstancesForActivity(kubernetesActivity)).isNull();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testCreateVerificationJobInstancesForActivity_whenHealthJobSuccess() throws IllegalAccessException {
    FieldUtils.writeField(activityService, "verificationJobService", realVerificationJobService, true);
    realVerificationJobInstanceService = spy(realVerificationJobInstanceService);
    doReturn(Lists.newArrayList(new AppDynamicsCVConfig()))
        .when(realVerificationJobInstanceService)
        .getCVConfigsForVerificationJob(any());
    FieldUtils.writeField(activityService, "verificationJobInstanceService", realVerificationJobInstanceService, true);
    KubernetesActivity kubernetesActivity = getKubernetesActivity();
    HealthVerificationJob healthVerificationJob =
        HealthVerificationJob.builder()
            .accountId(accountId)
            .jobName("job-name")
            .orgIdentifier(kubernetesActivity.getOrgIdentifier())
            .projectIdentifier(kubernetesActivity.getProjectIdentifier())
            .envIdentifier(RuntimeParameter.builder()
                               .isRuntimeParam(false)
                               .value(kubernetesActivity.getEnvironmentIdentifier())
                               .build())
            .serviceIdentifier(RuntimeParameter.builder()
                                   .isRuntimeParam(false)
                                   .value(kubernetesActivity.getServiceIdentifier())
                                   .build())
            .duration(RuntimeParameter.builder().isRuntimeParam(false).value("30m").build())
            .dataSources(Lists.newArrayList(DataSourceType.APP_DYNAMICS))
            .type(VerificationJobType.HEALTH)
            .identifier(generateUuid())
            .build();
    realVerificationJobService.save(healthVerificationJob);
    realVerificationJobInstanceService.create(builderFactory.verificationJobInstanceBuilder()
                                                  .accountId(kubernetesActivity.getAccountId())
                                                  .resolvedJob(healthVerificationJob)
                                                  .executionStatus(ExecutionStatus.SUCCESS)
                                                  .build());
    assertThat(activityService.createVerificationJobInstancesForActivity(kubernetesActivity)).hasSize(1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpdateActivityStatus_passed() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    instant = Instant.now();

    activityService.register(accountId, getDeploymentActivity(verificationJob));
    activityService.register(accountId, getDeploymentActivity(verificationJob));

    List<Activity> activities = hPersistence.createQuery(Activity.class).asList();
    activities.get(0).setVerificationJobInstanceIds(Arrays.asList("jobId1"));
    activities.get(1).setVerificationJobInstanceIds(Arrays.asList("jobId2"));

    assertThat(activities.get(0).getAnalysisStatus().name()).isEqualTo(ActivityVerificationStatus.NOT_STARTED.name());
    assertThat(activities.get(0).getVerificationSummary()).isNull();

    ActivityVerificationSummary summary = createActivitySummary(Instant.now());
    summary.setTotal(1);
    summary.setPassed(1);
    summary.setProgress(0);

    List<VerificationJobInstance> jobInstances1 = Arrays.asList(builderFactory.verificationJobInstanceBuilder()
                                                                    .deploymentStartTime(Instant.now())
                                                                    .startTime(Instant.now())
                                                                    .uuid("jobId1")
                                                                    .build());
    when(verificationJobInstanceService.get(Arrays.asList("jobId1"))).thenReturn(jobInstances1);

    when(verificationJobInstanceService.getActivityVerificationSummary(jobInstances1)).thenReturn(summary);
    // when(verificationJobInstanceService.getActivityVerificationSummary(jobInstances1)).thenReturn(createActivitySummary(Instant.now()));

    activityService.updateActivityStatus(activities.get(0));

    activities = hPersistence.createQuery(Activity.class).asList();

    assertThat(activities.get(0).getAnalysisStatus().name())
        .isEqualTo(ActivityVerificationStatus.VERIFICATION_PASSED.name());
    assertThat(activities.get(0).getVerificationSummary()).isNotNull();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testAbort_inNotStarted() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    instant = Instant.now();
    String activityId = activityService.register(accountId, getDeploymentActivity(verificationJob));
    Activity activity = activityService.get(activityId);
    List<String> verificationJobs = Lists.newArrayList("JOB_INSTANCE_ID");
    activity.setVerificationJobInstanceIds(verificationJobs);
    hPersistence.save(activity);

    activityService.abort(activityId);

    Activity updatedActivity = activityService.get(activityId);
    verify(verificationJobInstanceService).abort(Lists.newArrayList(updatedActivity.getVerificationJobInstanceIds()));
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testAbort_inError() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    instant = Instant.now();
    String activityId = activityService.register(accountId, getDeploymentActivity(verificationJob));
    Activity activity = activityService.get(activityId);
    activity.setAnalysisStatus(ActivityVerificationStatus.ERROR);
    hPersistence.save(activity);

    activityService.abort(activityId);

    Activity updatedActivity = activityService.get(activityId);
    // assert that errored activity is not aborted
    assertThat(updatedActivity.getAnalysisStatus()).isEqualTo(ActivityVerificationStatus.ERROR);
    verify(verificationJobInstanceService, never()).abort(any());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpdateActivityStatus_inProgress() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    instant = Instant.now();

    activityService.register(accountId, getDeploymentActivity(verificationJob));
    activityService.register(accountId, getDeploymentActivity(verificationJob));

    List<Activity> activities = hPersistence.createQuery(Activity.class).asList();
    activities.get(0).setVerificationJobInstanceIds(Arrays.asList("jobId1"));
    activities.get(1).setVerificationJobInstanceIds(Arrays.asList("jobId2"));

    assertThat(activities.get(0).getAnalysisStatus().name()).isEqualTo(ActivityVerificationStatus.NOT_STARTED.name());
    assertThat(activities.get(0).getVerificationSummary()).isNull();

    ActivityVerificationSummary summary = createActivitySummary(Instant.now());
    summary.setTotal(1);
    summary.setPassed(1);
    summary.setProgress(0);

    List<VerificationJobInstance> jobInstances1 = Arrays.asList(builderFactory.verificationJobInstanceBuilder()
                                                                    .deploymentStartTime(Instant.now())
                                                                    .startTime(Instant.now())
                                                                    .uuid("jobId1")
                                                                    .build());
    when(verificationJobInstanceService.get(Arrays.asList("jobId1"))).thenReturn(jobInstances1);

    when(verificationJobInstanceService.getActivityVerificationSummary(jobInstances1)).thenReturn(summary);
    when(verificationJobInstanceService.getActivityVerificationSummary(anyList()))
        .thenReturn(createActivitySummary(Instant.now()));

    activityService.updateActivityStatus(activities.get(1));

    activities = hPersistence.createQuery(Activity.class).asList();

    assertThat(activities.get(0).getAnalysisStatus().name()).isEqualTo(ActivityVerificationStatus.NOT_STARTED.name());
    assertThat(activities.get(0).getVerificationSummary()).isNull();

    verify(alertRuleService, times(0))
        .processDeploymentVerification(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier,
            ActivityType.DEPLOYMENT, VerificationStatus.getVerificationStatus(ActivityVerificationStatus.NOT_STARTED),
            123456L, 123456L, "tag");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreateActivityForDemo() {
    List<String> verificationJobInstanceIds = Collections.singletonList(generateUuid());
    when(verificationJobInstanceService.createDemoInstances(anyList())).thenReturn(verificationJobInstanceIds);
    VerificationJob verificationJob = createVerificationJob();
    DeploymentActivity deploymentActivity =
        DeploymentActivity.builder()
            .deploymentTag("tag")
            .verificationStartTime(builderFactory.getClock().instant().minus(Duration.ofMinutes(5)).toEpochMilli())
            .build();
    deploymentActivity.setVerificationJobs(Arrays.asList(verificationJob));
    deploymentActivity.setActivityStartTime(builderFactory.getClock().instant().minus(Duration.ofMinutes(10)));
    deploymentActivity.setOrgIdentifier(orgIdentifier);
    deploymentActivity.setAccountId(accountId);
    deploymentActivity.setProjectIdentifier(projectIdentifier);
    deploymentActivity.setServiceIdentifier(serviceIdentifier);
    deploymentActivity.setEnvironmentIdentifier(envIdentifier);
    deploymentActivity.setActivityName("CDNG demo activity");
    deploymentActivity.setType(ActivityType.DEPLOYMENT);
    String activityId =
        activityService.createActivityForDemo(deploymentActivity, ActivityVerificationStatus.VERIFICATION_FAILED);
    Activity activity = activityService.get(activityId);
    assertThat(activity).isNotNull();
    assertThat(activity.getVerificationJobInstanceIds()).isEqualTo(verificationJobInstanceIds);
  }

  private DeploymentActivityDTO getDeploymentActivity(VerificationJob verificationJob) {
    List<VerificationJobRuntimeDetails> verificationJobDetails = new ArrayList<>();
    Map<String, String> runtimeParams = new HashMap<>();
    runtimeParams.put(JOB_IDENTIFIER_KEY, verificationJob.getIdentifier());
    runtimeParams.put(SERVICE_IDENTIFIER_KEY, "cvngService");
    runtimeParams.put(ENV_IDENTIFIER_KEY, "production");
    VerificationJobRuntimeDetails runtimeDetails = VerificationJobRuntimeDetails.builder()
                                                       .verificationJobIdentifier(verificationJob.getIdentifier())
                                                       .runtimeValues(runtimeParams)
                                                       .build();
    verificationJobDetails.add(runtimeDetails);
    DeploymentActivityDTO activityDTO =
        getDeploymentActivityDTO(verificationJobDetails, instant, deploymentTag, envIdentifier, serviceIdentifier);
    return activityDTO;
  }

  private DeploymentActivityDTO getDeploymentActivityDTO(List<VerificationJobRuntimeDetails> verificationJobDetails,
      Instant verificationStartTime, String deploymentTag, String envIdentifier, String serviceIdentifier) {
    DeploymentActivityDTO activityDTO = DeploymentActivityDTO.builder()
                                            .dataCollectionDelayMs(2000l)
                                            .newVersionHosts(new HashSet<>(Arrays.asList("node1", "node2")))
                                            .oldVersionHosts(new HashSet<>(Arrays.asList("node3", "node4")))
                                            .verificationStartTime(verificationStartTime.toEpochMilli())
                                            .deploymentTag(deploymentTag)
                                            .build();
    activityDTO.setAccountIdentifier(accountId);
    activityDTO.setProjectIdentifier(projectIdentifier);
    activityDTO.setOrgIdentifier(orgIdentifier);
    activityDTO.setActivityStartTime(verificationStartTime.toEpochMilli());
    activityDTO.setEnvironmentIdentifier(envIdentifier);
    activityDTO.setName("Build 23 deploy");
    activityDTO.setVerificationJobRuntimeDetails(verificationJobDetails);
    activityDTO.setServiceIdentifier(serviceIdentifier);
    activityDTO.setTags(Arrays.asList("build88", "prod deploy"));
    return activityDTO;
  }

  private KubernetesActivity getKubernetesActivity() {
    KubernetesActivity activity = KubernetesActivity.builder().build();
    activity.setAccountId(accountId);
    activity.setProjectIdentifier(projectIdentifier);
    activity.setOrgIdentifier(orgIdentifier);
    activity.setActivityStartTime(Instant.now());
    activity.setEnvironmentIdentifier(envIdentifier);
    activity.setServiceIdentifier(generateUuid());
    return activity;
  }

  private InfrastructureActivityDTO getInfrastructureActivity(VerificationJob verificationJob) {
    InfrastructureActivityDTO activityDTO = InfrastructureActivityDTO.builder().message("pod restarts").build();
    activityDTO.setAccountIdentifier(accountId);
    activityDTO.setProjectIdentifier(projectIdentifier);
    activityDTO.setOrgIdentifier(orgIdentifier);
    activityDTO.setActivityStartTime(Instant.now().toEpochMilli());
    activityDTO.setEnvironmentIdentifier(envIdentifier);
    activityDTO.setName("Pod restart activity");
    activityDTO.setServiceIdentifier(generateUuid());
    activityDTO.setMessage(generateUuid());

    Map<String, String> runtimeParams = new HashMap<>();
    runtimeParams.put(JOB_IDENTIFIER_KEY, verificationJob.getIdentifier());

    VerificationJobRuntimeDetails runtimeDetails = VerificationJobRuntimeDetails.builder()
                                                       .verificationJobIdentifier(verificationJob.getIdentifier())
                                                       .runtimeValues(runtimeParams)
                                                       .build();
    List<VerificationJobRuntimeDetails> verificationJobDetails = new ArrayList<>();
    verificationJobDetails.add(runtimeDetails);

    activityDTO.setVerificationJobRuntimeDetails(verificationJobDetails);
    return activityDTO;
  }

  private VerificationJob createVerificationJob() {
    CanaryVerificationJob testVerificationJob = new CanaryVerificationJob();
    testVerificationJob.setUuid(generateUuid());
    testVerificationJob.setAccountId(accountId);
    testVerificationJob.setIdentifier("identifier");
    testVerificationJob.setJobName(generateUuid());
    testVerificationJob.setMonitoringSources(Collections.singletonList("monitoringIdentifier"));
    testVerificationJob.setSensitivity(Sensitivity.MEDIUM);
    testVerificationJob.setServiceIdentifier(serviceIdentifier, false);
    testVerificationJob.setEnvIdentifier(envIdentifier, false);
    testVerificationJob.setDuration(Duration.ofMinutes(5));
    testVerificationJob.setProjectIdentifier(projectIdentifier);
    testVerificationJob.setOrgIdentifier(orgIdentifier);
    return testVerificationJob;
  }

  private ActivityVerificationSummary createActivitySummary(Instant startTime) {
    return ActivityVerificationSummary.builder()
        .total(1)
        .startTime(startTime.toEpochMilli())
        .risk(Risk.MEDIUM)
        .progress(1)
        .notStarted(0)
        .durationMs(Duration.ofMinutes(15).toMillis())
        .remainingTimeMs(1200000)
        .progressPercentage(25)
        .build();
  }

  private CD10ActivitySourceDTO createCD10ActivitySource(String appId, String envId, String serviceId) {
    Set<CD10EnvMappingDTO> cd10EnvMappingDTOS = new HashSet<>();
    Set<CD10ServiceMappingDTO> cd10ServiceMappingDTOS = new HashSet<>();
    cd10EnvMappingDTOS.add(createEnvMapping(appId, envId, generateUuid()));
    cd10ServiceMappingDTOS.add(createServiceMapping(appId, serviceId, generateUuid()));
    return CD10ActivitySourceDTO.builder()
        .identifier(CD10ActivitySource.HARNESS_CD_10_ACTIVITY_SOURCE_IDENTIFIER)
        .name("some-name")
        .envMappings(cd10EnvMappingDTOS)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .serviceMappings(cd10ServiceMappingDTOS)
        .build();
  }
  private CD10EnvMappingDTO createEnvMapping(String appId, String envId, String envIdentifier) {
    return CD10EnvMappingDTO.builder().appId(appId).envId(envId).envIdentifier(envIdentifier).build();
  }

  private CD10ServiceMappingDTO createServiceMapping(String appId, String serviceId, String serviceIdentifier) {
    return CD10ServiceMappingDTO.builder()
        .appId(appId)
        .serviceId(serviceId)
        .serviceIdentifier(serviceIdentifier)
        .build();
  }

  private CVConfig createCVConfig() {
    CVConfig cvConfig = builderFactory.appDynamicsCVConfigBuilder().build();
    return cvConfigService.save(cvConfig);
  }

  private VerificationJobInstance createVerificationJobInstance() {
    VerificationJobInstance jobInstance = builderFactory.verificationJobInstanceBuilder().build();
    jobInstance.setAccountId(accountId);
    return jobInstance;
  }

  private DeploymentTimeSeriesAnalysisDTO.HostInfo createHostInfo(
      String hostName, int risk, Double score, boolean primary, boolean canary) {
    return DeploymentTimeSeriesAnalysisDTO.HostInfo.builder()
        .hostName(hostName)
        .risk(risk)
        .score(score)
        .primary(primary)
        .canary(canary)
        .build();
  }
  private DeploymentTimeSeriesAnalysisDTO.HostData createHostData(
      String hostName, int risk, Double score, List<Double> controlData, List<Double> testData) {
    return DeploymentTimeSeriesAnalysisDTO.HostData.builder()
        .hostName(hostName)
        .risk(risk)
        .score(score)
        .controlData(controlData)
        .testData(testData)
        .build();
  }

  private DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData createTransactionMetricHostData(
      String transactionName, String metricName, int risk, Double score,
      List<DeploymentTimeSeriesAnalysisDTO.HostData> hostDataList) {
    return DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData.builder()
        .transactionName(transactionName)
        .metricName(metricName)
        .risk(risk)
        .score(score)
        .hostData(hostDataList)
        .build();
  }
  private DeploymentTimeSeriesAnalysis createDeploymentTimeSeriesAnalysis(String verificationTaskId) {
    DeploymentTimeSeriesAnalysisDTO.HostInfo hostInfo1 = createHostInfo("node1", 1, 1.1, false, true);
    DeploymentTimeSeriesAnalysisDTO.HostInfo hostInfo2 = createHostInfo("node2", 2, 2.2, false, true);
    DeploymentTimeSeriesAnalysisDTO.HostInfo hostInfo3 = createHostInfo("node3", 2, 2.2, false, true);
    DeploymentTimeSeriesAnalysisDTO.HostData hostData1 =
        createHostData("node1", 0, 0.0, Arrays.asList(1D), Arrays.asList(1D));
    DeploymentTimeSeriesAnalysisDTO.HostData hostData2 =
        createHostData("node2", 2, 2.0, Arrays.asList(1D), Arrays.asList(1D));

    DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData transactionMetricHostData1 =
        createTransactionMetricHostData(
            "/todolist/inside", "Errors per Minute", 0, 0.5, Arrays.asList(hostData1, hostData2));

    DeploymentTimeSeriesAnalysisDTO.HostData hostData3 =
        createHostData("node1", 0, 0.0, Arrays.asList(1D), Arrays.asList(1D));
    DeploymentTimeSeriesAnalysisDTO.HostData hostData4 =
        createHostData("node2", 2, 2.0, Arrays.asList(1D), Arrays.asList(1D));
    DeploymentTimeSeriesAnalysisDTO.HostData hostData5 =
        createHostData("node3", 2, 2.0, Arrays.asList(1D), Arrays.asList(1D));

    DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData transactionMetricHostData2 =
        createTransactionMetricHostData(
            "/todolist/exception", "Calls per Minute", 2, 2.5, Arrays.asList(hostData3, hostData4, hostData5));
    return DeploymentTimeSeriesAnalysis.builder()
        .accountId(accountId)
        .score(.7)
        .risk(Risk.MEDIUM)
        .verificationTaskId(verificationTaskId)
        .transactionMetricSummaries(Arrays.asList(transactionMetricHostData1, transactionMetricHostData2))
        .hostSummaries(Arrays.asList(hostInfo1, hostInfo2, hostInfo3))
        .startTime(Instant.now())
        .endTime(Instant.now().plus(1, ChronoUnit.MINUTES))
        .build();
  }
}
