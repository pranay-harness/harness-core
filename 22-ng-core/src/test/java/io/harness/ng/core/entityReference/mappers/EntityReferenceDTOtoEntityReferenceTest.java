package io.harness.ng.core.entityReference.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.EntityType;
import io.harness.ng.core.entityReference.dto.EntityReferenceDTO;
import io.harness.ng.core.entityReference.entity.EntityReference;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class EntityReferenceDTOtoEntityReferenceTest extends CategoryTest {
  @InjectMocks EntityReferenceDTOtoEntity entityReferenceDTOtoEntity;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void toEntityReference() {
    String accountIdentifier = "accountIdentifier";
    String referredByEntityFQN = "account/pipelineIdentifier";
    EntityType referredByEntityType = EntityType.PIPELINES;
    String referredByEntityName = "Pipeline 1";
    String referredEntityFQN = "account/org1/connectorIdnentifier";
    EntityType referredEntityType = EntityType.CONNECTORS;
    String referredEntityName = "Connector 1";

    EntityReferenceDTO entityReferenceDTO = EntityReferenceDTO.builder()
                                                .accountIdentifier(accountIdentifier)
                                                .referredByEntityFQN(referredByEntityFQN)
                                                .referredByEntityType(referredByEntityType)
                                                .referredEntityFQN(referredEntityFQN)
                                                .referredEntityType(referredEntityType)
                                                .referredEntityName(referredEntityName)
                                                .referredByEntityName(referredByEntityName)
                                                .build();
    EntityReference entityReference = entityReferenceDTOtoEntity.toEntityReference(entityReferenceDTO);
    assertThat(entityReference).isNotNull();
    assertThat(entityReference.getAccountIdentifier()).isEqualTo(accountIdentifier);
    assertThat(entityReference.getReferredByEntityFQN()).isEqualTo(referredByEntityFQN);
    assertThat(entityReference.getReferredEntityFQN()).isEqualTo(referredEntityFQN);
    assertThat(entityReference.getReferredByEntityType()).isEqualTo(referredByEntityType.toString());
    assertThat(entityReference.getReferredEntityType()).isEqualTo(referredEntityType.toString());
    assertThat(entityReference.getReferredByEntityName()).isEqualTo(referredByEntityName);
    assertThat(entityReference.getReferredEntityName()).isEqualTo(referredEntityName);
  }
}