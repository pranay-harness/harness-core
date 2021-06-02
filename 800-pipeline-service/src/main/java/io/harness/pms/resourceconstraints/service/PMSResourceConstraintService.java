package io.harness.pms.resourceconstraints.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.resourceconstraints.response.ResourceConstraintExecutionInfoDTO;

import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PMSResourceConstraintService {
  List<ResourceConstraintExecutionInfoDTO> getResourceConstraintExecutionInfoList(
      String accountId, String resourceUnit);
}
