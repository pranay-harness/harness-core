package io.harness.cvng.activity.services.impl;

import static io.harness.cvng.verificationjob.CVVerificationJobConstants.ENV_IDENTIFIER_KEY;
import static io.harness.cvng.verificationjob.CVVerificationJobConstants.JOB_IDENTIFIER_KEY;
import static io.harness.cvng.verificationjob.CVVerificationJobConstants.SERVICE_IDENTIFIER_KEY;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.NEMANJA;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.VUK;

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
import io.harness.cvng.activity.source.services.api.ActivitySourceService;
import io.harness.cvng.alert.services.api.AlertRuleService;
import io.harness.cvng.alert.util.VerificationStatus;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.analysis.entities.HealthVerificationPeriod;
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
import io.harness.cvng.beans.activity.cd10.CD10RegisterActivityDTO;
import io.harness.cvng.beans.activity.cd10.CD10ServiceMappingDTO;
import io.harness.cvng.beans.job.Sensitivity;
import io.harness.cvng.beans.job.VerificationJobType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.services.api.WebhookService;
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
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ActivityServiceImplTest extends CvNextGenTestBase {
  @Inject private HPersistence hPersistence;
  @Inject private ActivityService activityService;
  @Inject private VerificationJobService realVerificationJobService;
  @Inject private VerificationJobInstanceService realVerificationJobInstanceService;
  @Mock private WebhookService mockWebhookService;
  @Mock private VerificationJobService verificationJobService;
  @Mock private VerificationJobInstanceService verificationJobInstanceService;
  @Mock private HealthVerificationHeatMapService healthVerificationHeatMapService;
  @Mock private NextGenService nextGenService;
  @Inject private ActivitySourceService activitySourceService;
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

    FieldUtils.writeField(activityService, "webhookService", mockWebhookService, true);
    FieldUtils.writeField(activityService, "verificationJobService", verificationJobService, true);
    FieldUtils.writeField(activityService, "verificationJobInstanceService", verificationJobInstanceService, true);
    FieldUtils.writeField(activityService, "nextGenService", nextGenService, true);
    FieldUtils.writeField(activityService, "healthVerificationHeatMapService", healthVerificationHeatMapService, true);
    FieldUtils.writeField(activityService, "alertRuleService", alertRuleService, true);
    when(nextGenService.getService(any(), any(), any(), any()))
        .thenReturn(ServiceResponseDTO.builder().name("service name").build());
    when(mockWebhookService.validateWebhookToken(any(), any(), any())).thenReturn(true);
    when(verificationJobInstanceService.getCVConfigsForVerificationJob(any()))
        .thenReturn(Lists.newArrayList(new AppDynamicsCVConfig()));
    realVerificationJobService.createDefaultVerificationJobs(accountId, orgIdentifier, projectIdentifier);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testRegisterActivity_whenNoJobExists() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenAnswer(invocationOnMock -> { return null; });
    assertThatThrownBy(
        () -> activityService.register(accountId, generateUuid(), getDeploymentActivity(verificationJob)))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("No Job exists for verificationJobIdentifier: 'identifier'");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testRegisterActivity_whenNoCvConfigExists() {
    VerificationJob verificationJob = createVerificationJob();
    List<String> monitoringSourceIdentifiers = Collections.singletonList("monitoringSourceIdentifier");
    verificationJob.setMonitoringSources(monitoringSourceIdentifiers);
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    when(verificationJobInstanceService.getCVConfigsForVerificationJob(any())).thenReturn(Lists.newArrayList());
    DeploymentActivityDTO deploymentActivity = getDeploymentActivity(verificationJob);
    assertThatThrownBy(() -> activityService.register(accountId, generateUuid(), deploymentActivity))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No monitoring sources with identifiers " + verificationJob.getMonitoringSources()
            + " defined for environment " + verificationJob.getEnvIdentifier() + " and service "
            + verificationJob.getServiceIdentifier());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testRegisterCD10Activity_ifCD10FieldsAreNotPresent() {
    DeploymentActivityDTO deploymentActivity = getDeploymentActivity(createVerificationJob());
    deploymentActivity.setEnvironmentIdentifier(null);
    deploymentActivity.setServiceIdentifier(null);
    assertThatThrownBy(() -> activityService.registerCD10Activity(accountId, deploymentActivity))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("harnessCdAppId is not present in runtimeValues");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testRegisterCD10Activity_resolveServiceAndEnvIdentifiers() throws IllegalAccessException {
    String appId = generateUuid();
    String envId = generateUuid();
    String serviceId = generateUuid();
    CD10ActivitySourceDTO cd10ActivitySourceDTO = createCD10ActivitySource(appId, envId, serviceId);
    activitySourceService.saveActivitySource(accountId, orgIdentifier, projectIdentifier, cd10ActivitySourceDTO);
    VerificationJob verificationJob = createVerificationJob();
    verificationJob.setActivitySourceIdentifier(cd10ActivitySourceDTO.getIdentifier());
    realVerificationJobService.save(verificationJob);
    FieldUtils.writeField(activityService, "verificationJobService", realVerificationJobService, true);
    when(verificationJobInstanceService.create(anyList())).thenReturn(Arrays.asList("taskId1"));
    DeploymentActivityDTO deploymentActivity = getDeploymentActivity(verificationJob);
    deploymentActivity.setEnvironmentIdentifier(null);
    deploymentActivity.setServiceIdentifier(null);
    Map<String, String> runtimeValues = new HashMap<>();
    runtimeValues.put("harnessCdAppId", appId);
    runtimeValues.put("harnessCdServiceId", serviceId);
    runtimeValues.put("harnessCdEnvId", envId);
    List<VerificationJobRuntimeDetails> verificationJobRuntimeDetails =
        Lists.newArrayList(VerificationJobRuntimeDetails.builder()
                               .verificationJobIdentifier(verificationJob.getIdentifier())
                               .runtimeValues(runtimeValues)
                               .build());
    deploymentActivity.setVerificationJobRuntimeDetails(verificationJobRuntimeDetails);
    CD10RegisterActivityDTO cd10RegisterActivityDTO =
        activityService.registerCD10Activity(accountId, deploymentActivity);
    DeploymentActivity updated = (DeploymentActivity) activityService.get(cd10RegisterActivityDTO.getActivityId());
    VerificationJobRuntimeDetails updatedDetails = updated.getVerificationJobRuntimeDetails().get(0);
    assertThat(updated.getEnvironmentIdentifier())
        .isEqualTo(cd10ActivitySourceDTO.getEnvMappings().iterator().next().getEnvIdentifier());
    assertThat(updated.getServiceIdentifier())
        .isEqualTo(cd10ActivitySourceDTO.getServiceMappings().iterator().next().getServiceIdentifier());
    assertThat(cd10RegisterActivityDTO.getEnvIdentifier())
        .isEqualTo(cd10ActivitySourceDTO.getEnvMappings().iterator().next().getEnvIdentifier());
    assertThat(cd10RegisterActivityDTO.getServiceIdentifier())
        .isEqualTo(cd10ActivitySourceDTO.getServiceMappings().iterator().next().getServiceIdentifier());
    assertThat(updatedDetails.getRuntimeValues().get(ENV_IDENTIFIER_KEY))
        .isEqualTo(cd10ActivitySourceDTO.getEnvMappings().iterator().next().getEnvIdentifier());
    assertThat(updatedDetails.getRuntimeValues().get(SERVICE_IDENTIFIER_KEY))
        .isEqualTo(cd10ActivitySourceDTO.getServiceMappings().iterator().next().getServiceIdentifier());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testRegisterCD10Activity_validateMappingsWithFailure() throws IllegalAccessException {
    String appId = generateUuid();
    String envId = generateUuid();
    String serviceId = generateUuid();
    CD10ActivitySourceDTO cd10ActivitySourceDTO = createCD10ActivitySource(appId, envId, serviceId);
    activitySourceService.saveActivitySource(accountId, orgIdentifier, projectIdentifier, cd10ActivitySourceDTO);
    VerificationJob verificationJob = createVerificationJob();
    verificationJob.setActivitySourceIdentifier(cd10ActivitySourceDTO.getIdentifier());
    verificationJob.setServiceIdentifier(generateUuid(), false);
    realVerificationJobService.save(verificationJob);
    FieldUtils.writeField(activityService, "verificationJobService", realVerificationJobService, true);
    when(verificationJobInstanceService.create(anyList())).thenReturn(Arrays.asList("taskId1"));
    DeploymentActivityDTO deploymentActivity = getDeploymentActivity(verificationJob);
    deploymentActivity.setEnvironmentIdentifier(null);
    deploymentActivity.setServiceIdentifier(null);
    Map<String, String> runtimeValues = new HashMap<>();
    runtimeValues.put("harnessCdAppId", appId);
    runtimeValues.put("harnessCdServiceId", serviceId);
    runtimeValues.put("harnessCdEnvId", envId);
    List<VerificationJobRuntimeDetails> verificationJobRuntimeDetails =
        Lists.newArrayList(VerificationJobRuntimeDetails.builder()
                               .verificationJobIdentifier(verificationJob.getIdentifier())
                               .runtimeValues(runtimeValues)
                               .build());
    deploymentActivity.setVerificationJobRuntimeDetails(verificationJobRuntimeDetails);
    assertThatThrownBy(() -> activityService.registerCD10Activity(accountId, deploymentActivity))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Next gen Service identifier does not match CD 1.0 service mapping");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testRegisterCD10Activity_noCD10MappingExists() {
    VerificationJob verificationJob = createVerificationJob();
    DeploymentActivityDTO deploymentActivity = getDeploymentActivity(verificationJob);
    deploymentActivity.setEnvironmentIdentifier(null);
    deploymentActivity.setServiceIdentifier(null);
    Map<String, String> runtimeValues = new HashMap<>();
    runtimeValues.put("harnessCdAppId", generateUuid());
    runtimeValues.put("harnessCdServiceId", generateUuid());
    runtimeValues.put("harnessCdEnvId", generateUuid());
    List<VerificationJobRuntimeDetails> verificationJobRuntimeDetails =
        Lists.newArrayList(VerificationJobRuntimeDetails.builder()
                               .verificationJobIdentifier(verificationJob.getIdentifier())
                               .runtimeValues(runtimeValues)
                               .build());
    deploymentActivity.setVerificationJobRuntimeDetails(verificationJobRuntimeDetails);
    assertThatThrownBy(() -> activityService.registerCD10Activity(accountId, deploymentActivity))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("No CD 1.0 mapping defined for projectIdentifier: %s", projectIdentifier);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testRegisterActivity_kubernetesActivity() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    when(verificationJobInstanceService.create(anyList())).thenReturn(Arrays.asList("taskId1"));

    activityService.register(accountId, generateUuid(), getDeploymentActivity(verificationJob));

    Activity activity = hPersistence.createQuery(Activity.class)
                            .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                            .get();

    assertThat(activity).isNotNull();
    assertThat(activity.getType().name()).isEqualTo(ActivityType.DEPLOYMENT.name());
    assertThat(activity.getActivityName()).isEqualTo("Build 23 deploy");
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testRegister_resolveJobUuidShouldBeNull() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    when(verificationJobInstanceService.create(anyList())).thenReturn(Arrays.asList("taskId1"));

    activityService.register(accountId, generateUuid(), getDeploymentActivity(verificationJob));

    ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(verificationJobInstanceService).create(argumentCaptor.capture());

    List<VerificationJobInstance> verificationJobInstanceList = argumentCaptor.getValue();

    assertThat(verificationJobInstanceList.get(0).getResolvedJob().getUuid()).isNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testRegisterActivity_deploymentActivityNoJobDetails() {
    ActivityDTO activityDTO = DeploymentActivityDTO.builder()
                                  .dataCollectionDelayMs(2000l)
                                  .newVersionHosts(new HashSet<>(Arrays.asList("node1", "node2")))
                                  .oldVersionHosts(new HashSet<>(Arrays.asList("node3", "node4")))
                                  .build();
    activityDTO.setAccountIdentifier(accountId);
    activityDTO.setProjectIdentifier(projectIdentifier);
    activityDTO.setOrgIdentifier(orgIdentifier);
    activityDTO.setActivityStartTime(Instant.now().toEpochMilli());
    activityDTO.setEnvironmentIdentifier(generateUuid());
    activityDTO.setName("Build 23 deploy");
    activityDTO.setServiceIdentifier(generateUuid());

    assertThatThrownBy(() -> activityService.register(accountId, generateUuid(), activityDTO))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testRegisterActivity_deploymentActivity() {
    when(verificationJobService.getVerificationJob(accountId, orgIdentifier, projectIdentifier, "canaryJobName1"))
        .thenReturn(createVerificationJob());
    when(verificationJobInstanceService.create(anyList())).thenReturn(Arrays.asList("taskId1"));
    List<VerificationJobRuntimeDetails> verificationJobDetails = new ArrayList<>();
    Map<String, String> runtimeParams = new HashMap<>();
    runtimeParams.put(JOB_IDENTIFIER_KEY, "canaryJobName1");
    runtimeParams.put(SERVICE_IDENTIFIER_KEY, "cvngService");
    runtimeParams.put(ENV_IDENTIFIER_KEY, "production");
    VerificationJobRuntimeDetails runtimeDetails = VerificationJobRuntimeDetails.builder()
                                                       .verificationJobIdentifier("canaryJobName1")
                                                       .runtimeValues(runtimeParams)
                                                       .build();
    verificationJobDetails.add(runtimeDetails);
    Instant now = Instant.now();
    ActivityDTO activityDTO = DeploymentActivityDTO.builder()
                                  .dataCollectionDelayMs(2000l)
                                  .newVersionHosts(new HashSet<>(Arrays.asList("node1", "node2")))
                                  .oldVersionHosts(new HashSet<>(Arrays.asList("node3", "node4")))
                                  .verificationStartTime(now.toEpochMilli())
                                  .deploymentTag(deploymentTag)
                                  .build();
    activityDTO.setAccountIdentifier(accountId);
    activityDTO.setProjectIdentifier(projectIdentifier);
    activityDTO.setOrgIdentifier(orgIdentifier);
    activityDTO.setActivityStartTime(now.toEpochMilli());
    activityDTO.setEnvironmentIdentifier(generateUuid());
    activityDTO.setName("Build 23 deploy");
    activityDTO.setVerificationJobRuntimeDetails(verificationJobDetails);
    activityDTO.setServiceIdentifier(generateUuid());
    activityDTO.setTags(Arrays.asList("build88", "prod deploy"));

    activityService.register(accountId, generateUuid(), activityDTO);

    Activity activity = hPersistence.createQuery(Activity.class)
                            .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                            .get();
    assertThat(activity).isNotNull();
    assertThat(activity.getType().name()).isEqualTo(ActivityType.DEPLOYMENT.name());
    assertThat(activity.getActivityName()).isEqualTo("Build 23 deploy");
    assertThat(activity.getVerificationJobInstanceIds()).isNotEmpty();
    assertThat(activity.getVerificationJobInstanceIds().get(0)).isEqualTo("taskId1");
    assertThat(activity.getTags().size()).isEqualTo(2);
    assertThat(activity.getTags()).containsExactlyInAnyOrder("build88", "prod deploy");

    verify(verificationJobInstanceService).create(anyList());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testRegisterActivity_deploymentActivityBadJobName() {
    when(verificationJobService.getVerificationJob(accountId, orgIdentifier, projectIdentifier, "canaryJobName2"))
        .thenReturn(createVerificationJob());
    when(verificationJobInstanceService.create(anyList())).thenReturn(Arrays.asList("taskId1"));
    List<VerificationJobRuntimeDetails> verificationJobDetails = new ArrayList<>();
    Map<String, String> runtimeParams = new HashMap<>();
    runtimeParams.put(JOB_IDENTIFIER_KEY, "canaryJobName1");
    runtimeParams.put(SERVICE_IDENTIFIER_KEY, "cvngService");
    runtimeParams.put(ENV_IDENTIFIER_KEY, "production");
    VerificationJobRuntimeDetails runtimeDetails = VerificationJobRuntimeDetails.builder()
                                                       .verificationJobIdentifier("canaryJobName1")
                                                       .runtimeValues(runtimeParams)
                                                       .build();
    verificationJobDetails.add(runtimeDetails);
    ActivityDTO activityDTO = DeploymentActivityDTO.builder()
                                  .dataCollectionDelayMs(2000l)
                                  .newVersionHosts(new HashSet<>(Arrays.asList("node1", "node2")))
                                  .oldVersionHosts(new HashSet<>(Arrays.asList("node3", "node4")))
                                  .build();
    activityDTO.setAccountIdentifier(accountId);
    activityDTO.setProjectIdentifier(projectIdentifier);
    activityDTO.setOrgIdentifier(orgIdentifier);
    activityDTO.setActivityStartTime(Instant.now().toEpochMilli());
    activityDTO.setEnvironmentIdentifier(generateUuid());
    activityDTO.setName("Build 23 deploy");
    activityDTO.setVerificationJobRuntimeDetails(verificationJobDetails);
    activityDTO.setServiceIdentifier(generateUuid());
    activityDTO.setTags(Arrays.asList("build88", "prod deploy"));

    assertThatThrownBy(() -> activityService.register(accountId, generateUuid(), activityDTO))
        .isInstanceOf(NullPointerException.class);

    verify(verificationJobInstanceService, times(0)).create(anyList());
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
    activityService.register(accountId, generateUuid(), activityDTO);

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

    activityService.register(accountId, generateUuid(), activityDTOManager);
    activityService.register(accountId, generateUuid(), activityDTOCVNG1);
    activityService.register(accountId, generateUuid(), activityDTOCVNG2);
    activityService.register(accountId, generateUuid(), activityDTOCVNG3);
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
    activityService.register(accountId, generateUuid(), activityDTO);
    activityService.register(accountId, generateUuid(), getDeploymentActivity(verificationJob));
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
    activityService.register(accountId, "webhook", deploymentActivityDTO);
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

    activityService.register(accountId, generateUuid(), getDeploymentActivity(verificationJob));

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

    activityService.register(accountId, generateUuid(), getDeploymentActivity(verificationJob));

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
    activityService.register(accountId, generateUuid(), getDeploymentActivity(verificationJob));

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
    activityService.register(accountId, generateUuid(), getDeploymentActivity(verificationJob));

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
    activityService.register(accountId, generateUuid(), getDeploymentActivity(verificationJob));

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
    activityService.register(accountId, generateUuid(), getDeploymentActivity(verificationJob));

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
    activityService.register(accountId, generateUuid(), getInfrastructureActivity(verificationJob));
    activityService.register(accountId, generateUuid(), getInfrastructureActivity(verificationJob));

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
    activityService.register(accountId, generateUuid(), getDeploymentActivity(verificationJob));
    activityService.register(accountId, generateUuid(), getDeploymentActivity(verificationJob));
    List<ActivityDashboardDTO> dashboardDTOList =
        activityService.listActivitiesInTimeRange(accountId, orgIdentifier, projectIdentifier, envIdentifier,
            serviceIdentifier, Instant.now().minus(15, ChronoUnit.MINUTES), Instant.now().plus(15, ChronoUnit.MINUTES));

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
    String activityId = activityService.register(accountId, generateUuid(), deploymentActivityDTO);
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
    String activityId = activityService.register(accountId, generateUuid(), deploymentActivityDTO);
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

    activityService.register(accountId, generateUuid(), getDeploymentActivity(verificationJob));
    activityService.register(accountId, generateUuid(), getDeploymentActivity(verificationJob));

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
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpdateActivityStatus_inProgress() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    instant = Instant.now();

    activityService.register(accountId, generateUuid(), getDeploymentActivity(verificationJob));
    activityService.register(accountId, generateUuid(), getDeploymentActivity(verificationJob));

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
    testVerificationJob.setDataSources(Lists.newArrayList(DataSourceType.APP_DYNAMICS));
    testVerificationJob.setSensitivity(Sensitivity.MEDIUM);
    testVerificationJob.setServiceIdentifier("<+input>", true);
    testVerificationJob.setEnvIdentifier("<+input>", true);
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
}
