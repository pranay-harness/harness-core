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
import io.harness.cvng.activity.entities.ActivitySource.ActivitySourceKeys;
import io.harness.cvng.activity.entities.KubernetesActivity;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.activity.source.services.api.ActivitySourceService;
import io.harness.cvng.activity.source.services.api.KubernetesActivitySourceService;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.activity.ActivitySourceDTO;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.activity.KubernetesActivityDTO;
import io.harness.cvng.beans.activity.KubernetesActivityDTO.KubernetesEventType;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO.KubernetesActivitySourceConfig;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.models.VerificationType;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mongodb.morphia.query.UpdateOperations;

public class ActivitySourceServiceImplTest extends CvNextGenTest {
  @Inject private HPersistence hPersistence;
  @Inject private KubernetesActivitySourceService kubernetesActivitySourceService;
  @Inject private ActivitySourceService activitySourceService;
  @Inject private CVConfigService cvConfigService;
  @Mock private VerificationManagerService verificationManagerService;
  private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;
  private String serviceIdentifier;
  private String envIdentifier;
  private String perpetualTaskId;

  @Before
  public void setup() throws IllegalAccessException {
    accountId = generateUuid();
    orgIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    perpetualTaskId = generateUuid();
    serviceIdentifier = generateUuid();
    envIdentifier = generateUuid();
    when(verificationManagerService.createDataCollectionTask(
             eq(accountId), eq(orgIdentifier), eq(projectIdentifier), any(DataCollectionConnectorBundle.class)))
        .thenReturn(perpetualTaskId);
    FieldUtils.writeField(activitySourceService, "verificationManagerService", verificationManagerService, true);
    FieldUtils.writeField(
        kubernetesActivitySourceService, "verificationManagerService", verificationManagerService, true);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category({UnitTests.class})
  public void testCreateAndGetKubernetesSource() {
    String identifier = generateUuid();
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO =
        KubernetesActivitySourceDTO.builder()
            .identifier(identifier)
            .name("some-name")
            .connectorIdentifier(generateUuid())
            .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                       .serviceIdentifier(generateUuid())
                                                       .envIdentifier(generateUuid())
                                                       .namespace(generateUuid())
                                                       .workloadName(generateUuid())
                                                       .build()))
            .build();
    String kubernetesSourceId = activitySourceService.saveActivitySource(
        accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO);

