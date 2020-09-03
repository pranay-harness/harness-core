package io.harness.cvng.analysis.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TimeSeriesCanaryLearningEngineTask extends LearningEngineTask {
  private String postDeploymentDataUrl;
  private String preDeploymentDataUrl;
  private String metricTemplateUrl;
  private DeploymentVerificationTaskInfo deploymentVerificationTaskInfo;
  private int dataLength;
  private int tolerance;
  @Override
  public LearningEngineTaskType getType() {
    return LearningEngineTaskType.TIME_SERIES_CANARY;
  }

  @Value
  @Builder
  public static class DeploymentVerificationTaskInfo {
    private Set<String> oldVersionHosts;
    private Set<String> newVersionHosts;
    private Integer newHostsTrafficSplitPercentage;
  }
}
