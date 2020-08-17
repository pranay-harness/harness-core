package io.harness.ng.core.entityReference.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.entityReference.ReferenceEntityType;
import io.harness.ng.core.entityReference.dto.EntityReferenceDTO;
import io.harness.ng.core.entityReference.entity.EntityReference;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class EntityReferenceDTOtoEntityTest extends CategoryTest {
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
    ReferenceEntityType referredByEntityType = ReferenceEntityType.PIPELINE;
    String referredEntityFQN = "account/org1/connectorIdnentifier";
    ReferenceEntityType referredEntityType = ReferenceEntityType.CONNECTOR;

    EntityReferenceDTO entityReferenceDTO = EntityReferenceDTO.builder()
                                                .accountIdentifier(accountIdentifier)
                                                .referredByEntityFQN(referredByEntityFQN)
                                                .referredByEntityType(referredByEntityType)
                                                .referredEntityFQN(referredEntityFQN)
                                                .referredEntityType(referredEntityType)
                                                .build();
    EntityReference entityReference = entityReferenceDTOtoEntity.toEntityReference(entityReferenceDTO);
    assertThat(entityReference).isNotNull();
    assertThat(entityReference.getAccountIdentifier()).isEqualTo(accountIdentifier);
    assertThat(entityReference.getReferredByEntityFQN()).isEqualTo(referredByEntityFQN);
    assertThat(entityReference.getReferredEntityFQN()).isEqualTo(referredEntityFQN);
    assertThat(entityReference.getReferredByEntityType()).isEqualTo(referredByEntityType.toString());
    assertThat(entityReference.getReferredEntityType()).isEqualTo(referredEntityType.toString());
  }
}