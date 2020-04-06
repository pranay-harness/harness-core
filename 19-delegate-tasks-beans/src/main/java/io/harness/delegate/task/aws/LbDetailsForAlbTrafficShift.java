package io.harness.delegate.task.aws;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LbDetailsForAlbTrafficShift {
  private String loadBalancerName;
  private String loadBalancerArn;
  private String listenerPort;
  private String listenerArn;
  private boolean useSpecificRule;
  private String ruleArn;
  private String prodTargetGroupName;
  private String prodTargetGroupArn;
  private String stageTargetGroupName;
  private String stageTargetGroupArn;
}