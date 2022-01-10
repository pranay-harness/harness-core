/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
