package io.harness.delegate.task.aws;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsElbListenerRuleData {
  private String ruleArn;
  private String rulePriority;
  private String ruleTargetGroupArn;
}