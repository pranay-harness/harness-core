package io.harness.governance.pipeline.service;

import io.harness.governance.pipeline.model.PipelineGovernanceConfig;

import java.util.List;

public interface PipelineGovernanceService {
  PipelineGovernanceConfig get(String uuid);

  boolean delete(String uuid);

  List<PipelineGovernanceConfig> list(String accountId);

  PipelineGovernanceConfig update(String uuid, PipelineGovernanceConfig config);

  PipelineGovernanceConfig add(PipelineGovernanceConfig config);
}
