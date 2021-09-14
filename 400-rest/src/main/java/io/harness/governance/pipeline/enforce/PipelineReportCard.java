/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.governance.pipeline.enforce;

import io.harness.data.structure.CollectionUtils;
import io.harness.governance.pipeline.service.model.PipelineGovernanceConfig;

import java.util.List;
import lombok.Value;

/**
 * this model tells how a pipeline is performing relative a pipeline governance standard.
 */
@Value
public class PipelineReportCard {
  /**
   * this is just a restricted version of {@link io.harness.governance.pipeline.service.model.PipelineGovernanceConfig}
   * model
   */
  @Value
  static class GovernanceStandard {
    private String id;
    private String name;
    private String description;
  }

  private GovernanceStandard governanceStandard;
  private String pipelineId;
  List<GovernanceRuleStatus> ruleStatuses;

  public PipelineReportCard(
      PipelineGovernanceConfig pipelineGovernanceConfig, String pipelineId, List<GovernanceRuleStatus> ruleStatuses) {
    this.governanceStandard = new GovernanceStandard(pipelineGovernanceConfig.getUuid(),
        pipelineGovernanceConfig.getName(), pipelineGovernanceConfig.getDescription());
    this.pipelineId = pipelineId;
    this.ruleStatuses = ruleStatuses;
  }

  public List<GovernanceRuleStatus> getRuleStatuses() {
    return CollectionUtils.emptyIfNull(ruleStatuses);
  }
}
