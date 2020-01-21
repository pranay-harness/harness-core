package io.harness.governance.pipeline.service;

import io.harness.governance.pipeline.service.model.PipelineGovernanceConfig;

import java.util.List;

public interface PipelineGovernanceService {
  PipelineGovernanceConfig get(String uuid);

  boolean delete(String accountId, String uuid);

  List<PipelineGovernanceConfig> list(String accountId);

  PipelineGovernanceConfig add(String accountId, PipelineGovernanceConfig config);
}
