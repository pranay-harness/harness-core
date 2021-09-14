/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps.resourcerestraint.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.utils.PmsConstants;
import io.harness.steps.resourcerestraint.ResourceRestraintSpecParameters;
import io.harness.steps.resourcerestraint.service.ResourceRestraintInstanceService;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class ResourceRestraintUtils {
  public String getReleaseEntityId(ResourceRestraintSpecParameters specParameters, String planExecutionId) {
    String releaseEntityId;
    if (PmsConstants.RELEASE_ENTITY_TYPE_PLAN.equals(specParameters.getHoldingScope().getScope())) {
      releaseEntityId = ResourceRestraintInstanceService.getReleaseEntityId(planExecutionId);
    } else {
      releaseEntityId = ResourceRestraintInstanceService.getReleaseEntityId(
          planExecutionId, specParameters.getHoldingScope().getNodeSetupId());
    }
    return releaseEntityId;
  }
}
