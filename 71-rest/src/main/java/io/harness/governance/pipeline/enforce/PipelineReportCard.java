package io.harness.governance.pipeline.enforce;

import io.harness.data.structure.CollectionUtils;
import io.harness.governance.pipeline.model.PipelineGovernanceConfig;
import lombok.Value;

import java.util.List;

/**
 * this model tells how a pipeline is performing relative a pipeline governance standard.
 */
@Value
public class PipelineReportCard {
  /**
   * this is just a restricted version of {@link io.harness.governance.pipeline.model.PipelineGovernanceConfig} model
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
