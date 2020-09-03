package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.core.beans.ActivityDTO;
import io.harness.cvng.core.beans.DeploymentActivityDTO;
import io.harness.cvng.core.beans.KubernetesActivityDTO;
import io.harness.cvng.core.entities.Activity;
import io.harness.cvng.core.entities.Activity.ActivityKeys;
import io.harness.cvng.core.entities.Activity.ActivityType;
import io.harness.cvng.core.services.api.ActivityService;
import io.harness.cvng.core.services.api.WebhookService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;

public class ActivityServiceImplTest extends CvNextGenTest {
  @Inject HPersistence hPersistence;
  @Inject ActivityService activityService;
  @Mock WebhookService mockWebhookService;

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
  public void testRegisterActivity_deploymentActivity() {
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

    activityService.register(accountId, generateUuid(), activityDTO);

    Activity activity = hPersistence.createQuery(Activity.class)
                            .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                            .get();
    assertThat(activity).isNotNull();
    assertThat(activity.getType().name()).isEqualTo(ActivityType.DEPLOYMENT.name());
    assertThat(activity.getActivityName()).isEqualTo("Build 23 deploy");
  }
}