package io.harness.cvng.activity.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.KANHAIYA;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.VUK;

import static junit.framework.TestCase.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cvng.activity.beans.KubernetesActivityDetailsDTO;
import io.harness.cvng.activity.beans.KubernetesActivityDetailsDTO.KubernetesActivityDetail;
import io.harness.cvng.activity.entities.Activity.ActivityKeys;
import io.harness.cvng.activity.entities.ActivitySource;
import io.harness.cvng.activity.entities.ActivitySource.ActivitySourceKeys;
import io.harness.cvng.activity.entities.ActivitySource.ActivitySourceUpdatableEntity;
import io.harness.cvng.activity.entities.CD10ActivitySource;
import io.harness.cvng.activity.entities.CDNGActivitySource;
import io.harness.cvng.activity.entities.KubernetesActivity;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.activity.source.services.api.ActivitySourceService;
import io.harness.cvng.activity.source.services.api.KubernetesActivitySourceService;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.activity.ActivitySourceDTO;
import io.harness.cvng.beans.activity.ActivitySourceType;
import io.harness.cvng.beans.activity.KubernetesActivityDTO;
import io.harness.cvng.beans.activity.KubernetesActivityDTO.KubernetesEventType;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO.KubernetesActivitySourceConfig;
import io.harness.cvng.beans.activity.cd10.CD10ActivitySourceDTO;
import io.harness.cvng.beans.activity.cd10.CD10EnvMappingDTO;
import io.harness.cvng.beans.activity.cd10.CD10ServiceMappingDTO;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import io.harness.reflection.ReflectionUtils;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mongodb.morphia.query.UpdateOperations;

@OwnedBy(HarnessTeam.CV)
public class ActivitySourceServiceImplTest extends CvNextGenTestBase {
  @Inject private HPersistence hPersistence;
  @Inject private KubernetesActivitySourceService kubernetesActivitySourceService;
  @Inject private ActivitySourceService activitySourceService;
  @Inject private CVConfigService cvConfigService;
  @Inject private VerificationJobService verificationJobService;
  @Inject Injector injector;
  @Mock private VerificationManagerService verificationManagerService;

  private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;
  private String serviceIdentifier;
  private String envIdentifier;
  private String perpetualTaskId;
  private String connectorIdentifier;

