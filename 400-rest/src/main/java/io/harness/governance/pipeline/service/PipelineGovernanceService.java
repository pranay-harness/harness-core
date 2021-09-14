/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.governance.pipeline.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.governance.pipeline.service.model.PipelineGovernanceConfig;

import java.util.List;

@OwnedBy(HarnessTeam.CDC)
public interface PipelineGovernanceService {
  PipelineGovernanceConfig get(String uuid);

  boolean delete(String accountId, String uuid);

  List<PipelineGovernanceConfig> list(String accountId);

  PipelineGovernanceConfig add(String accountId, PipelineGovernanceConfig config);
}
