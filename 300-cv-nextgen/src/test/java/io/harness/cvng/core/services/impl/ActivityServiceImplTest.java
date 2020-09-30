package io.harness.cvng.core.services.impl;

import static io.harness.cvng.verificationjob.CVVerificationJobConstants.ENV_IDENTIFIER_KEY;
import static io.harness.cvng.verificationjob.CVVerificationJobConstants.JOB_IDENTIFIER_KEY;
import static io.harness.cvng.verificationjob.CVVerificationJobConstants.SERVICE_IDENTIFIER_KEY;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.ActivityDTO;
import io.harness.cvng.core.beans.DeploymentActivityDTO;
import io.harness.cvng.core.beans.DeploymentActivityVerificationResultDTO;
import io.harness.cvng.core.beans.KubernetesActivityDTO;
import io.harness.cvng.core.entities.Activity;
import io.harness.cvng.core.entities.Activity.ActivityKeys;
import io.harness.cvng.core.entities.Activity.ActivityType;
import io.harness.cvng.core.entities.Activity.VerificationJobRuntimeDetails;
import io.harness.cvng.core.entities.KubernetesActivity.KubernetesActivityKeys;
import io.harness.cvng.core.services.api.ActivityService;
import io.harness.cvng.core.services.api.WebhookService;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.cvng.verificationjob.entities.CanaryVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ActivityServiceImplTest extends CvNextGenTest {
  @Inject private HPersistence hPersistence;
  @Inject private ActivityService activityService;
  @Mock private WebhookService mockWebhookService;
  @Mock private VerificationJobService verificationJobService;
  @Mock private VerificationJobInstanceService verificationJobInstanceService;
  @Mock private NextGenService nextGenService;

  private String projectIdentifier;
  private String orgIdentifier;
  private String accountId;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    projectIdentifier = generateUuid();
    orgIdentifier = generateUuid();
    accountId = generateUuid();

    FieldUtils.writeField(activityService, "webhookService", mockWebhookService, true);
    FieldUtils.writeField(activityService, "verificationJobService", verificationJobService, true);
    FieldUtils.writeField(activityService, "verificationJobInstanceService", verificationJobInstanceService, true);
    FieldUtils.writeField(activityService, "nextGenService", nextGenService, true);
    when(nextGenService.getService(any(), any(), any(), any()))
        .thenReturn(ServiceResponseDTO.builder().name("service name").build());
    when(mockWebhookService.validateWebhookToken(any(), any(), any())).thenReturn(true);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testRegisterActivity_kubernetesActivity() {
    ActivityDTO activityDTO =
        KubernetesActivityDTO.builder().clusterName("harness-test").activityDescription("pod restarts").build();
    activityDTO.setAccountIdentifier(accountId);
    activityDTO.setProjectIdentifier(projectIdentifier);
    activityDTO.setOrgIdentifier(orgIdentifier);
    activityDTO.setActivityStartTime(Instant.now().toEpochMilli());
    activityDTO.setEnvironmentIdentifier(generateUuid());
    activityDTO.setName("Pod restart activity");
    activityDTO.setServiceIdentifier(generateUuid());

    activityService.register(accountId, generateUuid(), activityDTO);

    Activity activity = hPersistence.createQuery(Activity.class)
                            .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                            .get();
    assertThat(activity).isNotNull();
    assertThat(activity.getType().name()).isEqualTo(ActivityType.INFRASTRUCTURE.name());
    assertThat(activity.getActivityName()).isEqualTo("Pod restart activity");
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testRegisterActivity_kubernetesActivityNoClusterName() {
    ActivityDTO activityDTO = KubernetesActivityDTO.builder().activityDescription("pod restarts").build();
    activityDTO.setAccountIdentifier(accountId);
    activityDTO.setProjectIdentifier(projectIdentifier);
    activityDTO.setOrgIdentifier(orgIdentifier);
    activityDTO.setActivityStartTime(Instant.now().toEpochMilli());
    activityDTO.setEnvironmentIdentifier(generateUuid());
    activityDTO.setName("Pod restart activity");
    activityDTO.setServiceIdentifier(generateUuid());

    assertThatThrownBy(() -> activityService.register(accountId, generateUuid(), activityDTO))
        .isInstanceOf(NullPointerException.class)
        .hasMessage(KubernetesActivityKeys.clusterName + " should not be null");
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
    when(verificationJobService.getVerificationJob(accountId, "canaryJobName1")).thenReturn(createVerificationJob());
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
    when(verificationJobService.getVerificationJob(accountId, "canaryJobName2")).thenReturn(createVerificationJob());
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

  private VerificationJob createVerificationJob() {
    CanaryVerificationJob testVerificationJob = new CanaryVerificationJob();
    testVerificationJob.setAccountId(accountId);
    testVerificationJob.setIdentifier("identifier");
    testVerificationJob.setJobName(generateUuid());
    testVerificationJob.setDataSources(Lists.newArrayList(DataSourceType.APP_DYNAMICS));
    testVerificationJob.setSensitivity(Sensitivity.MEDIUM);
    testVerificationJob.setServiceIdentifier(generateUuid(), false);
    testVerificationJob.setEnvIdentifier(generateUuid(), false);
    testVerificationJob.setDuration(Duration.ofMinutes(5));
    testVerificationJob.setProjectIdentifier(generateUuid());
    testVerificationJob.setOrgIdentifier(generateUuid());
    return testVerificationJob;
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetRecentDeploymentActivityVerifications_noData() {
    List<DeploymentActivityVerificationResultDTO> deploymentActivityVerificationResultDTOs =
        activityService.getRecentDeploymentActivityVerifications(accountId, projectIdentifier);
    assertThat(deploymentActivityVerificationResultDTOs).isEmpty();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetRecentDeploymentActivityVerifications_withVerificationJobInstanceInQueuedState() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(accountId, verificationJob.getIdentifier()))
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
    ActivityDTO activityDTO = DeploymentActivityDTO.builder()
                                  .dataCollectionDelayMs(2000l)
                                  .newVersionHosts(new HashSet<>(Arrays.asList("node1", "node2")))
                                  .oldVersionHosts(new HashSet<>(Arrays.asList("node3", "node4")))
                                  .verificationStartTime(now.toEpochMilli())
                                  .deploymentTag("build#1")
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
    List<DeploymentActivityVerificationResultDTO> deploymentActivityVerificationResultDTOs =
        activityService.getRecentDeploymentActivityVerifications(accountId, projectIdentifier);
    assertThat(deploymentActivityVerificationResultDTOs)
        .isEqualTo(Collections.singletonList(deploymentActivityVerificationResultDTO));
  }
}