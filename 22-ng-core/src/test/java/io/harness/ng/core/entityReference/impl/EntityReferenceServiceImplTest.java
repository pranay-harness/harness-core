package io.harness.ng.core.entityReference.impl;

import static io.harness.ng.core.entityReference.ReferenceEntityType.CONNECTOR;
import static io.harness.ng.core.entityReference.ReferenceEntityType.PIPELINE;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ng.core.NGCoreBaseTest;
import io.harness.ng.core.entityReference.ReferenceEntityType;
import io.harness.ng.core.entityReference.dto.EntityReferenceDTO;
import io.harness.ng.core.entityReference.service.EntityReferenceService;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.springframework.data.domain.Page;

public class EntityReferenceServiceImplTest extends NGCoreBaseTest {
  @Inject @InjectMocks EntityReferenceService entityReferenceService;

  private EntityReferenceDTO createEntityReference(String accountIdentifier, String referredEntityFQN,
      ReferenceEntityType referredEntityType, String referredEntityName, String referredByEntityFQN,
      ReferenceEntityType referredByEntityType, String referredByEntityName) {
    return EntityReferenceDTO.builder()
        .accountIdentifier(accountIdentifier)
        .referredEntityFQN(referredEntityFQN)
        .referredEntityType(referredEntityType)
        .referredEntityName(referredEntityName)
        .referredByEntityFQN(referredByEntityFQN)
        .referredByEntityType(referredByEntityType)
        .referredByEntityName(referredByEntityName)
        .build();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void listTest() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";

    String referredIdentifier1 = "referredIdentifier1";
    String referredIdentifier2 = "referredIdentifier2";

    String referredEntityName1 = "Connector 1";
    String referredEntityName2 = "Connector 2";

    String referredEntityFQN1 = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, referredIdentifier1);
    String referredEntityFQN2 = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, referredIdentifier2);

    String referredByIdentifier1 = "referredByIdentifier1";
    String referredByIdentifier2 = "referredByIdentifier2";
    String referredBydentifier3 = "referredIBydentifier3";

    String referredByEntityFQN1 = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, referredByIdentifier1);
    String referredByEntityFQN2 = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, referredByIdentifier2);
    String referredByEntityFQN3 = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, referredBydentifier3);

    String referredByEntityName1 = "Pipeline 1";
    String referredByEntityName2 = "Pipeline 2";
    String referredByEntityName3 = "Pipeline 3";

    EntityReferenceDTO entityReferenceDTO1 = createEntityReference(accountIdentifier, referredEntityFQN1, CONNECTOR,
        referredEntityName1, referredByEntityFQN1, PIPELINE, referredByEntityName1);
    EntityReferenceDTO entityReferenceDTO2 = createEntityReference(accountIdentifier, referredEntityFQN1, CONNECTOR,
        referredEntityName1, referredByEntityFQN2, PIPELINE, referredByEntityName2);
    EntityReferenceDTO entityReferenceDTO3 = createEntityReference(accountIdentifier, referredEntityFQN2, CONNECTOR,
        referredEntityName2, referredByEntityFQN1, PIPELINE, referredByEntityName1);
    entityReferenceService.save(entityReferenceDTO1);
    entityReferenceService.save(entityReferenceDTO2);
    entityReferenceService.save(entityReferenceDTO3);
    Page<EntityReferenceDTO> entityReferenceDTOPage = entityReferenceService.list(
        0, 10, accountIdentifier, orgIdentifier, projectIdentifier, referredIdentifier1, null);
    assertThat(entityReferenceDTOPage.getTotalElements()).isEqualTo(2);
    verifyTheValuesAreCorrect(entityReferenceDTOPage.getContent().get(0), entityReferenceDTO2);
    verifyTheValuesAreCorrect(entityReferenceDTOPage.getContent().get(1), entityReferenceDTO1);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void saveTest() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String referredIdentifier1 = "referredIdentifier1";
    String referredByIdentifier1 = "referredByIdentifier1";
    String referredEntityName1 = "Connector 1";
    String referredByEntityName1 = "Pipeline 1";

    String referredEntityFQN1 = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, referredIdentifier1);
    String referredByEntityFQN1 = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, referredByIdentifier1);

    EntityReferenceDTO entityReferenceDTO1 = createEntityReference(accountIdentifier, referredEntityFQN1, CONNECTOR,
        referredEntityName1, referredByEntityFQN1, PIPELINE, referredByEntityName1);
    EntityReferenceDTO savedEntityReferenceDTO = entityReferenceService.save(entityReferenceDTO1);
    verifyTheValuesAreCorrect(savedEntityReferenceDTO, entityReferenceDTO1);
  }

  private void verifyTheValuesAreCorrect(
      EntityReferenceDTO actualEntityReferenceDTO, EntityReferenceDTO expectedEntityReferenceDTO) {
    assertThat(actualEntityReferenceDTO).isNotNull();
    assertThat(actualEntityReferenceDTO.getReferredEntityFQN())
        .isEqualTo(expectedEntityReferenceDTO.getReferredEntityFQN());
    assertThat(actualEntityReferenceDTO.getReferredEntityType())
        .isEqualTo(expectedEntityReferenceDTO.getReferredEntityType());
    assertThat(actualEntityReferenceDTO.getReferredEntityName())
        .isEqualTo(expectedEntityReferenceDTO.getReferredEntityName());

    assertThat(actualEntityReferenceDTO.getReferredByEntityFQN())
        .isEqualTo(expectedEntityReferenceDTO.getReferredByEntityFQN());
    assertThat(actualEntityReferenceDTO.getReferredByEntityType())
        .isEqualTo(expectedEntityReferenceDTO.getReferredByEntityType());
    assertThat(actualEntityReferenceDTO.getReferredEntityName())
        .isEqualTo(expectedEntityReferenceDTO.getReferredEntityName());

    assertThat(actualEntityReferenceDTO.getCreatedAt()).isNotNull();
  }
}