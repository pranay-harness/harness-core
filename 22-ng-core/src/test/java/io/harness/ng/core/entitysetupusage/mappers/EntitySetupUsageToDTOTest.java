package io.harness.ng.core.entitysetupusage.mappers;

import static io.harness.EntityType.CONNECTORS;
import static io.harness.EntityType.SECRETS;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class EntitySetupUsageToDTOTest extends CategoryTest {
  @InjectMocks EntitySetupUsageEntityToDTO setupUsageEntityToDTO;

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
    EntitySetupUsage entitySetupUsage =
        EntitySetupUsage.builder()
            .accountIdentifier(accountIdentifier)
            .referredByEntityFQN(referredByEntityFQN)
            .referredByEntityType(referredByEntityType.toString())
            .referredEntityFQN(referredEntityFQN)
            .referredEntityType(referredEntityType.toString())
            .referredEntityName(referredEntityName)
            .referredByEntityName(referredByEntityName)
            .referredEntity(EntityDetail.builder().type(CONNECTORS).name(referredEntityName).build())
            .referredByEntity(EntityDetail.builder().type(SECRETS).name(referredByEntityName).build())
            .build();
    EntitySetupUsageDTO entitySetupUsageDTO = setupUsageEntityToDTO.createEntityReferenceDTO(entitySetupUsage);
    assertThat(entitySetupUsageDTO).isNotNull();
    assertThat(entitySetupUsageDTO.getAccountIdentifier()).isEqualTo(accountIdentifier);
    assertThat(entitySetupUsageDTO.getReferredByEntity()).isNotNull();
    assertThat(entitySetupUsageDTO.getReferredEntity()).isNotNull();
    assertThat(entitySetupUsageDTO.getReferredByEntity().getType()).isEqualTo(referredByEntityType);
    assertThat(entitySetupUsageDTO.getReferredEntity().getType()).isEqualTo(referredEntityType);
    assertThat(entitySetupUsage.getReferredByEntityName()).isEqualTo(referredByEntityName);
    assertThat(entitySetupUsage.getReferredEntityName()).isEqualTo(referredEntityName);
  }
}