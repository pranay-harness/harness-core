package io.harness.pms.resourceconstraints.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.resourceconstraints.response.ResourceConstraintExecutionInfoDTO;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PMSResourceConstraintService {
  ResourceConstraintExecutionInfoDTO getResourceConstraintExecutionInfo(String accountId, String resourceUnit);
}
