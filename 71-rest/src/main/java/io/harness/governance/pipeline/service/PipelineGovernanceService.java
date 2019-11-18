package io.harness.governance.pipeline.service;

import io.harness.governance.pipeline.service.model.PipelineGovernanceConfig;

import java.util.List;

public interface PipelineGovernanceService {
  PipelineGovernanceConfig get(String uuid);

  boolean delete(String accountId, String uuid);

  List<PipelineGovernanceConfig> list(String accountId);

  PipelineGovernanceConfig update(String accountId, String uuid, PipelineGovernanceConfig config);

  PipelineGovernanceConfig add(String accountId, PipelineGovernanceConfig config);
}
