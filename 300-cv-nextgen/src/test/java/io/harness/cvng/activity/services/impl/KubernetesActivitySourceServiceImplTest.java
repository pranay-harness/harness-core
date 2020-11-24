package io.harness.cvng.activity.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.RAGHU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.activity.entities.KubernetesActivity;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.activity.entities.KubernetesActivitySource.KubernetesActivitySourceKeys;
import io.harness.cvng.activity.services.api.KubernetesActivitySourceService;
import io.harness.cvng.beans.ActivityType;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.KubernetesActivityDTO;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO.KubernetesActivitySourceConfig;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mongodb.morphia.query.UpdateOperations;

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
             eq(accountId), eq(orgIdentifier), eq(projectIdentifier), any(DataCollectionConnectorBundle.class)))
        .thenReturn(perpetualTaskId);
    FieldUtils.writeField(
        kubernetesActivitySourceService, "verificationManagerService", verificationManagerService, true);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category({UnitTests.class})
  public void testCreateAndGetKubernetesSource() {
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO =
        KubernetesActivitySourceDTO.builder()
            .identifier(generateUuid())
            .name(generateUuid())
            .connectorIdentifier(generateUuid())
            .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                       .serviceIdentifier(generateUuid())
                                                       .envIdentifier(generateUuid())
                                                       .namespace(generateUuid())
                                                       .workloadName(generateUuid())
                                                       .build()))
            .build();
    String kubernetesSourceId = kubernetesActivitySourceService.saveKubernetesSource(
        accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO);

    KubernetesActivitySource activitySource = kubernetesActivitySourceService.getActivitySource(kubernetesSourceId);
    assertThat(activitySource.getAccountId()).isEqualTo(accountId);
    assertThat(activitySource.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(activitySource.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(activitySource.getConnectorIdentifier()).isEqualTo(kubernetesActivitySourceDTO.getConnectorIdentifier());
    assertThat(activitySource.getIdentifier()).isEqualTo(kubernetesActivitySourceDTO.getIdentifier());
    assertThat(activitySource.getName()).isEqualTo(kubernetesActivitySourceDTO.getName());
    assertThat(activitySource.getActivitySourceConfigs())
        .isEqualTo(kubernetesActivitySourceDTO.getActivitySourceConfigs());

    List<KubernetesActivitySourceDTO> kubernetesActivitySourceDTOS =
        kubernetesActivitySourceService.listKubernetesSources(accountId, orgIdentifier, projectIdentifier);
    assertThat(kubernetesActivitySourceDTOS.size()).isEqualTo(1);
    KubernetesActivitySourceDTO activitySourceDTO = kubernetesActivitySourceDTOS.get(0);
    assertThat(activitySourceDTO.getConnectorIdentifier())
        .isEqualTo(kubernetesActivitySourceDTO.getConnectorIdentifier());
    assertThat(activitySourceDTO.getIdentifier()).isEqualTo(kubernetesActivitySourceDTO.getIdentifier());
    assertThat(activitySourceDTO.getName()).isEqualTo(kubernetesActivitySourceDTO.getName());
    assertThat(activitySourceDTO.getActivitySourceConfigs())
        .isEqualTo(kubernetesActivitySourceDTO.getActivitySourceConfigs());

    // delete and test
    assertThat(kubernetesActivitySourceService.deleteKubernetesSource(
                   accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO.getIdentifier()))
        .isTrue();
    kubernetesActivitySourceDTOS =
        kubernetesActivitySourceService.listKubernetesSources(accountId, orgIdentifier, projectIdentifier);
    assertThat(kubernetesActivitySourceDTOS.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category({UnitTests.class})
  public void testEnqueueDataCollectionTask() {
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO =
        KubernetesActivitySourceDTO.builder()
            .identifier(generateUuid())
            .name(generateUuid())
            .connectorIdentifier(generateUuid())
            .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                       .serviceIdentifier(generateUuid())
                                                       .envIdentifier(generateUuid())
                                                       .namespace(generateUuid())
                                                       .workloadName(generateUuid())
                                                       .build()))
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
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO =
        KubernetesActivitySourceDTO.builder()
            .identifier(generateUuid())
            .name(generateUuid())
            .connectorIdentifier(generateUuid())
            .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                       .serviceIdentifier(generateUuid())
                                                       .envIdentifier(generateUuid())
                                                       .namespace(generateUuid())
                                                       .workloadName(generateUuid())
                                                       .build()))
            .build();
    String kubernetesSourceId = kubernetesActivitySourceService.saveKubernetesSource(
        accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO);
    Instant activityStartTime = Instant.now();
    Instant activityEndTime = Instant.now().plus(1, ChronoUnit.HOURS);
    kubernetesActivitySourceService.saveKubernetesActivities(accountId, kubernetesSourceId,
        Lists.newArrayList(KubernetesActivityDTO.builder()
                               .message("description")
                               .activityStartTime(activityStartTime.toEpochMilli())
                               .activityEndTime(activityEndTime.toEpochMilli())
                               .build()));

    List<KubernetesActivity> kubernetesActivities =
        hPersistence.createQuery(KubernetesActivity.class, excludeAuthority).asList();
    assertThat(kubernetesActivities.size()).isEqualTo(1);
    KubernetesActivity kubernetesActivity = kubernetesActivities.get(0);
    assertThat(kubernetesActivity.getMessage()).isEqualTo("description");
    assertThat(kubernetesActivity.getType()).isEqualTo(ActivityType.INFRASTRUCTURE);
    assertThat(kubernetesActivity.getAccountIdentifier()).isEqualTo(accountId);
    assertThat(kubernetesActivity.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(kubernetesActivity.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(kubernetesActivity.getActivityStartTime()).isEqualTo(activityStartTime);
    assertThat(kubernetesActivity.getActivityEndTime()).isEqualTo(activityEndTime);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category({UnitTests.class})
  public void testDeletePerpetualTask_whenEditKubernetesActivities() {
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO =
        KubernetesActivitySourceDTO.builder()
            .identifier(generateUuid())
            .name(generateUuid())
            .connectorIdentifier(generateUuid())
            .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                       .serviceIdentifier(generateUuid())
                                                       .envIdentifier(generateUuid())
                                                       .namespace(generateUuid())
                                                       .workloadName(generateUuid())
                                                       .build()))
            .build();
    String kubernetesSourceId = kubernetesActivitySourceService.saveKubernetesSource(
        accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO);
    UpdateOperations<KubernetesActivitySource> updateOperations =
        hPersistence.createUpdateOperations(KubernetesActivitySource.class);
    updateOperations.set(KubernetesActivitySourceKeys.dataCollectionTaskId, generateUuid());
    hPersistence.update(hPersistence.get(KubernetesActivitySource.class, kubernetesSourceId), updateOperations);

    KubernetesActivitySource kubernetesActivitySource =
        hPersistence.get(KubernetesActivitySource.class, kubernetesSourceId);
    String dataCollectionTaskId = kubernetesActivitySource.getDataCollectionTaskId();
    assertThat(dataCollectionTaskId).isNotEmpty();

    kubernetesActivitySourceDTO = KubernetesActivitySourceDTO.builder()
                                      .uuid(kubernetesSourceId)
                                      .identifier(generateUuid())
                                      .name(generateUuid())
                                      .connectorIdentifier(generateUuid())
                                      .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                                                 .serviceIdentifier(generateUuid())
                                                                                 .envIdentifier(generateUuid())
                                                                                 .namespace(generateUuid())
                                                                                 .workloadName(generateUuid())
                                                                                 .build()))
                                      .build();
    kubernetesActivitySourceService.saveKubernetesSource(
        accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO);
    verify(verificationManagerService, times(1)).deletePerpetualTask(accountId, dataCollectionTaskId);
    kubernetesActivitySource = hPersistence.get(KubernetesActivitySource.class, kubernetesSourceId);
    dataCollectionTaskId = kubernetesActivitySource.getDataCollectionTaskId();
    assertThat(dataCollectionTaskId).isNull();
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
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO =
        KubernetesActivitySourceDTO.builder()
            .identifier(generateUuid())
            .name(generateUuid())
            .connectorIdentifier(generateUuid())
            .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                       .serviceIdentifier(generateUuid())
                                                       .envIdentifier(generateUuid())
                                                       .namespace(generateUuid())
                                                       .workloadName(generateUuid())
                                                       .build()))
            .build();
    kubernetesActivitySourceService.saveKubernetesSource(
        accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO);
    boolean doesActivitySourceExistsForThisProject =
        kubernetesActivitySourceService.doesAActivitySourceExistsForThisProject(
            accountId, orgIdentifier, projectIdentifier);
    assertThat(doesActivitySourceExistsForThisProject).isTrue();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({UnitTests.class})
  public void testGetNumberOfServicesSetup() {
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO1 =
        KubernetesActivitySourceDTO.builder()
            .identifier(generateUuid())
            .name(generateUuid())
            .connectorIdentifier(generateUuid())
            .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                       .serviceIdentifier(generateUuid())
                                                       .envIdentifier(generateUuid())
                                                       .namespace(generateUuid())
                                                       .workloadName(generateUuid())
                                                       .build(),
                KubernetesActivitySourceConfig.builder()
                    .serviceIdentifier(generateUuid())
                    .envIdentifier(generateUuid())
                    .namespace(generateUuid())
                    .workloadName(generateUuid())
                    .build()))
            .build();
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO2 =
        KubernetesActivitySourceDTO.builder()
            .identifier(generateUuid())
            .name(generateUuid())
            .connectorIdentifier(generateUuid())
            .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                       .serviceIdentifier(generateUuid())
                                                       .envIdentifier(generateUuid())
                                                       .namespace(generateUuid())
                                                       .workloadName(generateUuid())
                                                       .build(),
                KubernetesActivitySourceConfig.builder()
                    .serviceIdentifier(generateUuid())
                    .envIdentifier(generateUuid())
                    .namespace(generateUuid())
                    .workloadName(generateUuid())
                    .build()))
            .build();

    kubernetesActivitySourceService.saveKubernetesSource(
        accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO1);
    kubernetesActivitySourceService.saveKubernetesSource(
        accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO2);
    int numberOfServicesInActivity =
        kubernetesActivitySourceService.getNumberOfServicesSetup(accountId, orgIdentifier, projectIdentifier);
    assertThat(numberOfServicesInActivity).isEqualTo(4);
  }
}
