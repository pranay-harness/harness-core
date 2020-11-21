package io.harness.ng.core.entitysetupusage.impl;

import static io.harness.EntityType.CONNECTORS;
import static io.harness.EntityType.PIPELINES;
import static io.harness.EntityType.SECRETS;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.EntityType;
import io.harness.category.element.UnitTests;
import io.harness.common.EntityReference;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.NGCoreTestBase;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.springframework.data.domain.Page;

public class EntitySetupUsageServiceImplTest extends NGCoreTestBase {
  @Inject @InjectMocks EntitySetupUsageService entitySetupUsageService;
  String accountIdentifier = "accountIdentifier";
  String orgIdentifier = "orgIdentifier";
  String projectIdentifier = "projectIdentifier";
  String referredIdentifier = "referredIdentifier1";
  String referredByIdentifier = "referredByIdentifier1";
  String referredEntityName = "Connector 1";
  String referredByEntityName = "Pipeline 1";

  private EntitySetupUsageDTO createEntityReference(
      String accountIdentifier, EntityDetail referredEntity, EntityDetail referredByEntity) {
    return EntitySetupUsageDTO.builder()
        .accountIdentifier(accountIdentifier)
        .referredEntity(referredEntity)
        .referredByEntity(referredByEntity)
        .build();
  }

