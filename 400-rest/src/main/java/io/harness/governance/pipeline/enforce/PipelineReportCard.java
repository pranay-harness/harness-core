/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