    KubernetesActivitySource activitySource =
        (KubernetesActivitySource) activitySourceService.getActivitySource(kubernetesSourceId);
    assertThat(activitySource.getAccountId()).isEqualTo(accountId);
    assertThat(activitySource.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(activitySource.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(activitySource.getConnectorIdentifier()).isEqualTo(kubernetesActivitySourceDTO.getConnectorIdentifier());
    assertThat(activitySource.getIdentifier()).isEqualTo(kubernetesActivitySourceDTO.getIdentifier());
    assertThat(activitySource.getName()).isEqualTo(kubernetesActivitySourceDTO.getName());
    assertThat(activitySource.getActivitySourceConfigs())
        .isEqualTo(kubernetesActivitySourceDTO.getActivitySourceConfigs());

    List<ActivitySourceDTO> activitySourceDTOS =
        activitySourceService.listActivitySources(accountId, orgIdentifier, projectIdentifier, 0, 10, null)
            .getContent();
    assertThat(activitySourceDTOS.size()).isEqualTo(1);
    KubernetesActivitySourceDTO activitySourceDTO = (KubernetesActivitySourceDTO) activitySourceDTOS.get(0);
    assertThat(activitySourceDTO.getConnectorIdentifier())
        .isEqualTo(kubernetesActivitySourceDTO.getConnectorIdentifier());
    assertThat(activitySourceDTO.getIdentifier()).isEqualTo(kubernetesActivitySourceDTO.getIdentifier());
    assertThat(activitySourceDTO.getName()).isEqualTo(kubernetesActivitySourceDTO.getName());
    assertThat(activitySourceDTO.getActivitySourceConfigs())
        .isEqualTo(kubernetesActivitySourceDTO.getActivitySourceConfigs());

    // list call with filter
    activitySourceDTOS =
        activitySourceService.listActivitySources(accountId, orgIdentifier, projectIdentifier, 0, 10, "Me-")
            .getContent();
    assertThat(activitySourceDTOS.size()).isEqualTo(1);

    activitySourceDTOS =
        activitySourceService.listActivitySources(accountId, orgIdentifier, projectIdentifier, 0, 10, "sddhvsh")
            .getContent();
    assertThat(activitySourceDTOS.size()).isEqualTo(0);

    // get call
    activitySourceDTO = (KubernetesActivitySourceDTO) activitySourceService.getActivitySource(
        accountId, orgIdentifier, projectIdentifier, identifier);
    assertThat(activitySourceDTO.getConnectorIdentifier())
        .isEqualTo(kubernetesActivitySourceDTO.getConnectorIdentifier());
    assertThat(activitySourceDTO.getIdentifier()).isEqualTo(kubernetesActivitySourceDTO.getIdentifier());
    assertThat(activitySourceDTO.getName()).isEqualTo(kubernetesActivitySourceDTO.getName());
    assertThat(activitySourceDTO.getActivitySourceConfigs())
        .isEqualTo(kubernetesActivitySourceDTO.getActivitySourceConfigs());

    // delete and test
    assertThat(activitySourceService.deleteActivitySource(
                   accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO.getIdentifier()))
        .isTrue();
    activitySourceDTOS =
        activitySourceService.listActivitySources(accountId, orgIdentifier, projectIdentifier, 0, 10, null)
            .getContent();
    assertThat(activitySourceDTOS.size()).isEqualTo(0);
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
    String kubernetesSourceId = activitySourceService.saveActivitySource(
        accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO);
    kubernetesActivitySourceService.enqueueDataCollectionTask(
        (KubernetesActivitySource) activitySourceService.getActivitySource(kubernetesSourceId));
    KubernetesActivitySource activitySource =
        (KubernetesActivitySource) activitySourceService.getActivitySource(kubernetesSourceId);
    assertThat(activitySource.getDataCollectionTaskId()).isEqualTo(perpetualTaskId);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category({UnitTests.class})
  public void testSaveKubernetesActivities() {
    createCVConfig();

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
    String kubernetesSourceId = activitySourceService.saveActivitySource(
        accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO);
    int numOfEvents = 10;
    ArrayList<ActivityType> activityTypes =
        Lists.newArrayList(ActivityType.DEPLOYMENT, ActivityType.INFRASTRUCTURE, ActivityType.OTHER);
    ArrayList<KubernetesEventType> kubernetesEventTypes =
        Lists.newArrayList(KubernetesEventType.Normal, KubernetesEventType.Error);
    Instant activityStartTime = Instant.now();
    Instant activityEndTime = Instant.now().plus(1, ChronoUnit.HOURS);

    List<KubernetesActivityDTO> activityDTOS = new ArrayList<>();
    activityTypes.forEach(activityType -> kubernetesEventTypes.forEach(kubernetesEventType -> {
      for (int i = 0; i < numOfEvents; i++) {
        activityDTOS.add(KubernetesActivityDTO.builder()
                             .message(generateUuid())
                             .activitySourceConfigId(kubernetesSourceId)
                             .eventDetails(generateUuid())
                             .eventType(kubernetesEventType)
                             .kubernetesActivityType(activityType)
                             .activityStartTime(activityStartTime.toEpochMilli())
                             .activityEndTime(activityEndTime.toEpochMilli())
                             .serviceIdentifier(serviceIdentifier)
                             .environmentIdentifier(envIdentifier)
                             .build());
      }
    }));
    kubernetesActivitySourceService.saveKubernetesActivities(accountId, kubernetesSourceId, activityDTOS);

    List<KubernetesActivity> kubernetesActivities =
        hPersistence.createQuery(KubernetesActivity.class, excludeAuthority).asList();
    assertThat(kubernetesActivities.size()).isEqualTo(activityTypes.size() * kubernetesEventTypes.size());
    activityTypes.forEach(activityType -> {
      List<KubernetesActivity> activities =
          kubernetesActivities.stream()
              .filter(kubernetesActivity -> activityType.equals(kubernetesActivity.getKubernetesActivityType()))
              .collect(Collectors.toList());
      assertThat(activities.size()).isEqualTo(kubernetesEventTypes.size());
      kubernetesEventTypes.forEach(kubernetesEventType -> {
        List<KubernetesActivity> eventTypeList =
            activities.stream()
                .filter(kubernetesActivity -> kubernetesEventType.equals(kubernetesActivity.getEventType()))
                .collect(Collectors.toList());
        eventTypeList.forEach(
            kubernetesActivity -> assertThat(kubernetesActivity.getActivities().size()).isEqualTo(numOfEvents));
      });
    });
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
    String kubernetesSourceId = activitySourceService.saveActivitySource(
        accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO);
    UpdateOperations<KubernetesActivitySource> updateOperations =
        hPersistence.createUpdateOperations(KubernetesActivitySource.class);
    updateOperations.set(ActivitySourceKeys.dataCollectionTaskId, generateUuid());
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
    activitySourceService.saveActivitySource(accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO);
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
    activitySourceService.saveActivitySource(accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO);
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

    activitySourceService.saveActivitySource(accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO1);
    activitySourceService.saveActivitySource(accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO2);
    int numberOfServicesInActivity =
        kubernetesActivitySourceService.getNumberOfKubernetesServicesSetup(accountId, orgIdentifier, projectIdentifier);
    assertThat(numberOfServicesInActivity).isEqualTo(4);
  }

  private CVConfig createCVConfig() {
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setVerificationType(VerificationType.TIME_SERIES);
    cvConfig.setAccountId(accountId);
    cvConfig.setConnectorIdentifier("AppDynamics Connector");
    cvConfig.setServiceIdentifier(serviceIdentifier);
    cvConfig.setEnvIdentifier(envIdentifier);
    cvConfig.setOrgIdentifier(orgIdentifier);
    cvConfig.setProjectIdentifier(projectIdentifier);
    cvConfig.setIdentifier(generateUuid());
    cvConfig.setMonitoringSourceName(generateUuid());
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName(generateUuid());
    cvConfig.setApplicationName("appName");
    cvConfig.setTierName("tierName");
    cvConfig.setMetricPack(MetricPack.builder().build());
    return cvConfigService.save(cvConfig);
  }
}
