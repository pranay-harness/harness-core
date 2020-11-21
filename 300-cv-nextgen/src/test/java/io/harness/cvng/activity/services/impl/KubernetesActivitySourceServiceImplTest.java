package io.harness.cvng.activity.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.RAGHU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.activity.beans.KubernetesActivitySourceDTO;
import io.harness.cvng.activity.entities.KubernetesActivity;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.activity.services.api.KubernetesActivitySourceService;
import io.harness.cvng.beans.ActivityType;
import io.harness.cvng.beans.DataCollectionType;
import io.harness.cvng.beans.KubernetesActivityDTO;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class KubernetesActivitySourceServiceImplTest extends CvNextGenTest {
  @Inject private HPersistence hPersistence;
  @Inject private KubernetesActivitySourceService kubernetesActivitySourceService;
  @Mock private VerificationManagerService verificationManagerService;
  private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;
  private String perpetualTaskId;

  @Before
  public void setup() throws IllegalAccessException {
    accountId = generateUuid();
    orgIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    perpetualTaskId = generateUuid();
    when(verificationManagerService.createDataCollectionTask(
             eq(accountId), eq(orgIdentifier), eq(projectIdentifier), eq(DataCollectionType.KUBERNETES), anyMap()))
        .thenReturn(perpetualTaskId);
    FieldUtils.writeField(
        kubernetesActivitySourceService, "verificationManagerService", verificationManagerService, true);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category({UnitTests.class})
  public void testCreateAndGetKubernetesSource() {
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO = KubernetesActivitySourceDTO.builder()
                                                                  .connectorIdentifier(generateUuid())
                                                                  .serviceIdentifier(generateUuid())
                                                                  .envIdentifier(generateUuid())
                                                                  .namespace(generateUuid())
                                                                  .clusterName(generateUuid())
                                                                  .workloadName(generateUuid())
                                                                  .build();
    String kubernetesSourceId = kubernetesActivitySourceService.saveKubernetesSource(
        accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO);

    KubernetesActivitySource activitySource = kubernetesActivitySourceService.getActivitySource(kubernetesSourceId);
    assertThat(activitySource.getAccountId()).isEqualTo(accountId);
    assertThat(activitySource.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(activitySource.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(activitySource.getConnectorIdentifier()).isEqualTo(kubernetesActivitySourceDTO.getConnectorIdentifier());
    assertThat(activitySource.getServiceIdentifier()).isEqualTo(kubernetesActivitySourceDTO.getServiceIdentifier());
    assertThat(activitySource.getEnvIdentifier()).isEqualTo(kubernetesActivitySourceDTO.getEnvIdentifier());
    assertThat(activitySource.getNamespace()).isEqualTo(kubernetesActivitySourceDTO.getNamespace());
    assertThat(activitySource.getClusterName()).isEqualTo(kubernetesActivitySourceDTO.getClusterName());
    assertThat(activitySource.getWorkloadName()).isEqualTo(kubernetesActivitySourceDTO.getWorkloadName());
  }

  @Test
  @Owner(developers = RAGHU)
  @Category({UnitTests.class})
  public void testEnqueueDataCollectionTask() {
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO = KubernetesActivitySourceDTO.builder()
                                                                  .connectorIdentifier(generateUuid())
                                                                  .serviceIdentifier(generateUuid())
                                                                  .envIdentifier(generateUuid())
                                                                  .namespace(generateUuid())
                                                                  .clusterName(generateUuid())
                                                                  .workloadName(generateUuid())
                                                                  .build();
    String kubernetesSourceId = kubernetesActivitySourceService.saveKubernetesSource(
        accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO);
    kubernetesActivitySourceService.enqueueDataCollectionTask(
        kubernetesActivitySourceService.getActivitySource(kubernetesSourceId));
    KubernetesActivitySource activitySource = kubernetesActivitySourceService.getActivitySource(kubernetesSourceId);
    assertThat(activitySource.getDataCollectionTaskId()).isEqualTo(perpetualTaskId);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category({UnitTests.class})
  public void testSaveKubernetesActivities() {
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO = KubernetesActivitySourceDTO.builder()
                                                                  .connectorIdentifier(generateUuid())
                                                                  .serviceIdentifier(generateUuid())
                                                                  .envIdentifier(generateUuid())
                                                                  .namespace(generateUuid())
                                                                  .clusterName(generateUuid())
                                                                  .workloadName(generateUuid())
                                                                  .build();
    String kubernetesSourceId = kubernetesActivitySourceService.saveKubernetesSource(
        accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO);
    Instant activityStartTime = Instant.now();
    Instant activityEndTime = Instant.now().plus(1, ChronoUnit.HOURS);
    kubernetesActivitySourceService.saveKubernetesActivities(accountId, kubernetesSourceId,
        Lists.newArrayList(KubernetesActivityDTO.builder()
                               .clusterName(kubernetesActivitySourceDTO.getClusterName())
                               .activityDescription("description")
                               .activityStartTime(activityStartTime.toEpochMilli())
                               .activityEndTime(activityEndTime.toEpochMilli())
                               .build()));

    List<KubernetesActivity> kubernetesActivities =
        hPersistence.createQuery(KubernetesActivity.class, excludeAuthority).asList();
    assertThat(kubernetesActivities.size()).isEqualTo(1);
    KubernetesActivity kubernetesActivity = kubernetesActivities.get(0);
    assertThat(kubernetesActivity.getClusterName()).isEqualTo(kubernetesActivitySourceDTO.getClusterName());
    assertThat(kubernetesActivity.getActivityDescription()).isEqualTo("description");
    assertThat(kubernetesActivity.getType()).isEqualTo(ActivityType.INFRASTRUCTURE);
    assertThat(kubernetesActivity.getAccountIdentifier()).isEqualTo(accountId);
    assertThat(kubernetesActivity.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(kubernetesActivity.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(kubernetesActivity.getServiceIdentifier()).isEqualTo(kubernetesActivitySourceDTO.getServiceIdentifier());
    assertThat(kubernetesActivity.getEnvironmentIdentifier()).isEqualTo(kubernetesActivitySourceDTO.getEnvIdentifier());
    assertThat(kubernetesActivity.getActivityStartTime()).isEqualTo(activityStartTime);
    assertThat(kubernetesActivity.getActivityEndTime()).isEqualTo(activityEndTime);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({UnitTests.class})
  public void testDoesAActivitySourceExistsForTheCaseWhenNoSourceAdded() {
    boolean doesAActivitySourceExistsForThisProject =
        kubernetesActivitySourceService.doesAActivitySourceExistsForThisProject(
            accountId, orgIdentifier, projectIdentifier);
    assertThat(doesAActivitySourceExistsForThisProject).isFalse();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({UnitTests.class})
  public void testDoesAActivitySourceExistsForTheCaseWhenSourceExists() {
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO = KubernetesActivitySourceDTO.builder()
                                                                  .connectorIdentifier(generateUuid())
                                                                  .serviceIdentifier(generateUuid())
                                                                  .envIdentifier(generateUuid())
                                                                  .namespace(generateUuid())
                                                                  .clusterName(generateUuid())
                                                                  .workloadName(generateUuid())
                                                                  .build();
    kubernetesActivitySourceService.saveKubernetesSource(
        accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO);
    boolean doesAActivitySourceExistsForThisProject =
        kubernetesActivitySourceService.doesAActivitySourceExistsForThisProject(
            accountId, orgIdentifier, projectIdentifier);
    assertThat(doesAActivitySourceExistsForThisProject).isTrue();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({UnitTests.class})
  public void testGetNumberOfServicesSetup() {
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO1 = KubernetesActivitySourceDTO.builder()
                                                                   .connectorIdentifier(generateUuid())
                                                                   .serviceIdentifier(generateUuid())
                                                                   .envIdentifier(generateUuid())
                                                                   .namespace(generateUuid())
                                                                   .clusterName(generateUuid())
                                                                   .workloadName(generateUuid())
                                                                   .build();
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO2 = KubernetesActivitySourceDTO.builder()
                                                                   .connectorIdentifier(generateUuid())
                                                                   .serviceIdentifier(generateUuid())
                                                                   .envIdentifier(generateUuid())
                                                                   .namespace(generateUuid())
                                                                   .clusterName(generateUuid())
                                                                   .workloadName(generateUuid())
                                                                   .build();
    kubernetesActivitySourceService.saveKubernetesSource(
        accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO1);
    kubernetesActivitySourceService.saveKubernetesSource(
        accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO2);
    int numberOfServicesInActivity =
        kubernetesActivitySourceService.getNumberOfServicesSetup(accountId, orgIdentifier, projectIdentifier);
    assertThat(numberOfServicesInActivity).isEqualTo(2);
  }
}
