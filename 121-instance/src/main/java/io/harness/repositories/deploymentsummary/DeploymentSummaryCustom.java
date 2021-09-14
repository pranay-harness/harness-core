/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.deploymentsummary;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.DeploymentSummary;

import java.util.Optional;

@OwnedBy(HarnessTeam.DX)
public interface DeploymentSummaryCustom {
  Optional<DeploymentSummary> fetchNthRecordFromNow(int N, String instanceSyncKey);
}
