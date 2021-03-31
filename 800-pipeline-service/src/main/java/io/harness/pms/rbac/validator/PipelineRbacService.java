package io.harness.pms.rbac.validator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PipelineRbacService {
  /**
   * validates the rbac on static referred entities for the given yaml
   * It does not validate rbac on runtime expressions
   */
  void validateStaticallyReferredEntitiesInYaml(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineId, String pipelineYaml);
}
