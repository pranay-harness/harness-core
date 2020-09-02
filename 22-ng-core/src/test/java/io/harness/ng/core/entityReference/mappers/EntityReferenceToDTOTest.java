package io.harness.ng.core.entityReference.mappers;

import static io.harness.ng.EntityType.SECRETS;
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

public class EntityReferenceToDTOTest extends CategoryTest {
  @InjectMocks EntityReferenceToDTO entityReferenceToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void createEntityReferenceDTOTest() {
    String accountIdentifier = "accountIdentifier";
    String referredByEntityFQN = "account/pipelineIdentifier";
    EntityType referredEntityType = EntityType.CONNECTORS;
    String referredByEntityName = "Pipeline 1";
    String referredEntityFQN = "account/org1/connectorIdnentifier";
    EntityType referredByEntityType = SECRETS;
    String referredEntityName = "Connector 1";
    EntityReference entityReference = EntityReference.builder()
                                          .accountIdentifier(accountIdentifier)
                                          .referredByEntityFQN(referredByEntityFQN)
                                          .referredByEntityType(referredByEntityType.toString())
                                          .referredEntityFQN(referredEntityFQN)
                                          .referredEntityType(referredEntityType.toString())
                                          .referredEntityName(referredEntityName)
                                          .referredByEntityName(referredByEntityName)
                                          .build();
    EntityReferenceDTO entityReferenceDTO = entityReferenceToDTO.createEntityReferenceDTO(entityReference);
    assertThat(entityReferenceDTO).isNotNull();
    assertThat(entityReferenceDTO.getAccountIdentifier()).isEqualTo(accountIdentifier);
    assertThat(entityReferenceDTO.getReferredByEntityFQN()).isEqualTo(referredByEntityFQN);
    assertThat(entityReferenceDTO.getReferredEntityFQN()).isEqualTo(referredEntityFQN);
    assertThat(entityReferenceDTO.getReferredByEntityType()).isEqualTo(referredByEntityType);
    assertThat(entityReferenceDTO.getReferredEntityType()).isEqualTo(referredEntityType);
    assertThat(entityReference.getReferredByEntityName()).isEqualTo(referredByEntityName);
    assertThat(entityReference.getReferredEntityName()).isEqualTo(referredEntityName);
  }
}