  @Before
  public void setup() throws IllegalAccessException {
    accountId = generateUuid();
    orgIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    perpetualTaskId = generateUuid();
    serviceIdentifier = generateUuid();
    envIdentifier = generateUuid();
    connectorIdentifier = generateUuid();
    when(verificationManagerService.createDataCollectionTask(
             eq(accountId), eq(orgIdentifier), eq(projectIdentifier), any(DataCollectionConnectorBundle.class)))
        .thenReturn(perpetualTaskId);
    FieldUtils.writeField(activitySourceService, "verificationManagerService", verificationManagerService, true);
    FieldUtils.writeField(
        kubernetesActivitySourceService, "verificationManagerService", verificationManagerService, true);
    verificationJobService.createDefaultVerificationJobs(accountId, orgIdentifier, projectIdentifier);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category({UnitTests.class})
  @Ignore("We do not use Kubernetes activity source anymore. Moved to change source")
  public void testCreate_GetKubernetesSource() {
    String identifier = generateUuid();
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO =
        KubernetesActivitySourceDTO.builder()
            .identifier(identifier)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .name("some-name")
            .connectorIdentifier(generateUuid())
            .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                       .serviceIdentifier(generateUuid())
                                                       .envIdentifier(generateUuid())
                                                       .namespace(generateUuid())
                                                       .workloadName(generateUuid())
                                                       .build()))
            .build();
    String kubernetesSourceId = activitySourceService.create(accountId, kubernetesActivitySourceDTO);

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
  @Owner(developers = KANHAIYA)
  @Category({UnitTests.class})
  public void test_ActivitySourceImplementsActivitySourceUpdatableEntity() {
    EnumSet.allOf(ActivitySourceType.class).forEach(activitySourceType -> {
      assertNotNull(
          injector.getInstance(Key.get(ActivitySourceUpdatableEntity.class, Names.named(activitySourceType.name()))));
    });
  }

  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testDeleteActivitySource_cd10ActivitySource() {
    Set<CD10EnvMappingDTO> cd10EnvMappingDTOS = new HashSet<>();
    Set<CD10ServiceMappingDTO> cd10ServiceMappingDTOS = new HashSet<>();
    Set<String> appIds = new HashSet<>();
    for (int i = 0; i < 10; i++) {
      String appId = generateUuid();
      appIds.add(appId);
      cd10EnvMappingDTOS.add(createEnvMapping(appId, generateUuid(), generateUuid()));
      cd10ServiceMappingDTOS.add(createServiceMapping(appId, generateUuid(), generateUuid()));
    }
    CD10ActivitySourceDTO cd10ActivitySourceDTO = CD10ActivitySourceDTO.builder()
                                                      .identifier(generateUuid())
                                                      .orgIdentifier(orgIdentifier)
                                                      .projectIdentifier(projectIdentifier)
                                                      .name("some-name")
                                                      .envMappings(cd10EnvMappingDTOS)
                                                      .serviceMappings(cd10ServiceMappingDTOS)
                                                      .build();
    String activitySourceUUID = activitySourceService.create(accountId, cd10ActivitySourceDTO);
    // delete and test
    assertThat(activitySourceService.deleteActivitySource(
                   accountId, orgIdentifier, projectIdentifier, cd10ActivitySourceDTO.getIdentifier()))
        .isTrue();
    List<ActivitySourceDTO> activitySourceDTOS =
        activitySourceService.listActivitySources(accountId, orgIdentifier, projectIdentifier, 0, 10, null)
            .getContent();
    assertThat(activitySourceDTOS.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testCreate_cd10ActivitySource() {
    String identifier = CD10ActivitySource.HARNESS_CD_10_ACTIVITY_SOURCE_IDENTIFIER;
    Set<CD10EnvMappingDTO> cd10EnvMappingDTOS = new HashSet<>();
    Set<CD10ServiceMappingDTO> cd10ServiceMappingDTOS = new HashSet<>();
    Set<String> appIds = new HashSet<>();
    for (int i = 0; i < 10; i++) {
      String appId = generateUuid();
      appIds.add(appId);
      cd10EnvMappingDTOS.add(createEnvMapping(appId, generateUuid(), generateUuid()));
      cd10ServiceMappingDTOS.add(createServiceMapping(appId, generateUuid(), generateUuid()));
    }
    CD10ActivitySourceDTO cd10ActivitySourceDTO = CD10ActivitySourceDTO.builder()
                                                      .identifier(identifier)
                                                      .orgIdentifier(orgIdentifier)
                                                      .projectIdentifier(projectIdentifier)
                                                      .name("some-name")
                                                      .envMappings(cd10EnvMappingDTOS)
                                                      .serviceMappings(cd10ServiceMappingDTOS)
                                                      .build();
    String activitySourceUUID = activitySourceService.create(accountId, cd10ActivitySourceDTO);

    CD10ActivitySource activitySource =
        (CD10ActivitySource) activitySourceService.getActivitySource(activitySourceUUID);
    assertThat(activitySource.getAccountId()).isEqualTo(accountId);
    assertThat(activitySource.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(activitySource.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(activitySource.getName()).isEqualTo("some-name");
    assertThat(activitySource.getIdentifier()).isEqualTo(identifier);
    assertThat(activitySource.getEnvMappings()).isEqualTo(cd10EnvMappingDTOS);
    assertThat(activitySource.getServiceMappings()).isEqualTo(cd10ServiceMappingDTOS);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testCreate_multipleCD10ActivitySource() {
    String identifier = generateUuid();
    Set<CD10EnvMappingDTO> cd10EnvMappingDTOS = new HashSet<>();
    Set<CD10ServiceMappingDTO> cd10ServiceMappingDTOS = new HashSet<>();
    Set<String> appIds = new HashSet<>();
    for (int i = 0; i < 2; i++) {
      String appId = generateUuid();
      appIds.add(appId);
      cd10EnvMappingDTOS.add(createEnvMapping(appId, generateUuid(), generateUuid()));
      cd10ServiceMappingDTOS.add(createServiceMapping(appId, generateUuid(), generateUuid()));
    }
    CD10ActivitySourceDTO cd10ActivitySourceDTO = CD10ActivitySourceDTO.builder()
                                                      .identifier(identifier)
                                                      .orgIdentifier(orgIdentifier)
                                                      .projectIdentifier(projectIdentifier)
                                                      .name("some-name")
                                                      .envMappings(cd10EnvMappingDTOS)
                                                      .serviceMappings(cd10ServiceMappingDTOS)
                                                      .build();
    activitySourceService.create(accountId, cd10ActivitySourceDTO);
    CD10ActivitySourceDTO secondCd10ActivitySource = CD10ActivitySourceDTO.builder()
                                                         .identifier("second identifier")
                                                         .orgIdentifier(orgIdentifier)
                                                         .projectIdentifier(projectIdentifier)
                                                         .name("some-name")
                                                         .envMappings(cd10EnvMappingDTOS)
                                                         .serviceMappings(cd10ServiceMappingDTOS)
                                                         .build();
    assertThatThrownBy(() -> activitySourceService.create(accountId, secondCd10ActivitySource))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("There can only be one CD 1.0 activity source per project");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testUpdate_cd10ActivitySource() {
    Set<CD10EnvMappingDTO> cd10EnvMappingDTOS = new HashSet<>();
    Set<CD10ServiceMappingDTO> cd10ServiceMappingDTOS = new HashSet<>();
    Set<String> appIds = new HashSet<>();
    for (int i = 0; i < 10; i++) {
      String appId = generateUuid();
      appIds.add(appId);
      cd10EnvMappingDTOS.add(createEnvMapping(appId, generateUuid(), generateUuid()));
      cd10ServiceMappingDTOS.add(createServiceMapping(appId, generateUuid(), generateUuid()));
    }
    CD10ActivitySourceDTO cd10ActivitySourceDTO =
        CD10ActivitySourceDTO.builder()
            .identifier(CD10ActivitySource.HARNESS_CD_10_ACTIVITY_SOURCE_IDENTIFIER)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .name("some-name")
            .envMappings(cd10EnvMappingDTOS)
            .serviceMappings(cd10ServiceMappingDTOS)
            .build();
    String activitySourceUUID = activitySourceService.create(accountId, cd10ActivitySourceDTO);
    cd10ActivitySourceDTO.setUuid(activitySourceUUID);
    cd10ActivitySourceDTO.setName("updated name");
    cd10ActivitySourceDTO.setEnvMappings(Collections.emptySet());
    cd10ActivitySourceDTO.setServiceMappings(Collections.singleton(cd10ServiceMappingDTOS.iterator().next()));
    activitySourceService.update(accountId, cd10ActivitySourceDTO.getIdentifier(), cd10ActivitySourceDTO);
    CD10ActivitySource activitySource =
        (CD10ActivitySource) activitySourceService.getActivitySource(activitySourceUUID);
    assertThat(activitySource.getAccountId()).isEqualTo(accountId);
    assertThat(activitySource.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(activitySource.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(activitySource.getName()).isEqualTo("updated name");
    assertThat(activitySource.getIdentifier()).isEqualTo(CD10ActivitySource.HARNESS_CD_10_ACTIVITY_SOURCE_IDENTIFIER);
    assertThat(activitySource.getEnvMappings()).isEmpty();
    assertThat(activitySource.getServiceMappings())
        .isEqualTo(Collections.singleton(cd10ServiceMappingDTOS.iterator().next()));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testGetActivitySource() {
    String identifier = CD10ActivitySource.HARNESS_CD_10_ACTIVITY_SOURCE_IDENTIFIER;
    Set<CD10EnvMappingDTO> cd10EnvMappingDTOS = new HashSet<>();
    Set<CD10ServiceMappingDTO> cd10ServiceMappingDTOS = new HashSet<>();
    Set<String> appIds = new HashSet<>();
    for (int i = 0; i < 10; i++) {
      String appId = generateUuid();
      appIds.add(appId);
      cd10EnvMappingDTOS.add(createEnvMapping(appId, generateUuid(), generateUuid()));
      cd10ServiceMappingDTOS.add(createServiceMapping(appId, generateUuid(), generateUuid()));
    }
    CD10ActivitySourceDTO cd10ActivitySourceDTO = CD10ActivitySourceDTO.builder()
                                                      .identifier(identifier)
                                                      .orgIdentifier(orgIdentifier)
                                                      .projectIdentifier(projectIdentifier)
                                                      .name("some-name")
                                                      .envMappings(cd10EnvMappingDTOS)
                                                      .serviceMappings(cd10ServiceMappingDTOS)
                                                      .build();
    String activitySourceUUID = activitySourceService.create(accountId, cd10ActivitySourceDTO);

    CD10ActivitySourceDTO activitySource = (CD10ActivitySourceDTO) activitySourceService.getActivitySource(
        accountId, orgIdentifier, projectIdentifier, identifier);
    assertThat(activitySource.getUuid()).isEqualTo(activitySourceUUID);
    assertThat(activitySource.getName()).isEqualTo("some-name");
    assertThat(activitySource.getIdentifier()).isEqualTo(identifier);
    assertThat(activitySource.getEnvMappings()).isEqualTo(cd10EnvMappingDTOS);
    assertThat(activitySource.getServiceMappings()).isEqualTo(cd10ServiceMappingDTOS);
  }
  @Test
  @Owner(developers = VUK)
  @Category({UnitTests.class})
  public void testDeleteByProjectIdentifier() {
    String identifier = generateUuid();
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO =
        KubernetesActivitySourceDTO.builder()
            .identifier(identifier)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .name("some-name")
            .connectorIdentifier(generateUuid())
            .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                       .serviceIdentifier(generateUuid())
                                                       .envIdentifier(generateUuid())
                                                       .namespace(generateUuid())
                                                       .workloadName(generateUuid())
                                                       .build()))
            .build();
    String kubernetesSourceId = activitySourceService.create(accountId, kubernetesActivitySourceDTO);

    activitySourceService.deleteByProjectIdentifier(ActivitySource.class, accountId, orgIdentifier, projectIdentifier);

    KubernetesActivitySource kubernetesActivitySource =
        hPersistence.get(KubernetesActivitySource.class, kubernetesSourceId);

    assertThat(kubernetesActivitySource).isNull();
  }

  @Test
  @Owner(developers = VUK)
  @Category({UnitTests.class})
  public void testDeleteByOrganisationIdentifier() {
    String identifier = generateUuid();
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO =
        KubernetesActivitySourceDTO.builder()
            .identifier(identifier)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .name("some-name")
            .connectorIdentifier(generateUuid())
            .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                       .serviceIdentifier(generateUuid())
                                                       .envIdentifier(generateUuid())
                                                       .namespace(generateUuid())
                                                       .workloadName(generateUuid())
                                                       .build()))
            .build();
    String kubernetesSourceId = activitySourceService.create(accountId, kubernetesActivitySourceDTO);

    activitySourceService.deleteByOrgIdentifier(ActivitySource.class, accountId, orgIdentifier);

    KubernetesActivitySource kubernetesActivitySource =
        hPersistence.get(KubernetesActivitySource.class, kubernetesSourceId);

    assertThat(kubernetesActivitySource).isNull();
  }

  @Test
  @Owner(developers = VUK)
  @Category({UnitTests.class})
  public void testDeleteByAccountIdentifier() {
    String identifier = generateUuid();
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO =
        KubernetesActivitySourceDTO.builder()
            .identifier(identifier)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .name("some-name")
            .connectorIdentifier(generateUuid())
            .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                       .serviceIdentifier(generateUuid())
                                                       .envIdentifier(generateUuid())
                                                       .namespace(generateUuid())
                                                       .workloadName(generateUuid())
                                                       .build()))
            .build();
    String kubernetesSourceId = activitySourceService.create(accountId, kubernetesActivitySourceDTO);

    activitySourceService.deleteByAccountIdentifier(ActivitySource.class, accountId);

    KubernetesActivitySource kubernetesActivitySource =
        hPersistence.get(KubernetesActivitySource.class, kubernetesSourceId);

    assertThat(kubernetesActivitySource).isNull();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category({UnitTests.class})
  public void testEnqueueDataCollectionTask() {
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO =
        KubernetesActivitySourceDTO.builder()
            .identifier(generateUuid())
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .name(generateUuid())
            .connectorIdentifier(generateUuid())
            .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                       .serviceIdentifier(generateUuid())
                                                       .envIdentifier(generateUuid())
                                                       .namespace(generateUuid())
                                                       .workloadName(generateUuid())
                                                       .build()))
            .build();
    String kubernetesSourceId = activitySourceService.create(accountId, kubernetesActivitySourceDTO);
    kubernetesActivitySourceService.enqueueDataCollectionTask(
        (KubernetesActivitySource) activitySourceService.getActivitySource(kubernetesSourceId));
    KubernetesActivitySource activitySource =
        (KubernetesActivitySource) activitySourceService.getActivitySource(kubernetesSourceId);
    assertThat(activitySource.getDataCollectionTaskId()).isEqualTo(perpetualTaskId);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category({UnitTests.class})
  @Ignore("We do not use Kubernetes activity source anymore. Moved to change source")
  public void testSaveKubernetesActivities() {
    createCVConfig();

    String nameSpace = generateUuid();
    String workLoadName = generateUuid();
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO =
        KubernetesActivitySourceDTO.builder()
            .identifier(generateUuid())
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .name(generateUuid())
            .connectorIdentifier(generateUuid())
            .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                       .serviceIdentifier(generateUuid())
                                                       .envIdentifier(generateUuid())
                                                       .namespace(nameSpace)
                                                       .workloadName(workLoadName)
                                                       .build()))
            .build();
    String kubernetesSourceId = activitySourceService.create(accountId, kubernetesActivitySourceDTO);
    int numOfEvents = 10;
    List<String> kindTypes = Lists.newArrayList("Deployment", "Replicaset", "Pod");
    ArrayList<KubernetesEventType> kubernetesEventTypes =
        Lists.newArrayList(KubernetesEventType.Normal, KubernetesEventType.Error);
    Instant activityStartTime = Instant.now();
    Instant activityEndTime = Instant.now().plus(1, ChronoUnit.HOURS);

    List<KubernetesActivityDTO> activityDTOS = new ArrayList<>();
    kindTypes.forEach(kind -> kubernetesEventTypes.forEach(kubernetesEventType -> {
      for (int i = 0; i < numOfEvents; i++) {
        activityDTOS.add(KubernetesActivityDTO.builder()
                             .message(generateUuid())
                             .activitySourceConfigId(kubernetesSourceId)
                             .eventJson(generateUuid())
                             .eventType(kubernetesEventType)
                             .namespace(nameSpace)
                             .workloadName(workLoadName)
                             .kind(kind)
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
    assertThat(kubernetesActivities.size()).isEqualTo(kindTypes.size());
    kindTypes.forEach(kind -> {
      List<KubernetesActivityDTO> kubernetesActivityDTOS = new ArrayList<>();
      kubernetesActivities.stream()
          .filter(kubernetesActivity -> kind.equals(kubernetesActivity.getKind()))
          .forEach(kubernetesActivity -> {
            assertThat(kubernetesActivity.getActivities().size()).isEqualTo(kubernetesEventTypes.size() * numOfEvents);
          });
    });
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  @Ignore("We do not use Kubernetes activity source anymore. Moved to change source")
  public void testUpsertAddsAllFields() {
    createCVConfig();

    KubernetesActivitySourceDTO kubernetesActivitySourceDTO =
        KubernetesActivitySourceDTO.builder()
            .identifier(generateUuid())
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .name(generateUuid())
            .connectorIdentifier(generateUuid())
            .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                       .serviceIdentifier(generateUuid())
                                                       .envIdentifier(generateUuid())
                                                       .namespace(generateUuid())
                                                       .workloadName(generateUuid())
                                                       .build()))
            .build();
    String kubernetesSourceId = activitySourceService.create(accountId, kubernetesActivitySourceDTO);
    kubernetesActivitySourceService.saveKubernetesActivities(accountId, kubernetesSourceId,
        Lists.newArrayList(KubernetesActivityDTO.builder()
                               .message(generateUuid())
                               .activitySourceConfigId(kubernetesSourceId)
                               .eventJson(generateUuid())
                               .eventType(KubernetesEventType.Normal)
                               .kind(generateUuid())
                               .namespace(generateUuid())
                               .workloadName(generateUuid())
                               .activityStartTime(Instant.now().toEpochMilli())
                               .activityEndTime(Instant.now().toEpochMilli())
                               .serviceIdentifier(serviceIdentifier)
                               .environmentIdentifier(envIdentifier)
                               .build()));
    List<KubernetesActivity> kubernetesActivities =
        hPersistence.createQuery(KubernetesActivity.class, excludeAuthority).asList();
    Set<String> nullableFields = Sets.newHashSet(ActivityKeys.activityName, ActivityKeys.verificationJobRuntimeDetails,
        ActivityKeys.activityEndTime, ActivityKeys.tags, ActivityKeys.verificationSummary,
        ActivityKeys.verificationIteration, ActivityKeys.verificationJobs, ActivityKeys.changeSourceIdentifier,
        ActivityKeys.eventTime);
    kubernetesActivities.forEach(activity -> {
      List<Field> fields = ReflectionUtils.getAllDeclaredAndInheritedFields(KubernetesActivity.class);
      fields.stream().filter(field -> !nullableFields.contains(field.getName())).forEach(field -> {
        try {
          field.setAccessible(true);
          assertThat(field.get(activity)).withFailMessage("field %s is null", field.getName()).isNotNull();
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      });
    });
  }

  @Test
  @Owner(developers = RAGHU)
  @Category({UnitTests.class})
  @Ignore("We do not use Kubernetes activity source anymore. Moved to change source")
  public void testDeletePerpetualTask_whenEditKubernetesActivities() {
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO =
        KubernetesActivitySourceDTO.builder()
            .identifier(generateUuid())
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .name(generateUuid())
            .connectorIdentifier(generateUuid())
            .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                       .serviceIdentifier(generateUuid())
                                                       .envIdentifier(generateUuid())
                                                       .namespace(generateUuid())
                                                       .workloadName(generateUuid())
                                                       .build()))
            .build();
    String kubernetesSourceId = activitySourceService.create(accountId, kubernetesActivitySourceDTO);
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
                                      .identifier(kubernetesActivitySourceDTO.getIdentifier())
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .name(generateUuid())
                                      .connectorIdentifier(generateUuid())
                                      .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                                                 .serviceIdentifier(generateUuid())
                                                                                 .envIdentifier(generateUuid())
                                                                                 .namespace(generateUuid())
                                                                                 .workloadName(generateUuid())
                                                                                 .build()))
                                      .build();
    activitySourceService.update(accountId, kubernetesActivitySourceDTO.getIdentifier(), kubernetesActivitySourceDTO);
    verify(verificationManagerService, times(1)).deletePerpetualTask(accountId, dataCollectionTaskId);
    kubernetesActivitySource = hPersistence.get(KubernetesActivitySource.class, kubernetesSourceId);
    dataCollectionTaskId = kubernetesActivitySource.getDataCollectionTaskId();
    assertThat(dataCollectionTaskId).isNull();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({UnitTests.class})
  @Ignore("We do not use Kubernetes activity source anymore. Moved to change source")
  public void testDoesAActivitySourceExistsForTheCaseWhenNoSourceAdded() {
    boolean doesAActivitySourceExistsForThisProject =
        kubernetesActivitySourceService.doesAActivitySourceExistsForThisProject(
            accountId, orgIdentifier, projectIdentifier);
    assertThat(doesAActivitySourceExistsForThisProject).isFalse();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({UnitTests.class})
  @Ignore("We do not use Kubernetes activity source anymore. Moved to change source")
  public void testDoesAActivitySourceExistsForTheCaseWhenSourceExists() {
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO =
        KubernetesActivitySourceDTO.builder()
            .identifier(generateUuid())
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .name(generateUuid())
            .connectorIdentifier(generateUuid())
            .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                       .serviceIdentifier(generateUuid())
                                                       .envIdentifier(generateUuid())
                                                       .namespace(generateUuid())
                                                       .workloadName(generateUuid())
                                                       .build()))
            .build();
    activitySourceService.create(accountId, kubernetesActivitySourceDTO);
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
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
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
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
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

    activitySourceService.create(accountId, kubernetesActivitySourceDTO1);
    activitySourceService.create(accountId, kubernetesActivitySourceDTO2);
    int numberOfServicesInActivity =
        kubernetesActivitySourceService.getNumberOfKubernetesServicesSetup(accountId, orgIdentifier, projectIdentifier);
    assertThat(numberOfServicesInActivity).isEqualTo(4);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testFindByConnectorIdentifier_projectScopedConnector() {
    List<KubernetesActivitySourceDTO> kubernetesActivitySourceDTOS = createKubernetesActivitySourceDTOs(5);

    ActivitySource activitySource = null;
    for (KubernetesActivitySourceDTO kubernetesActivitySourceDTO : kubernetesActivitySourceDTOS) {
      activitySource =
          KubernetesActivitySource.fromDTO(accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO);
    }
    String kubernetesSourceId = activitySourceService.create(accountId, activitySource.toDTO());
    KubernetesActivitySource kubernetesActivitySource =
        (KubernetesActivitySource) activitySourceService.getActivitySource(kubernetesSourceId);
    List<KubernetesActivitySource> kubernetesActivitySourceList = Arrays.asList(kubernetesActivitySource);

    List<KubernetesActivitySource> result = kubernetesActivitySourceService.findByConnectorIdentifier(
        accountId, orgIdentifier, projectIdentifier, connectorIdentifier, Scope.PROJECT);

    assertThat(result).isEqualTo(kubernetesActivitySourceList);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testFindByConnectorIdentifier_orgScopedConnector() {
    List<KubernetesActivitySourceDTO> projectScopedKubernetesActivitySourceDTOS = createKubernetesActivitySourceDTOs(5);
    ActivitySource projectScopedActivitySource = null;
    for (KubernetesActivitySourceDTO kubernetesActivitySourceDTO : projectScopedKubernetesActivitySourceDTOS) {
      projectScopedActivitySource =
          KubernetesActivitySource.fromDTO(accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO);
    }

    String projectScopedConnectorIdentifier = connectorIdentifier;
    connectorIdentifier = "org.connectorId";

    String projectScopedKubernetesSourceId =
        activitySourceService.create(accountId, projectScopedActivitySource.toDTO());
    KubernetesActivitySource projectScopedKubernetesActivitySource =
        (KubernetesActivitySource) activitySourceService.getActivitySource(projectScopedKubernetesSourceId);
    List<KubernetesActivitySource> kubernetesActivitySourceListKubernetesActivitySourceList =
        Arrays.asList(projectScopedKubernetesActivitySource);

    List<KubernetesActivitySourceDTO> kubernetesActivitySourceDTOS = createKubernetesActivitySourceDTOs(5);

    ActivitySource activitySource = null;
    for (KubernetesActivitySourceDTO kubernetesActivitySourceDTO : kubernetesActivitySourceDTOS) {
      activitySource =
          KubernetesActivitySource.fromDTO(accountId, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO);
    }
    String kubernetesSourceId = activitySourceService.create(accountId, activitySource.toDTO());
    KubernetesActivitySource kubernetesActivitySource =
        (KubernetesActivitySource) activitySourceService.getActivitySource(kubernetesSourceId);
    List<KubernetesActivitySource> kubernetesActivitySourceList = Arrays.asList(kubernetesActivitySource);

    List<KubernetesActivitySource> result = kubernetesActivitySourceService.findByConnectorIdentifier(
        accountId, orgIdentifier, "", "connectorId", Scope.ORG);

    assertThat(result).isEqualTo(kubernetesActivitySourceList);

    result = kubernetesActivitySourceService.findByConnectorIdentifier(
        accountId, orgIdentifier, projectIdentifier, projectScopedConnectorIdentifier, Scope.PROJECT);

    assertThat(result).isEqualTo(kubernetesActivitySourceListKubernetesActivitySourceList);

    result = kubernetesActivitySourceService.findByConnectorIdentifier(
        accountId, orgIdentifier, projectIdentifier, "random", Scope.ACCOUNT);
    assertThat(result).isEmpty();
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testUpdate_noActivitySourceExists() {
    CD10ActivitySourceDTO cd10ActivitySourceDTO =
        CD10ActivitySourceDTO.builder()
            .identifier(CD10ActivitySource.HARNESS_CD_10_ACTIVITY_SOURCE_IDENTIFIER)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .name("some-name")
            .build();
    String activitySourceUUID = "some-id";

    assertThatThrownBy(() -> activitySourceService.update(accountId, activitySourceUUID, cd10ActivitySourceDTO))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(String.format(
            "Activity Source with identifier [%s] , orgIdentifier [%s] and projectIdentifier [%s] not found",
            activitySourceUUID, orgIdentifier, projectIdentifier));
  }

  @Test
  @Owner(developers = RAGHU)
  @Category({UnitTests.class})
  @Ignore("We do not use Kubernetes activity source anymore. Moved to change source")
  public void testGetEventDetails() {
    createCVConfig();
    String nameSpace = generateUuid();
    String workLoadName = generateUuid();
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO =
        KubernetesActivitySourceDTO.builder()
            .identifier(generateUuid())
            .name(generateUuid())
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .connectorIdentifier(generateUuid())
            .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                       .serviceIdentifier(generateUuid())
                                                       .envIdentifier(generateUuid())
                                                       .namespace(nameSpace)
                                                       .workloadName(workLoadName)
                                                       .build()))
            .build();
    String kubernetesSourceId = activitySourceService.create(accountId, kubernetesActivitySourceDTO);
    int numOfEvents = 10;
    ArrayList<KubernetesEventType> kubernetesEventTypes =
        Lists.newArrayList(KubernetesEventType.Normal, KubernetesEventType.Error);
    Instant activityStartTime = Instant.now();
    Instant activityEndTime = Instant.now().plus(1, ChronoUnit.HOURS);

    List<KubernetesActivityDTO> activityDTOS = new ArrayList<>();
    kubernetesEventTypes.forEach(kubernetesEventType -> {
      for (int i = 0; i < numOfEvents; i++) {
        activityDTOS.add(KubernetesActivityDTO.builder()
                             .message("message" + i)
                             .eventJson("json" + i)
                             .reason("reason" + i)
                             .activitySourceConfigId(kubernetesSourceId)
                             .eventType(kubernetesEventType)
                             .namespace(nameSpace)
                             .workloadName(workLoadName)
                             .kind("Pod")
                             .activityStartTime(activityStartTime.toEpochMilli())
                             .activityEndTime(activityEndTime.toEpochMilli())
                             .serviceIdentifier(serviceIdentifier)
                             .environmentIdentifier(envIdentifier)
                             .build());
      }
    });
    kubernetesActivitySourceService.saveKubernetesActivities(accountId, kubernetesSourceId, activityDTOS);

    List<KubernetesActivity> kubernetesActivities =
        hPersistence.createQuery(KubernetesActivity.class, excludeAuthority).asList();
    assertThat(kubernetesActivities.size()).isEqualTo(1);
    KubernetesActivityDetailsDTO eventDetails = kubernetesActivitySourceService.getEventDetails(
        accountId, orgIdentifier, projectIdentifier, kubernetesActivities.get(0).getUuid());
    assertThat(eventDetails.getSourceName()).isEqualTo(kubernetesActivitySourceDTO.getName());
    assertThat(eventDetails.getConnectorIdentifier()).isEqualTo(kubernetesActivitySourceDTO.getConnectorIdentifier());
    assertThat(eventDetails.getNamespace()).isEqualTo(nameSpace);
    assertThat(eventDetails.getWorkload()).isEqualTo(workLoadName);
    assertThat(eventDetails.getKind()).isEqualTo("Pod");
    assertThat(eventDetails.getDetails().size()).isEqualTo(numOfEvents * kubernetesEventTypes.size());
    kubernetesEventTypes.forEach(kubernetesEventType -> {
      List<KubernetesActivityDetail> kubernetesActivityDetails =
          eventDetails.getDetails()
              .stream()
              .filter(kubernetesActivityDetail -> kubernetesActivityDetail.getEventType().equals(kubernetesEventType))
              .collect(Collectors.toList());
      Collections.sort(kubernetesActivityDetails, Comparator.comparing(KubernetesActivityDetail::getReason));
      for (int i = 0; i < numOfEvents; i++) {
        KubernetesActivityDetail kubernetesActivityDetail = kubernetesActivityDetails.get(i);
        assertThat(kubernetesActivityDetail.getTimeStamp()).isEqualTo(activityStartTime.toEpochMilli());
        assertThat(kubernetesActivityDetail.getReason()).isEqualTo("reason" + i);
        assertThat(kubernetesActivityDetail.getMessage()).isEqualTo("message" + i);
        assertThat(kubernetesActivityDetail.getEventJson()).isEqualTo("json" + i);
      }
    });
  }
  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testCreateDefaultCDNGActivitySource() {
    activitySourceService.createDefaultCDNGActivitySource(accountId, orgIdentifier, projectIdentifier);
    ActivitySourceDTO activitySource = activitySourceService.getActivitySource(
        accountId, orgIdentifier, projectIdentifier, CDNGActivitySource.CDNG_ACTIVITY_SOURCE_IDENTIFIER);
    assertThat(activitySource.getType()).isEqualTo(ActivitySourceType.CDNG);
    assertThat(activitySource.getIdentifier()).isEqualTo(CDNGActivitySource.CDNG_ACTIVITY_SOURCE_IDENTIFIER);
    assertThat(activitySource.isEditable()).isEqualTo(false);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testCreateDefaultCDNGActivitySource_idempotent() {
    activitySourceService.createDefaultCDNGActivitySource(accountId, orgIdentifier, projectIdentifier);
    activitySourceService.createDefaultCDNGActivitySource(accountId, orgIdentifier, projectIdentifier);
    ActivitySourceDTO activitySource = activitySourceService.getActivitySource(
        accountId, orgIdentifier, projectIdentifier, CDNGActivitySource.CDNG_ACTIVITY_SOURCE_IDENTIFIER);
    assertThat(activitySource.getType()).isEqualTo(ActivitySourceType.CDNG);
    assertThat(activitySource.getIdentifier()).isEqualTo(CDNGActivitySource.CDNG_ACTIVITY_SOURCE_IDENTIFIER);
  }
  public List<KubernetesActivitySourceDTO> createKubernetesActivitySourceDTOs(int n) {
    return IntStream.range(0, n).mapToObj(index -> createKubernetesActivitySourceDTO()).collect(Collectors.toList());
  }

  private KubernetesActivitySourceDTO createKubernetesActivitySourceDTO() {
    return KubernetesActivitySourceDTO.builder()
        .identifier(generateUuid())
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .name("some-name")
        .connectorIdentifier(connectorIdentifier)
        .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                   .serviceIdentifier(generateUuid())
                                                   .envIdentifier(generateUuid())
                                                   .namespace(generateUuid())
                                                   .workloadName(generateUuid())
                                                   .build()))
        .build();
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
