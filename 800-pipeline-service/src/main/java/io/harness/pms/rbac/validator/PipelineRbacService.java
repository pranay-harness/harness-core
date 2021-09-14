/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.rbac.validator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.EntityDetail;

import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PipelineRbacService {
  /**
   * validates the rbac on static referred entities for the given yaml
   * It does not validate rbac on runtime expressions
   */
  void extractAndValidateStaticallyReferredEntities(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineId, String pipelineYaml);

  void validateStaticallyReferredEntities(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String pipelineId, String pipelineYaml, List<EntityDetail> entityDetails);
}
