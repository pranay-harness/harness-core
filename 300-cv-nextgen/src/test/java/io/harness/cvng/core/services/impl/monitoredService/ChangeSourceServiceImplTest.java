package io.harness.cvng.core.services.impl.monitoredService;

import static io.harness.rule.OwnerRule.ABHIJITH;
import static io.harness.rule.OwnerRule.ANJAN;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.DataCollectionType;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.beans.ChangeSummaryDTO;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.monitoredService.changeSourceSpec.KubernetesChangeSourceSpec;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.changeSource.ChangeSource;
import io.harness.cvng.core.entities.changeSource.ChangeSource.ChangeSourceKeys;
import io.harness.cvng.core.entities.changeSource.KubernetesChangeSource;
import io.harness.cvng.core.services.api.ChangeEventService;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;
import io.harness.cvng.core.transformer.changeSource.ChangeSourceEntityAndDTOTransformer;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.DuplicateFieldException;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class ChangeSourceServiceImplTest extends CvNextGenTestBase {
  @Inject ChangeSourceService changeSourceService;
  @Inject HPersistence hPersistence;
  @Inject ChangeSourceEntityAndDTOTransformer changeSourceEntityAndDTOTransformer;
  @Mock VerificationManagerService verificationManagerService;
  @Mock ChangeEventService changeEventService;
  BuilderFactory builderFactory;

  ServiceEnvironmentParams environmentParams;

  @Before
  public void setup() throws IllegalAccessException {
    FieldUtils.writeField(changeSourceService, "changeEventService", changeEventService, true);
    FieldUtils.writeField(changeSourceService, "verificationManagerService", verificationManagerService, true);

    builderFactory = BuilderFactory.getDefault();
    environmentParams = ServiceEnvironmentParams.builder()
                            .accountIdentifier(builderFactory.getContext().getAccountId())
                            .orgIdentifier(builderFactory.getContext().getOrgIdentifier())
                            .projectIdentifier(builderFactory.getContext().getProjectIdentifier())
                            .serviceIdentifier(builderFactory.getContext().getServiceIdentifier())
                            .environmentIdentifier(builderFactory.getContext().getProjectIdentifier())
                            .build();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testCreate() {
    ChangeSourceDTO changeSourceDTO = builderFactory.getHarnessCDChangeSourceDTOBuilder().build();
    Set<ChangeSourceDTO> changeSourceDTOToBeCreated = new HashSet<>(Arrays.asList(changeSourceDTO));

    changeSourceService.create(environmentParams, changeSourceDTOToBeCreated);

    Set<ChangeSourceDTO> changeSourceDTOSetFromDb =
        changeSourceService.get(environmentParams, Arrays.asList(changeSourceDTO.getIdentifier()));

    assertThat(changeSourceDTOSetFromDb.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testCreate_withNonUniqueIdentifier() {
    ChangeSourceDTO changeSourceDto1 = builderFactory.getHarnessCDChangeSourceDTOBuilder().build();
    ChangeSourceDTO changeSourceDto2 =
        builderFactory.getHarnessCDChangeSourceDTOBuilder().identifier(changeSourceDto1.getIdentifier()).build();
    Set<ChangeSourceDTO> changeSourceDTOToBeCreated = new HashSet<>(Arrays.asList(changeSourceDto1, changeSourceDto2));
    assertThatThrownBy(() -> changeSourceService.create(environmentParams, changeSourceDTOToBeCreated))
        .isInstanceOf(DuplicateFieldException.class);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testUpdate() {
    ChangeSourceDTO changeSourceDto = builderFactory.getHarnessCDChangeSourceDTOBuilder().build();
    Set<ChangeSourceDTO> updateDtos = new HashSet<>(Arrays.asList(changeSourceDto));
    String updatedName = "UPDATED_NAME";

    changeSourceService.create(environmentParams, updateDtos);
    ChangeSource initialChangeSource = getChangeSourceFromDb(changeSourceDto.getIdentifier());

    changeSourceDto.setName(updatedName);
    changeSourceService.update(environmentParams, updateDtos);
    ChangeSource updatedChangeSource = getChangeSourceFromDb(changeSourceDto.getIdentifier());

    assertThat(updatedChangeSource.getUuid()).isEqualTo(initialChangeSource.getUuid());
    assertThat(updatedChangeSource.getCreatedAt()).isEqualTo(initialChangeSource.getCreatedAt());
    assertThat(updatedChangeSource.getName()).isEqualTo(updatedName);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testDelete() {
    ChangeSourceDTO changeSourceDto = builderFactory.getHarnessCDChangeSourceDTOBuilder().build();
    Set<ChangeSourceDTO> dtos = new HashSet<>(Arrays.asList(changeSourceDto));
    changeSourceService.create(environmentParams, dtos);

    changeSourceService.delete(environmentParams, Arrays.asList(changeSourceDto.getIdentifier()));
    ChangeSource changeSourceFromDb = getChangeSourceFromDb(changeSourceDto.getIdentifier());

    assertThat(changeSourceFromDb).isNull();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetChangeEvents() {
    List<ChangeEventDTO> changeEventDTOS = Arrays.asList(builderFactory.getHarnessCDChangeEventDTOBuilder().build());
    when(changeEventService.get(eq(builderFactory.getContext().getServiceEnvironmentParams()), eq(new ArrayList<>()),
             eq(Instant.ofEpochSecond(100)), eq(Instant.ofEpochSecond(100)), eq(new ArrayList<>())))
        .thenReturn(changeEventDTOS);
    List<ChangeEventDTO> result =
        changeSourceService.getChangeEvents(builderFactory.getContext().getServiceEnvironmentParams(),
            new ArrayList<>(), Instant.ofEpochSecond(100), Instant.ofEpochSecond(100), new ArrayList<>());
    assertThat(result).isEqualTo(changeEventDTOS);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testgetChangeSummary() {
    ChangeSummaryDTO changeSummaryDTO = ChangeSummaryDTO.builder().build();
    when(changeEventService.getChangeSummary(eq(builderFactory.getContext().getServiceEnvironmentParams()),
             eq(new ArrayList<>()), eq(Instant.ofEpochSecond(100)), eq(Instant.ofEpochSecond(100))))
        .thenReturn(changeSummaryDTO);
    ChangeSummaryDTO result =
        changeSourceService.getChangeSummary(builderFactory.getContext().getServiceEnvironmentParams(),
            new ArrayList<>(), Instant.ofEpochSecond(100), Instant.ofEpochSecond(100));
    assertThat(result).isEqualTo(changeSummaryDTO);
  }

  @Test
  @Owner(developers = ANJAN)
  @Category(UnitTests.class)
  public void testEnqueueDataCollectionTask() {
    String kubeConnectorIdentifier = randomAlphanumeric(20);
    String datacollectionTaskId = UUIDGenerator.generateUuid();
    String identifier = randomAlphanumeric(20);
    String name = randomAlphanumeric(20);

    KubernetesChangeSource kubeChangeSource = KubernetesChangeSource.builder()
                                                  .connectorIdentifier(kubeConnectorIdentifier)
                                                  .dataCollectionRequired(true)
                                                  .identifier(identifier)
                                                  .name(name)
                                                  .type(ChangeSourceType.KUBERNETES)
                                                  .orgIdentifier(builderFactory.getContext().getOrgIdentifier())
                                                  .projectIdentifier(builderFactory.getContext().getProjectIdentifier())
                                                  .serviceIdentifier(builderFactory.getContext().getServiceIdentifier())
                                                  .accountId(builderFactory.getContext().getAccountId())
                                                  .envIdentifier(builderFactory.getContext().getEnvIdentifier())
                                                  .serviceIdentifier(builderFactory.getContext().getServiceIdentifier())
                                                  .build();

    HashSet<ChangeSourceDTO> changeSourceDTOS = new HashSet<>();
    changeSourceDTOS.add(ChangeSourceDTO.builder()
                             .enabled(true)
                             .identifier(identifier)
                             .name(name)
                             .type(ChangeSourceType.KUBERNETES)
                             .spec(KubernetesChangeSourceSpec.builder().connectorRef(kubeConnectorIdentifier).build())
                             .build());

    changeSourceService.create(environmentParams, changeSourceDTOS);
    ChangeSource changeSource = getChangeSourceFromDb(identifier);
    kubeChangeSource.setUuid(changeSource.getUuid());

    when(verificationManagerService.createDataCollectionTask(eq(builderFactory.getContext().getAccountId()),
             eq(builderFactory.getContext().getOrgIdentifier()), eq(builderFactory.getContext().getProjectIdentifier()),
             eq(DataCollectionConnectorBundle.builder()
                     .dataCollectionType(DataCollectionType.KUBERNETES)
                     .connectorIdentifier(kubeChangeSource.getConnectorIdentifier())
                     .sourceIdentifier(kubeChangeSource.getIdentifier())
                     .dataCollectionWorkerId(kubeChangeSource.getUuid())
                     .build())))
        .thenReturn(datacollectionTaskId);
    changeSourceService.enqueueDataCollectionTask(kubeChangeSource);

    changeSource = getChangeSourceFromDb(identifier);
    assertThat(changeSource.getDataCollectionTaskId()).isEqualTo(datacollectionTaskId);
  }

  private ChangeSource getChangeSourceFromDb(String identifier) {
    return hPersistence.createQuery(ChangeSource.class).filter(ChangeSourceKeys.identifier, identifier).get();
  }
}