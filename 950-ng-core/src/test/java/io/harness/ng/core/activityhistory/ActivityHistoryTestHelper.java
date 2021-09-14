/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.activityhistory;

import static io.harness.EntityType.CONNECTORS;
import static io.harness.EntityType.PIPELINES;

import io.harness.common.EntityReference;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.activityhistory.dto.EntityUsageActivityDetailDTO;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.utils.IdentifierRefHelper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class ActivityHistoryTestHelper {
  public static NGActivityDTO createActivityHistoryDTO(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String identifier, NGActivityStatus status, long activityTime,
      NGActivityType activityType) {
    String identifier1 = "identifier1";
    EntityReference referredEntityRef =
        IdentifierRefHelper.getIdentifierRef(identifier, accountIdentifier, orgIdentifier, projectIdentifier);
    EntityReference referredByEntityRef =
        IdentifierRefHelper.getIdentifierRef(identifier1, accountIdentifier, orgIdentifier, projectIdentifier);
    EntityDetail referredEntity = EntityDetail.builder().entityRef(referredEntityRef).type(CONNECTORS).build();
    EntityDetail referredByEntity = EntityDetail.builder().entityRef(referredByEntityRef).type(PIPELINES).build();
    return NGActivityDTO.builder()
        .description("description")
        .accountIdentifier(accountIdentifier)
        .activityTime(activityTime)
        .activityStatus(status)
        .type(activityType)
        .accountIdentifier("accountIdentifier")
        .referredEntity(referredEntity)
        .detail(EntityUsageActivityDetailDTO.builder().referredByEntity(referredByEntity).build())
        .build();
  }
}