  private EntityDetail getEntityDetails(String identifier, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String name, EntityType type) {
    EntityReference referredByEntityRef =
        IdentifierRefHelper.getIdentifierRef(identifier, accountIdentifier, orgIdentifier, projectIdentifier);
    return EntityDetail.builder().entityRef(referredByEntityRef).name(name).type(type).build();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void isEntityReferenced() {
    String referredEntityFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, referredIdentifier);
    saveEntitySetupUsage();
    boolean entityReferenceExists = entitySetupUsageService.isEntityReferenced(accountIdentifier, referredEntityFQN);
    assertThat(entityReferenceExists).isTrue();

    boolean doesEntityReferenceExists =
        entitySetupUsageService.isEntityReferenced(accountIdentifier, "identifierWhichIsNotReferenced");
    assertThat(doesEntityReferenceExists).isFalse();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void listTest() {
    List<EntitySetupUsageDTO> setupUsages = new ArrayList<>();
    String referredIdentifier = "referredIdentifier";
    String referredEntityName = "Connector";
    for (int i = 0; i < 2; i++) {
      String referredByIdentifier = "referredByIdentifier" + i;
      String referredByEntityName = "Pipeline" + i;
      EntityDetail referredByEntity = getEntityDetails(
          referredByIdentifier, accountIdentifier, orgIdentifier, projectIdentifier, referredByEntityName, PIPELINES);
      EntityDetail referredEntity = getEntityDetails(
          referredIdentifier, accountIdentifier, orgIdentifier, projectIdentifier, referredEntityName, CONNECTORS);
      EntitySetupUsageDTO entitySetupUsageDTO =
          createEntityReference(accountIdentifier, referredEntity, referredByEntity);
      setupUsages.add(entitySetupUsageDTO);
      entitySetupUsageService.save(entitySetupUsageDTO);
    }

    // Adding one extra setup usage for different entity
    String referredByIdentifier = "referredByIdentifier";
    String referredIdentifier1 = "referredIdentifier1";
    EntityDetail referredByEntity = getEntityDetails(
        referredByIdentifier, accountIdentifier, orgIdentifier, projectIdentifier, referredByEntityName, PIPELINES);
    EntityDetail referredEntity = getEntityDetails(
        referredIdentifier1, accountIdentifier, orgIdentifier, projectIdentifier, referredEntityName, CONNECTORS);
    EntitySetupUsageDTO entitySetupUsageDTO =
        createEntityReference(accountIdentifier, referredEntity, referredByEntity);
    setupUsages.add(entitySetupUsageDTO);
    entitySetupUsageService.save(entitySetupUsageDTO);

    String referredEntityFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, referredIdentifier);
    Page<EntitySetupUsageDTO> entityReferenceDTOPage =
        entitySetupUsageService.listAllEntityUsage(0, 10, accountIdentifier, referredEntityFQN, null);
    assertThat(entityReferenceDTOPage.getTotalElements()).isEqualTo(2);
    verifyTheValuesAreCorrect(entityReferenceDTOPage.getContent().get(0), setupUsages.get(1));
    verifyTheValuesAreCorrect(entityReferenceDTOPage.getContent().get(1), setupUsages.get(0));
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void saveTest() {
    EntityDetail referredEntity = getEntityDetails(
        referredIdentifier, accountIdentifier, orgIdentifier, projectIdentifier, referredEntityName, CONNECTORS);
    EntityDetail referredByEntity = getEntityDetails(
        referredByIdentifier, accountIdentifier, orgIdentifier, projectIdentifier, referredByEntityName, SECRETS);
    EntitySetupUsageDTO entitySetupUsageDTO =
        createEntityReference(accountIdentifier, referredEntity, referredByEntity);
    EntitySetupUsageDTO savedEntitySetupUsageDTO = entitySetupUsageService.save(entitySetupUsageDTO);
    verifyTheValuesAreCorrect(savedEntitySetupUsageDTO, entitySetupUsageDTO);
  }

  private void verifyTheValuesAreCorrect(
      EntitySetupUsageDTO actualEntitySetupUsageDTO, EntitySetupUsageDTO expectedEntitySetupUsageDTO) {
    assertThat(actualEntitySetupUsageDTO).isNotNull();
    assertThat(actualEntitySetupUsageDTO.getReferredEntity())
        .isEqualTo(expectedEntitySetupUsageDTO.getReferredEntity());
    assertThat(actualEntitySetupUsageDTO.getReferredEntity().getType())
        .isEqualTo(expectedEntitySetupUsageDTO.getReferredEntity().getType());
    assertThat(actualEntitySetupUsageDTO.getReferredEntity().getName())
        .isEqualTo(expectedEntitySetupUsageDTO.getReferredEntity().getName());

    assertThat(actualEntitySetupUsageDTO.getReferredByEntity())
        .isEqualTo(expectedEntitySetupUsageDTO.getReferredByEntity());
    assertThat(actualEntitySetupUsageDTO.getReferredByEntity().getType())
        .isEqualTo(expectedEntitySetupUsageDTO.getReferredByEntity().getType());
    assertThat(actualEntitySetupUsageDTO.getReferredByEntity().getName())
        .isEqualTo(expectedEntitySetupUsageDTO.getReferredByEntity().getName());

    assertThat(actualEntitySetupUsageDTO.getCreatedAt()).isNotNull();
  }

  private EntitySetupUsageDTO saveEntitySetupUsage() {
    EntityDetail referredEntity = getEntityDetails(
        referredIdentifier, accountIdentifier, orgIdentifier, projectIdentifier, referredEntityName, CONNECTORS);
    EntityDetail referredByEntity = getEntityDetails(
        referredByIdentifier, accountIdentifier, orgIdentifier, projectIdentifier, referredByEntityName, SECRETS);
    EntitySetupUsageDTO entitySetupUsageDTO1 =
        createEntityReference(accountIdentifier, referredEntity, referredByEntity);
    return entitySetupUsageService.save(entitySetupUsageDTO1);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void deleteTest() {
    String referredEntityFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, referredIdentifier);
    String referredByEntityFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, referredByIdentifier);
    saveEntitySetupUsage();
    boolean isDeleted = entitySetupUsageService.delete(accountIdentifier, referredEntityFQN, referredByEntityFQN);
    assertThat(isDeleted).isTrue();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void deleteTestWhenRecordDoesnNotExists() {
    boolean isDeleted = entitySetupUsageService.delete(accountIdentifier, "abc", "def");
    assertThat(isDeleted).isFalse();
  }
}
