package io.harness.ng.core.activityhistory;

import static io.harness.encryption.Scope.PROJECT;

import io.harness.EntityType;
import io.harness.ng.core.activityhistory.dto.EntityUsageActivityDetailDTO;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class ActivityHistoryTestHelper {
  public NGActivityDTO createActivityHistoryDTO(
      String accountIdentfier, String orgIdentifier, String projectIdentifier, String identifier) {
    return NGActivityDTO.builder()
        .description("description")
        .accountIdentifier(accountIdentfier)
        .referredEntityOrgIdentifier(orgIdentifier)
        .referredEntityProjectIdentifier(projectIdentifier)
        .referredEntityIdentifier(identifier)
        .activityTime(System.currentTimeMillis())
        .referredEntityScope(PROJECT)
        .activityStatus(NGActivityStatus.SUCCESS)
        .type(NGActivityType.ENTITY_USAGE)
        .accountIdentifier("accountIdentifier")
        .referredEntityType(EntityType.CONNECTORS)
        .detail(EntityUsageActivityDetailDTO.builder()
                    .referredByEntityIdentifier("identifier")
                    .referredByEntityType(EntityType.PIPELINES)
                    .referredByEntityScope(PROJECT)
                    .build())
        .build();
  }
}
