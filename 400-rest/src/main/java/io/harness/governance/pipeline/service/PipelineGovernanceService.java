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
