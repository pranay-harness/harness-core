/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.resourceconstraints.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.resourceconstraints.response.ResourceConstraintExecutionInfoDTO;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PMSResourceConstraintService {
  ResourceConstraintExecutionInfoDTO getResourceConstraintExecutionInfo(String accountId, String resourceUnit);
}
