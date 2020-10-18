package io.harness.ng.core.activityhistory;

import static io.harness.EntityType.CONNECTORS;
import static io.harness.EntityType.PIPELINES;

import io.harness.common.EntityReference;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.activityhistory.dto.EntityUsageActivityDetailDTO;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import io.harness.utils.IdentifierRefHelper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class ActivityHistoryTestHelper {
  public NGActivityDTO createActivityHistoryDTO(
      String accountIdentfier, String orgIdentifier, String projectIdentifier, String identifier) {
    String identifier1 = "identifier1";
    String referredEntityFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentfier, orgIdentifier, projectIdentifier, identifier);
    EntityReference referredEntityRef =
        IdentifierRefHelper.getIdentifierRef(identifier, accountIdentfier, orgIdentifier, projectIdentifier);
    EntityReference referredByEntityRef =
        IdentifierRefHelper.getIdentifierRef(identifier1, accountIdentfier, orgIdentifier, projectIdentifier);
    EntityDetail referredEntity = EntityDetail.builder().entityRef(referredEntityRef).type(CONNECTORS).build();
    EntityDetail referredByEntity = EntityDetail.builder().entityRef(referredByEntityRef).type(PIPELINES).build();
    return NGActivityDTO.builder()
        .description("description")
        .accountIdentifier(accountIdentfier)
        .activityTime(System.currentTimeMillis())
        .activityStatus(NGActivityStatus.SUCCESS)
        .type(NGActivityType.ENTITY_USAGE)
        .accountIdentifier("accountIdentifier")
        .referredEntity(referredEntity)
        .detail(EntityUsageActivityDetailDTO.builder().referredByEntity(referredByEntity).build())
        .build();
  }
}
