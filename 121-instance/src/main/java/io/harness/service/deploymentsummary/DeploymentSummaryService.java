/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service.deploymentsummary;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.DeploymentSummaryDTO;

import java.util.Optional;

@OwnedBy(HarnessTeam.DX)
public interface DeploymentSummaryService {
  DeploymentSummaryDTO save(DeploymentSummaryDTO deploymentSummaryDTO);

  Optional<DeploymentSummaryDTO> getByDeploymentSummaryId(String deploymentSummaryId);

  Optional<DeploymentSummaryDTO> getNthDeploymentSummaryFromNow(int N, String instanceSyncKey);

  Optional<DeploymentSummaryDTO> getLatestByInstanceKey(String instanceSyncKey);
}
